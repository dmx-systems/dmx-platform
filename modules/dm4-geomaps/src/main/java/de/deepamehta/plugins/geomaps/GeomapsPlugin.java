package de.deepamehta.plugins.geomaps;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.plugins.geomaps.service.GeomapsService;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;



@Path("/geomap")
@Consumes("application/json")
@Produces("application/json")
public class GeomapsPlugin extends PluginActivator implements GeomapsService, PostCreateTopicListener,
                                                                              PostUpdateTopicListener,
                                                                              PreSendTopicListener {

    private static final String GEOCODER_URL = "http://maps.googleapis.com/maps/api/geocode/json?" +
        "address=%s&sensor=false";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicmapsService topicmapsService;
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

    @GET
    @Path("/topic/{id}")
    @Override
    public Topic getGeoTopic(@PathParam("id") long topicId, @HeaderParam("Cookie") ClientState clientState) {
        try {
            Topic topic = dms.getTopic(topicId, true, clientState);
            RelatedTopic parentTopic;
            while ((parentTopic = topic.getRelatedTopic(null, "dm4.core.part", "dm4.core.whole", null,
                    true, false, clientState)) != null) {
                topic = parentTopic;
            }
            return topic;
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Finding the geo coordinate's parent topic failed " +
                "(topicId=" + topicId + ")", e));
        }
    }

    @GET
    @Path("/{id}/topics")
    @Override
    public ResultSet<RelatedTopic> getGeomapTopics(@PathParam("id") long geomapId) {
        return Geomap.fetchGeomapTopics(geomapId, dms);
    }

    @PUT
    @Path("/{id}/topic/{topic_id}")
    @Override
    public void addTopicToGeomap(@PathParam("id") long geomapId, @PathParam("topic_id") long topicId) {
        logger.info("### Adding topic " + topicId + " to geomap " + geomapId);
        AssociationModel model = new AssociationModel("dm4.geomaps.geotopic_mapcontext",
            new TopicRoleModel(geomapId, "dm4.core.default"),
            new TopicRoleModel(topicId,  "dm4.topicmaps.topicmap_topic")
        );
        Association refAssoc = dms.createAssociation(model, null);     // FIXME: clientState=null
        // ### return refAssoc.getId();
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



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        topicmapsService.registerTopicmapRenderer(new GeomapRenderer());
    }

    // ---

    @Override
    @ConsumesService({
        "de.deepamehta.plugins.topicmaps.service.TopicmapsService",
        "de.deepamehta.plugins.facets.service.FacetsService"
    })
    public void serviceArrived(PluginService service) {
        if (service instanceof FacetsService) {
            facetsService = (FacetsService) service;
        } else if (service instanceof TopicmapsService) {
            topicmapsService = (TopicmapsService) service;
        }
    }

    @Override
    public void serviceGone(PluginService service) {
        if (service == facetsService) {
            facetsService = null;
        } else if (service == topicmapsService) {
            topicmapsService = null;
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            //
            facetsService.addFacetTypeToTopic(topic.getId(), "dm4.geomaps.geo_coordinate_facet");
            //
            Address address = new Address(topic.getCompositeValue());
            if (!address.isEmpty()) {
                logger.info("### New " + address);
                LonLat geoCoordinate = address.geocode();
                storeGeoFacet(topic, geoCoordinate, clientState, directives);
            } else {
                logger.info("### New empty address");
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel, ClientState clientState,
                                                                                       Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            Address address    = new Address(topic.getCompositeValue());
            Address oldAddress = new Address(oldModel.getCompositeValue());
            if (!address.equals(oldAddress)) {
                logger.info("### Address changed:" + address.changeReport(oldAddress));
                LonLat geoCoordinate = address.geocode();
                storeGeoFacet(topic, geoCoordinate, clientState, directives);
            } else {
                logger.info("### Address not changed");
            }
        }
    }

    // ---

    /**
     * Enriches an Address topic with its Geo Coordinate facet.
     */
    @Override
    public void preSendTopic(Topic topic, ClientState clientState) {
        TopicModel address = findAddress(topic);
        if (address == null) {
            return;
        }
        //
        Topic geoFacet = facetsService.getFacet(address.getId(), "dm4.geomaps.geo_coordinate_facet");
        if (geoFacet != null) {
            logger.info("### Enriching address " + address.getId() + " with its geo facet");
            address.getCompositeValue().put("dm4.geomaps.geo_coordinate", geoFacet.getModel());
        } else {
            logger.info("### Enriching address " + address.getId() + " with its geo facet ABORTED " +
                "-- no geo facet in DB");
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Stores a geo facet for an address topic in the DB.
     */
    private void storeGeoFacet(Topic address, LonLat geoCoordinate, ClientState clientState, Directives directives) {
        try {
            logger.info("Storing geo facet (" + geoCoordinate + ") of address " + address);
            TopicModel geoFacet = new TopicModel("dm4.geomaps.geo_coordinate", new CompositeValue()
                .put("dm4.geomaps.longitude", geoCoordinate.lon)
                .put("dm4.geomaps.latitude",  geoCoordinate.lat)
            );
            facetsService.updateFacet(address, "dm4.geomaps.geo_coordinate_facet", geoFacet, clientState, directives);
        } catch (Exception e) {
            throw new RuntimeException("Storing geo facet of address " + address.getId() + " failed", e);
        }
    }

    // ---

    private TopicModel findAddress(Topic topic) {
        return findChildTopic(topic.getModel(), "dm4.contacts.address");
    }

    /**
     * Searches a topic's composite value for a topic of a given type.
     * The search is driven by the topic's type definition. In other words, composite value entries which do not
     * adhere to the topic's type definition are not found.
     * Note: this is an in-memory search; the DB is not accessed.
     * <p>
     * The first topic found is returned, according to a depth-first search.
     * For multiple-value fields only the first topic is returned.
     * <p>
     * TODO: make this a generally available method by adding it to the Topic interface?
     */
    private TopicModel findChildTopic(TopicModel topic, String topicTypeUri) {
        String typeUri = topic.getTypeUri();
        if (typeUri.equals(topicTypeUri)) {
            return topic;
        }
        CompositeValue comp = topic.getCompositeValue();
        TopicType topicType = dms.getTopicType(typeUri, null);      // clientState=null
        for (AssociationDefinition assocDef : topicType.getAssocDefs()) {
            String childTypeUri   = assocDef.getPartTypeUri();
            String cardinalityUri = assocDef.getPartCardinalityUri();
            TopicModel childTopic = null;
            if (cardinalityUri.equals("dm4.core.one")) {
                childTopic = comp.getTopic(childTypeUri, null);
            } else if (cardinalityUri.equals("dm4.core.many")) {
                List<TopicModel> childTopics = comp.getTopics(childTypeUri, null);
                if (childTopics != null) {
                    childTopic = childTopics.get(0);     // ### FIXME: must check empty?
                }
            } else {
                throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
            }
            // Note: topics just created have no child topics yet
            if (childTopic == null) {
                continue;
            }
            childTopic = findChildTopic(childTopic, topicTypeUri);
            if (childTopic != null) {
                return childTopic;
            }
        }
        return null;
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
