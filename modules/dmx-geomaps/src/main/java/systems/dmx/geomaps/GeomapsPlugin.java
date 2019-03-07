package systems.dmx.geomaps;

import systems.dmx.topicmaps.TopicmapsService;
import systems.dmx.facets.FacetsService;

import systems.dmx.core.ChildTopics;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.event.PostCreateTopicListener;
import systems.dmx.core.service.event.PostUpdateTopicListener;
import systems.dmx.core.service.event.PreSendTopicListener;
import systems.dmx.core.util.ContextTracker;
import systems.dmx.core.util.DMXUtils;
import systems.dmx.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;



@Path("/geomap")
@Consumes("application/json")
@Produces("application/json")
public class GeomapsPlugin extends PluginActivator implements GeomapsService, GeomapsConstants,
                                                                              PostCreateTopicListener,
                                                                              PostUpdateTopicListener,
                                                                              PreSendTopicListener {

    private static final String GEOCODER_URL = "https://nominatim.openstreetmap.org/search?" +
        "street=%s&postalcode=%s&city=%s&country=%s&format=json&limit=1";

    private static final String COOKIE_NO_GEOCODING = "dmx_no_geocoding";

    private static final double EARTH_RADIUS_KM = 6371.009;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private TopicmapsService topicmapsService;
    @Inject private FacetsService facetsService;

    // used for geocoding suppression
    private ContextTracker contextTracker = new ContextTracker();

    private Messenger me = new Messenger("systems.dmx.webclient");

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************
    // *** GeomapsService ***
    // **********************



    @GET
    @Path("/{id}")
    @Override
    public Geomap getGeomap(@PathParam("id") long geomapId) {
        try {
            logger.info("Fetching geomap " + geomapId);
            // Note: a Geomap is not a DMXObject. So the JerseyResponseFilter's automatic
            // child topic loading is not applied. We must load the child topics manually here.
            Topic geomapTopic = dmx.getTopic(geomapId).loadChildTopics();
            return new Geomap(
                geomapTopic.getModel(),
                fetchGeomapViewProps(geomapTopic),
                fetchGeoCoordinates(geomapTopic)
            );
        } catch (Exception e) {
            throw new RuntimeException("Fetching geomap " + geomapId + " failed", e);
        }
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/coord/{geo_coord_id}")
    @Override
    public List<Topic> getDomainTopics(@PathParam("geo_coord_id") long geoCoordId) {
        try {
            return DMXUtils.getParentTopics(dmx.getTopic(geoCoordId));
        } catch (Exception e) {
            throw new RuntimeException("Finding domain topics failed (geoCoordId=" + geoCoordId + ")", e);
        }
    }

    @Override
    public GeoCoordinate getGeoCoordinate(Topic geoTopic) {
        try {
            Topic geoCoordTopic = getGeoCoordinateTopic(geoTopic);
            if (geoCoordTopic != null) {
                return geoCoordinate(geoCoordTopic);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Getting the geo coordinate failed (geoTopic=" + geoTopic + ")", e);
        }
    }

    @Override
    public GeoCoordinate geoCoordinate(Topic geoCoordTopic) {
        ChildTopics childTopics = geoCoordTopic.getChildTopics();
        return new GeoCoordinate(
            childTopics.getDouble("dmx.geomaps.longitude"),
            childTopics.getDouble("dmx.geomaps.latitude")
        );
    }

    // TODO: rename path segment "topic" to "coord"
    @PUT
    @Path("/{id}/topic/{geo_coord_id}")
    @Transactional
    @Override
    public void addCoordinateToGeomap(@PathParam("id") long geomapId, @PathParam("geo_coord_id") long geoCoordId) {
        logger.info("### Adding geo coordinate topic " + geoCoordId + " to geomap " + geomapId);
        AssociationModel model = mf.newAssociationModel("dmx.geomaps.geotopic_mapcontext",
            mf.newTopicRoleModel(geomapId,   "dmx.core.default"),
            mf.newTopicRoleModel(geoCoordId, "dmx.topicmaps.topicmap_content")  // TODO: use "dmx.core.default" instead?
        );
        dmx.createAssociation(model);
    }

    @PUT
    @Path("/{id}/center/{lon}/{lat}/zoom/{zoom}")
    @Transactional
    @Override
    public void setGeomapState(@PathParam("id") long geomapId, @PathParam("lon") double lon,
                               @PathParam("lat") double lat, @PathParam("zoom") int zoom) {
        try {
            mf.newViewProps()
                .put(PROP_LONGITUDE, lon)
                .put(PROP_LATITUDE, lat)
                .put(PROP_ZOOM, zoom)
                .store(dmx.getTopic(geomapId));
        } catch (Exception e) {
            throw new RuntimeException("Setting state of geomap " + geomapId + " failed (lon=" + lon + ", lat=" + lat +
                ", zoom=" + zoom + ")", e);
        }
    }

    @GET
    @Path("/distance")
    @Override
    public double getDistance(@QueryParam("coord1") GeoCoordinate coord1,
                              @QueryParam("coord2") GeoCoordinate coord2) {
        // calculate distance by the flat-surface formula for a "Spherical Earth projected to a plane"
        // http://en.wikipedia.org/wiki/Geographical_distance#Flat-surface_formulae
        double lonDiff = Math.toRadians(coord2.lon - coord1.lon);
        double latDiff = Math.toRadians(coord2.lat - coord1.lat);
        double latMean = Math.toRadians((coord1.lat + coord2.lat) / 2);
        return EARTH_RADIUS_KM * Math.sqrt(Math.pow(latDiff, 2) + Math.pow(Math.cos(latMean) * lonDiff, 2));
    }

    // ---

    @Override
    public <V> V runWithoutGeocoding(Callable<V> callable) throws Exception {
        return contextTracker.run(callable);
    }



    // *************
    // *** Hooks ***
    // *************



    @Override
    public void init() {
        topicmapsService.registerTopicmapType(new GeomapType());
    }



    // *****************
    // *** Listeners ***
    // *****************



    @Override
    public void postCreateTopic(Topic topic) {
        if (topic.getTypeUri().equals("dmx.contacts.address")) {
            if (!abortGeocoding(topic)) {
                //
                facetsService.addFacetTypeToTopic(topic.getId(), "dmx.geomaps.geo_coordinate_facet");
                //
                Address address = new Address(topic /* .getChildTopics().getModel() */);    // ### TODO
                if (!address.isEmpty()) {
                    logger.info("### New " + address);
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("New empty address");
                }
            }
        } else if (topic.getTypeUri().equals("dmx.geomaps.geo_coordinate")) {
            // logger.info("### New geo coordinate: " + topic.loadChildTopics());
            me.newGeoCoord(topic.loadChildTopics());
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals("dmx.contacts.address")) {
            // Note: Address is a value type. An address is immutable ### TODO
            throw new RuntimeException("postUpdateTopic() invoked for an Address topic: " + topic);
            /* if (!abortGeocoding(topic)) {
                Address address    = new Address(topic.getChildTopics().getModel());
                Address oldAddress = new Address(oldTopic.getChildTopicsModel());
                if (!address.equals(oldAddress)) {
                    logger.info("### Address changed:" + address.changeReport(oldAddress));
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("Address not changed");
                }
            } */
        }
    }

    // ---

    /**
     * Enriches an Address topic with its geo coordinate.
     */
    @Override
    public void preSendTopic(Topic topic) {
        Topic address = topic.findChildTopic("dmx.contacts.address");
        if (address != null) {
            String operation = "Enriching address " + address.getId() + " with its geo coordinate";
            Topic geoCoordTopic = getGeoCoordinateTopic(address);
            if (geoCoordTopic != null) {
                logger.info(operation);
                address.getChildTopics().getModel().put("dmx.geomaps.geo_coordinate", geoCoordTopic.getModel());
            } else {
                logger.info(operation + " SKIPPED -- no geo coordinate in DB");
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ViewProps fetchGeomapViewProps(Topic geomapTopic) {
        return mf.newViewProps()
            .put(PROP_LONGITUDE, geomapTopic.getProperty(PROP_LONGITUDE))
            .put(PROP_LATITUDE,  geomapTopic.getProperty(PROP_LATITUDE))
            .put(PROP_ZOOM,      geomapTopic.getProperty(PROP_ZOOM));
    }

    private Map<Long, TopicModel> fetchGeoCoordinates(Topic geomapTopic) {
        Map<Long, TopicModel> geoCoords = new HashMap();
        for (Topic geoCoord : _fetchGeoCoordinates(geomapTopic)) {
            geoCoords.put(geoCoord.getId(), geoCoord.getModel());
        }
        return geoCoords;
    }

    private List<? extends Topic> _fetchGeoCoordinates(Topic geomapTopic) {
        // Note: we retrieve just *all* (readable) geo coordinates
        return DMXUtils.loadChildTopics(dmx.getTopicsByType("dmx.geomaps.geo_coordinate"));
        // TODO: retrieve per-topicmap?
        // return DMXUtils.loadChildTopics(geomapTopic.getRelatedTopics("dmx.geomaps.geotopic_mapcontext",
        //     "dmx.core.default", "dmx.topicmaps.topicmap_content", "dmx.geomaps.geo_coordinate"));
    }

    // ---

    /**
     * Returns the Geo Coordinate topic (including its child topics) of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     */
    private Topic getGeoCoordinateTopic(Topic geoTopic) {
        Topic geoCoordTopic = facetsService.getFacet(geoTopic, "dmx.geomaps.geo_coordinate_facet");
        return geoCoordTopic != null ? geoCoordTopic.loadChildTopics() : null;
    }

    // ---

    /**
     * Geocodes the given address and stores the resulting coordinate as a facet value of the given Address topic.
     * If geocoding (or storing the coordinate) fails a warning is logged; no exception is thrown.
     *
     * @param   topic   the Address topic to be facetted.
     */
    private void geocodeAndStoreFacet(Address address, Topic topic) {
        try {
            GeoCoordinate geoCoord = address.geocode();
            storeGeoCoordinate(topic, geoCoord);
        } catch (Exception e) {
            // ### TODO: show to the user?
            logger.log(Level.WARNING, "Adding geo coordinate to " + address + " failed", e);
        }
    }

    /**
     * Stores a geo coordinate for an address topic in the DB.
     */
    private void storeGeoCoordinate(Topic address, GeoCoordinate geoCoord) {
        try {
            logger.info("Storing geo coordinate (" + geoCoord + ") of address topic " + address.getId());
            facetsService.updateFacet(address, "dmx.geomaps.geo_coordinate_facet",
                mf.newFacetValueModel("dmx.geomaps.geo_coordinate")
                .put(mf.newChildTopicsModel()
                    .put("dmx.geomaps.longitude", geoCoord.lon)
                    .put("dmx.geomaps.latitude",  geoCoord.lat)
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Storing geo coordinate of address " + address.getId() + " failed", e);
        }
    }

    // ---

    private boolean abortGeocoding(Topic address) {
        return abortGeocodingByCookie(address) || abortGeocodingByExcecutionContext(address);
    }

    private boolean abortGeocodingByCookie(Topic address) {
        boolean abort = false;
        Cookies cookies = Cookies.get();
        if (cookies.has(COOKIE_NO_GEOCODING)) {
            String value = cookies.get(COOKIE_NO_GEOCODING);
            if (!value.equals("false") && !value.equals("true")) {
                throw new RuntimeException("\"" + value + "\" is an unexpected value for the \"" + COOKIE_NO_GEOCODING +
                    "\" cookie (expected are \"false\" or \"true\")");
            }
            abort = value.equals("true");
            if (abort) {
                logger.info("Geocoding for Address topic " + address.getId() + " SUPPRESSED -- \"" +
                    COOKIE_NO_GEOCODING + "\" cookie detected");
            }
        }
        return abort;
    }

    private boolean abortGeocodingByExcecutionContext(Topic address) {
        boolean abort = contextTracker.runsInTrackedContext();
        if (abort) {
            logger.info("Geocoding for Address topic " + address.getId() + " SUPPRESSED -- runWithoutGeocoding() " +
                "context detected");
        }
        return abort;
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class Address {

        String street, postalCode, city, country;

        // ---

        Address(ChildTopicsModel address) {
            // Note: some Address child topics might be deleted (resp. do not exist), so we use ""
            // as defaults here. Otherwise "Invalid access to ChildTopicsModel" would be thrown.
            street     = address.getString("dmx.contacts.street", "");
            postalCode = address.getString("dmx.contacts.postal_code", "");
            city       = address.getString("dmx.contacts.city", "");
            country    = address.getString("dmx.contacts.country", "");
        }

        // TODO: drop it
        Address(Topic address) {
            // Note: some Address child topics might be deleted (resp. do not exist), so we use ""
            // as defaults here. Otherwise "Invalid access to ChildTopicsModel" would be thrown.
            ChildTopics childs = address.getChildTopics();
            String st = childs.getStringOrNull("dmx.contacts.street");
            String po = childs.getStringOrNull("dmx.contacts.postal_code");
            String ci = childs.getStringOrNull("dmx.contacts.city");
            String co = childs.getStringOrNull("dmx.contacts.country");
            street     = st != null ? st : "";
            postalCode = po != null ? po : "";
            city       = ci != null ? ci : "";
            country    = co != null ? co : "";
        }

        // ---

        GeoCoordinate geocode() {
            URL url = null;
            try {
                // perform request
                url = new URL(String.format(GEOCODER_URL,
                    JavaUtils.encodeURIComponent(street),
                    JavaUtils.encodeURIComponent(postalCode),
                    JavaUtils.encodeURIComponent(city),
                    JavaUtils.encodeURIComponent(country)
                ));
                logger.info("Geocoding url=\"" + url + "\"");
                // parse response
                JSONArray results = new JSONArray(JavaUtils.readTextURL(url));
                if (results.length() == 0) {
                    throw new RuntimeException("Address not found");
                }
                JSONObject result = results.getJSONObject(0);
                // create result
                GeoCoordinate geoCoord = new GeoCoordinate(
                    result.getDouble("lon"),
                    result.getDouble("lat")
                );
                logger.info("=> " + geoCoord);
                return geoCoord;
            } catch (Exception e) {
                throw new RuntimeException("Geocoding failed, url=\"" + url + "\"", e);
            }
        }

        boolean isEmpty() {
            return street.equals("") && postalCode.equals("") && city.equals("") && country.equals("");
        }

        String changeReport(Address oldAddr) {
            StringBuilder report = new StringBuilder();
            if (!street.equals(oldAddr.street)) {
                report.append("\n    Street: \"" + oldAddr.street + "\" -> \"" + street + "\"");
            }
            if (!postalCode.equals(oldAddr.postalCode)) {
                report.append("\n    Postal Code: \"" + oldAddr.postalCode + "\" -> \"" + postalCode + "\"");
            }
            if (!city.equals(oldAddr.city)) {
                report.append("\n    City: \"" + oldAddr.city + "\" -> \"" + city + "\"");
            }
            if (!country.equals(oldAddr.country)) {
                report.append("\n    Country: \"" + oldAddr.country + "\" -> \"" + country + "\"");
            }
            return report.toString();
        }

        // === Java API ===

        @Override
        public boolean equals(Object o) {
            if (o instanceof Address) {
                Address addr = (Address) o;
                return street.equals(addr.street) && postalCode.equals(addr.postalCode) &&
                    city.equals(addr.city) && country.equals(addr.country);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (street + postalCode + city + country).hashCode();
        }

        @Override
        public String toString() {
            return "address (street=\"" + street + "\", postalCode=\"" + postalCode +
                "\", city=\"" + city + "\", country=\"" + country + "\")";
        }
    }

    private class Messenger {

        private String pluginUri;

        private Messenger(String pluginUri) {
            this.pluginUri = pluginUri;
        }

        // ---

        private void newGeoCoord(Topic geoCoordTopic) {
            try {
                messageToAll(new JSONObject()
                    .put("type", "newGeoCoord")
                    .put("args", new JSONObject()
                        .put("geoCoordTopic", geoCoordTopic.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"newGeoCoord\" message:", e);
            }
        }

        // ---

        private void messageToAll(JSONObject message) {
            dmx.getWebSocketsService().messageToAll(pluginUri, message.toString());
        }
    }
}
