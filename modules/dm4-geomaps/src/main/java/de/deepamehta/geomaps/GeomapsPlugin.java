package de.deepamehta.geomaps;

import de.deepamehta.geomaps.model.GeoCoordinate;
import de.deepamehta.geomaps.model.Geomap;
import de.deepamehta.topicmaps.TopicmapsService;
import de.deepamehta.facets.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.util.ContextTracker;
import de.deepamehta.core.util.JavaUtils;

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

    private static final String COOKIE_NO_GEOCODING = "dm4_no_geocoding";

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
            Topic topic = dm4.getTopic(geoCoordId);
            RelatedTopic parentTopic;
            while ((parentTopic = topic.getRelatedTopic(null, "dm4.core.child", "dm4.core.parent", null)) != null) {
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
            childTopics.getDouble("dm4.geomaps.longitude"),
            childTopics.getDouble("dm4.geomaps.latitude")
        );
    }

    @PUT
    @Path("/{id}/topic/{geo_coord_id}")
    @Transactional
    @Override
    public void addCoordinateToGeomap(@PathParam("id") long geomapId, @PathParam("geo_coord_id") long geoCoordId) {
        logger.info("### Adding geo coordinate topic " + geoCoordId + " to geomap " + geomapId);
        AssociationModel model = mf.newAssociationModel("dm4.geomaps.geotopic_mapcontext",
            mf.newTopicRoleModel(geomapId,   "dm4.core.default"),
            mf.newTopicRoleModel(geoCoordId, "dm4.topicmaps.topicmap_topic")
        );
        dm4.createAssociation(model);
    }

    @PUT
    @Path("/{id}/center/{lon}/{lat}/zoom/{zoom}")
    @Transactional
    @Override
    public void setGeomapState(@PathParam("id") long geomapId, @PathParam("lon") double lon,
                               @PathParam("lat") double lat, @PathParam("zoom") int zoom) {
        ChildTopicsModel geomapState = mf.newChildTopicsModel().put(
            "dm4.topicmaps.state", mf.newChildTopicsModel().put(
                "dm4.topicmaps.translation", mf.newChildTopicsModel().put(
                    "dm4.topicmaps.translation_x", lon).put(
                    "dm4.topicmaps.translation_y", lat)).put(
                "dm4.topicmaps.zoom_level", zoom)
        );
        dm4.updateTopic(mf.newTopicModel(geomapId, geomapState));
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
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            if (!abortGeocoding(topic)) {
                //
                facetsService.addFacetTypeToTopic(topic.getId(), "dm4.geomaps.geo_coordinate_facet");
                //
                Address address = new Address(topic.getChildTopics().getModel());
                if (!address.isEmpty()) {
                    logger.info("### New " + address);
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("### New empty address");
                }
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            if (!abortGeocoding(topic)) {
                Address address    = new Address(topic.getChildTopics().getModel());
                Address oldAddress = new Address(oldTopic.getChildTopicsModel());
                if (!address.equals(oldAddress)) {
                    logger.info("### Address changed:" + address.changeReport(oldAddress));
                    geocodeAndStoreFacet(address, topic);
                } else {
                    logger.info("### Address not changed");
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
        Topic address = topic.findChildTopic("dm4.contacts.address");
        if (address != null) {
            String operation = "### Enriching address " + address.getId() + " with its geo coordinate";
            Topic geoCoordTopic = getGeoCoordinateTopic(address);
            if (geoCoordTopic != null) {
                logger.info(operation);
                address.getChildTopics().getModel().put("dm4.geomaps.geo_coordinate", geoCoordTopic.getModel());
            } else {
                logger.info(operation + " ABORTED -- no geo coordinate in DB");
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Returns the Geo Coordinate topic (including its child topics) of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     */
    private Topic getGeoCoordinateTopic(Topic geoTopic) {
        Topic geoCoordTopic = facetsService.getFacet(geoTopic, "dm4.geomaps.geo_coordinate_facet");
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
            logger.info("Storing geo coordinate (" + geoCoord + ") of address " + address);
            facetsService.updateFacet(address, "dm4.geomaps.geo_coordinate_facet",
                mf.newFacetValueModel("dm4.geomaps.geo_coordinate")
                .put(mf.newChildTopicsModel()
                    .put("dm4.geomaps.longitude", geoCoord.lon)
                    .put("dm4.geomaps.latitude",  geoCoord.lat)
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
            street     = address.getString("dm4.contacts.street", "");
            postalCode = address.getString("dm4.contacts.postal_code", "");
            city       = address.getString("dm4.contacts.city", "");
            country    = address.getString("dm4.contacts.country", "");
        }

        // ---

        GeoCoordinate geocode() {
            URL url = null;
            try {
                // perform request
                String address = street + ", " + postalCode + " " + city + ", " + country;
                url = new URL(String.format(GEOCODER_URL, JavaUtils.encodeURIComponent(address)));
                logger.info("### Geocoding \"" + address + "\"\n    url=\"" + url + "\"");
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
