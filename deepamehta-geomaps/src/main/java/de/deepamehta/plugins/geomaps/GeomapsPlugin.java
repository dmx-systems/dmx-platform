package de.deepamehta.plugins.geomaps;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.plugins.geomaps.service.GeomapsService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;

import java.net.URL;
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class GeomapsPlugin extends Plugin implements GeomapsService {

    private static final String GEOCODER_URL = "http://maps.googleapis.com/maps/api/geocode/json?" +
        "address=%s&sensor=false";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FacetsService facetsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************************
    // *** GeomapsService Implementation ***
    // *************************************



    @GET
    @Path("/{id}")
    @Override
    public Geomap getGeomap(@PathParam("id") long geomapId) {
        return new Geomap(geomapId, dms);
    }

    @PUT
    @Path("/{id}/topic/{topic_id}")
    @Override
    public void addTopicToGeomap(@PathParam("id") long geomapId, @PathParam("topic_id") long topicId) {
        logger.info("### Adding topic " + topicId + " to geomap " + geomapId);
        AssociationModel model = new AssociationModel("dm4.geomaps.geotopic_mapcontext",
            new TopicRoleModel(geomapId, "dm4.core.default"),
            new TopicRoleModel(topicId,  "dm4.topicmaps.topicmap_topic"));
        Association refAssoc = dms.createAssociation(model, null);     // FIXME: clientContext=null
        // ### return refAssoc.getId();
    }

    @GET
    @Path("/topic/{id}")
    @Override
    public Topic getGeoTopic(@PathParam("id") long topicId) {
        try {
            Topic topic = dms.getTopic(topicId, true, null);
            RelatedTopic parentTopic;
            while ((parentTopic = topic.getRelatedTopic(null, "dm4.core.part", "dm4.core.whole", null,
                    true, false)) != null) {
                topic = parentTopic;
            }
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Finding the geo coordinate's parent topic failed (topicId=" + topicId + ")");
        }
    }

    @PUT
    @Path("/{id}/center/{lon}/{lat}/zoom/{zoom}")
    @Override
    public void setGeomapState(@PathParam("id") long geomapId, @PathParam("lon") double lon,
                               @PathParam("lat") double lat, @PathParam("zoom") int zoom) {
        CompositeValue geomapState = new CompositeValue().put("dm4.topicmaps.state", new CompositeValue()
           .put("dm4.topicmaps.translation", new CompositeValue()
               .put("dm4.topicmaps.translation_x", lon)
               .put("dm4.topicmaps.translation_y", lat))
           .put("dm4.topicmaps.zoom_level", zoom));
        dms.updateTopic(new TopicModel(geomapId, geomapState), null);
    }



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void serviceArrived(PluginService service) {
        logger.info("########## Service arrived: " + service);
        if (service instanceof FacetsService) {
            facetsService = (FacetsService) service;
        }
    }

    @Override
    public void serviceGone(PluginService service) {
        logger.info("########## Service gone: " + service);
        if (service == facetsService) {
            facetsService = null;
        }
    }

    // ---

    @Override
    public void postFetchTopicHook(Topic topic) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            Topic geoFacet = facetsService.getFacet(topic, "dm4.geomaps.geo_coordinate_facet");
            if (geoFacet != null) {
                logger.info("### Extending composite of address " + topic.getId() + " with geo facet");
                topic.getCompositeValue().put("dm4.geomaps.geo_coordinate", geoFacet.getModel());
            } else {
                logger.info("### Extending composite of address " + topic.getId() + " ABORTED -- no geo facet in DB");
            }
        }
    }

    @Override
    public void postCreateHook(Topic topic, ClientContext clientContext, Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            //
            facetsService.associateWithFacetType(topic.getId(), "dm4.geomaps.geo_coordinate_facet");
            //
            Address address = new Address(topic.getCompositeValue());
            if (!address.isEmpty()) {
                logger.info("### New " + address);
                LonLat geoCoordinate = address.geocode();
                addGeoFacet(topic, geoCoordinate, clientContext, directives);
            } else {
                logger.info("### New empty address");
            }
        }
    }

    @Override
    public void postUpdateHook(Topic topic, TopicModel oldTopic, ClientContext clientContext, Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            Address address    = new Address(topic.getCompositeValue());
            Address oldAddress = new Address(oldTopic.getCompositeValue());
            if (!address.equals(oldAddress)) {
                logger.info("### Address changed:" + address.changeReport(oldAddress));
                LonLat geoCoordinate = address.geocode();
                addGeoFacet(topic, geoCoordinate, clientContext, directives);
            } else {
                logger.info("### Address not changed");
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Stores a geo facet for an address topic in the DB.
     */
    private void addGeoFacet(Topic addressTopic, LonLat geoCoordinate, ClientContext clientContext,
                                                                       Directives directives) {
        try {
            logger.info("Adding geo facet (" + geoCoordinate + ") to address " + addressTopic);
            TopicModel geoFacet = new TopicModel("dm4.geomaps.geo_coordinate", new CompositeValue()
                .put("dm4.geomaps.longitude", geoCoordinate.lon)
                .put("dm4.geomaps.latitude",  geoCoordinate.lat));
            facetsService.addFacet(addressTopic, "dm4.geomaps.geo_coordinate_facet", geoFacet,
                clientContext, directives);
        } catch (Exception e) {
            throw new RuntimeException("Adding geo facet to address topic failed");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class Address {

        String street, postalCode, city, country;

        // ---

        Address(CompositeValue address) {
            street     = address.getString("dm4.contacts.street");
            postalCode = address.getString("dm4.contacts.postal_code");
            city       = address.getString("dm4.contacts.city");
            country    = address.getString("dm4.contacts.country");
        }

        // ---

        LonLat geocode() {
            URL url = null;
            try {
                // perform request
                String address = street + ", " + postalCode + " " + city + ", " + country;
                url = new URL(String.format(GEOCODER_URL, JavaUtils.encodeURIComponent(address)));
                logger.info("Geocoding \"" + address + "\"\n        " + url);
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
                //
                LonLat geoCoordinate = new LonLat(lng, lat);
                logger.info("  => " + geoCoordinate);
                return geoCoordinate;
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
                report.append("\n        Street: \"" + oldAddr.street + "\" -> \"" + street + "\"");
            }
            if (!postalCode.equals(oldAddr.postalCode)) {
                report.append("\n        Postal Code: \"" + oldAddr.postalCode + "\" -> \"" + postalCode + "\"");
            }
            if (!city.equals(oldAddr.city)) {
                report.append("\n        City: \"" + oldAddr.city + "\" -> \"" + city + "\"");
            }
            if (!country.equals(oldAddr.country)) {
                report.append("\n        Country: \"" + oldAddr.country + "\" -> \"" + country + "\"");
            }
            return report.toString();
        }

        // === Java API ===

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Address)) {
                return false;
            }
            Address addr = (Address) o;
            return street.equals(addr.street) && postalCode.equals(addr.postalCode) &&
                city.equals(addr.city) && country.equals(addr.country);
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

    private class LonLat {

        double lon, lat;

        LonLat(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        public String toString() {
            return "long=" + lon + ", lat=" + lat;
        }
    }
}
