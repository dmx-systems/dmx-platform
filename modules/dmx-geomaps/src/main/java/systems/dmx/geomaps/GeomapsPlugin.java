package systems.dmx.geomaps;

import systems.dmx.geomaps.model.GeoCoordinate;
import systems.dmx.geomaps.model.Geomap;
import systems.dmx.topicmaps.TopicmapsService;
import systems.dmx.facets.FacetsService;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationDefinition;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.event.PostCreateTopicListener;
import systems.dmx.core.service.event.PostUpdateTopicListener;
import systems.dmx.core.service.event.PreSendTopicListener;
import systems.dmx.core.util.ContextTracker;
import systems.dmx.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;



@Path("/geomap")
@Consumes("application/json")
@Produces("application/json")
public class GeomapsPlugin extends PluginActivator implements GeomapsService, PostCreateTopicListener,
                                                                              PostUpdateTopicListener,
                                                                              PreSendTopicListener {

    private static final String GEOCODER_URL = "http://maps.googleapis.com/maps/api/geocode/json?" +
        "address=%s&sensor=false";

    private static final String COOKIE_NO_GEOCODING = "dmx_no_geocoding";

    private static final double EARTH_RADIUS_KM = 6371.009;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private TopicmapsService topicmapsService;
    @Inject private FacetsService facetsService;

    // used for geocoding suppression
    private ContextTracker contextTracker = new ContextTracker();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************************
    // *** GeomapsService Implementation ***
    // *************************************



    @GET
    @Path("/{id}")
    @Override
    public Geomap getGeomap(@PathParam("id") long geomapId) {
        return new Geomap(geomapId, dm4);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}")
    @Override
    public Topic getDomainTopic(@PathParam("id") long geoCoordId) {
        try {
            Topic topic = dmx.getTopic(geoCoordId);
            RelatedTopic parentTopic;
            while ((parentTopic = topic.getRelatedTopic(null, "dmx.core.child", "dmx.core.parent", null)) != null) {
                topic = parentTopic;
            }
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Finding the geo coordinate's domain topic failed (geoCoordId=" +
                geoCoordId + ")", e);
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

    @PUT
    @Path("/{id}/topic/{geo_coord_id}")
    @Transactional
    @Override
    public void addCoordinateToGeomap(@PathParam("id") long geomapId, @PathParam("geo_coord_id") long geoCoordId) {
        logger.info("### Adding geo coordinate topic " + geoCoordId + " to geomap " + geomapId);
        AssociationModel model = mf.newAssociationModel("dmx.geomaps.geotopic_mapcontext",
            mf.newTopicRoleModel(geomapId,   "dmx.core.default"),
            mf.newTopicRoleModel(geoCoordId, "dmx.topicmaps.topicmap_topic")
        );
        dmx.createAssociation(model);
    }

    @PUT
    @Path("/{id}/center/{lon}/{lat}/zoom/{zoom}")
    @Transactional
    @Override
    public void setGeomapState(@PathParam("id") long geomapId, @PathParam("lon") double lon,
                               @PathParam("lat") double lat, @PathParam("zoom") int zoom) {
        ChildTopicsModel geomapState = mf.newChildTopicsModel().put(
            "dmx.topicmaps.state", mf.newChildTopicsModel().put(
                "dmx.topicmaps.translation", mf.newChildTopicsModel().put(
                    "dmx.topicmaps.translation_x", lon).put(
                    "dmx.topicmaps.translation_y", lat)).put(
                "dmx.topicmaps.zoom_level", zoom)
        );
        dmx.updateTopic(mf.newTopicModel(geomapId, geomapState));
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



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        topicmapsService.registerTopicmapRenderer(new GeomapRenderer());
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postCreateTopic(Topic topic) {
        if (topic.getTypeUri().equals("dmx.contacts.address")) {
            if (!abortGeocoding(topic)) {
                //
                facetsService.addFacetTypeToTopic(topic.getId(), "dmx.geomaps.geo_coordinate_facet");
                //
                Address address = new Address(topic.getChildTopics().getModel());
                if (!address.isEmpty()) {
                    logger.info("### New " + address);
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("New empty address");
                }
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals("dmx.contacts.address")) {
            if (!abortGeocoding(topic)) {
                Address address    = new Address(topic.getChildTopics().getModel());
                Address oldAddress = new Address(oldTopic.getChildTopicsModel());
                if (!address.equals(oldAddress)) {
                    logger.info("### Address changed:" + address.changeReport(oldAddress));
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("Address not changed");
                }
            }
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

        // ---

        GeoCoordinate geocode() {
            URL url = null;
            try {
                // perform request
                String address = street + ", " + postalCode + " " + city + ", " + country;
                url = new URL(String.format(GEOCODER_URL, JavaUtils.encodeURIComponent(address)));
                logger.info("Geocoding \"" + address + "\"\n    url=\"" + url + "\"");
                JSONObject response = new JSONObject(JavaUtils.readTextURL(url));
                // check response status
                String status = response.getString("status");
                if (!status.equals("OK")) {
                    throw new RuntimeException(status);
                }
                // parse response
                JSONObject location = response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
                    .getJSONObject("location");
                double lng = location.getDouble("lng");
                double lat = location.getDouble("lat");
                // create result
                GeoCoordinate geoCoord = new GeoCoordinate(lng, lat);
                logger.info("=> " + geoCoord);
                return geoCoord;
            } catch (Exception e) {
                throw new RuntimeException("Geocoding failed (url=\"" + url + "\")", e);
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
}
