package de.deepamehta.plugins.webclient;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.PreUpdateTopicListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.util.DeepaMehtaUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import java.awt.Desktop;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



@Path("/webclient")
@Consumes("application/json")
@Produces("application/json")
public class WebclientPlugin extends PluginActivator implements AllPluginsActiveListener,
                                                                PreUpdateTopicListener,
                                                                PostUpdateTopicListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private boolean hasWebclientLaunched = false;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************
    // *** Webclient Service ***
    // *************************

    // Note: the client service is provided as REST service only (OSGi service not required for the moment).



    /**
     * Performs a fulltext search and creates a search result topic (a bucket).
     */
    @GET
    @Path("/search")
    public Topic searchTopics(@QueryParam("search") String searchTerm,
                              @QueryParam("field")  String fieldUri,
                              @HeaderParam("Cookie") ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            logger.info("searchTerm=\"" + searchTerm + "\", fieldUri=\"" + fieldUri + "\", clientState=" + clientState);
            Set<Topic> singleTopics = dms.searchTopics(searchTerm, fieldUri, clientState);
            Set<Topic> searchableUnits = findSearchableUnits(singleTopics, clientState);
            logger.info(singleTopics.size() + " single topics found, " + searchableUnits.size() + " searchable units");
            Topic searchTopic = createSearchTopic("\"" + searchTerm + "\"", searchableUnits, clientState);
            tx.success();
            return searchTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new WebApplicationException(new RuntimeException("Searching topics failed", e));
        } finally {
            tx.finish();
        }
    }

    /**
     * Performs a by-type search and creates a search result topic (a bucket).
     * <p>
     * Note: this resource method is actually part of the Type Search plugin.
     * TODO: proper modularization. Either let the Type Search plugin provide its own REST resource (with
     * another namespace again) or make the Type Search plugin an integral part of the Webclient plugin.
     */
    @GET
    @Path("/search/by_type/{type_uri}")
    public Topic getTopics(@PathParam("type_uri") String typeUri,
                           @QueryParam("max_result_size") int maxResultSize,
                           @HeaderParam("Cookie") ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            logger.info("typeUri=\"" + typeUri + "\", maxResultSize=" + maxResultSize);
            String searchTerm = dms.getTopicType(typeUri, clientState).getSimpleValue() + "(s)";
            ResultSet<Topic> result = dms.getTopics(typeUri, false, maxResultSize, clientState); // fetchComposite=false
            Topic searchTopic = createSearchTopic(searchTerm, result.getItems(), clientState);
            tx.success();
            return searchTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new WebApplicationException(new RuntimeException("Searching topics failed", e));
        } finally {
            tx.finish();
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void allPluginsActive() {
        String webclientUrl = getWebclientUrl();
        //
        if (hasWebclientLaunched == true) {
            logger.info("### Launching webclient (url=\"" + webclientUrl + "\") ABORTED -- already launched");
            return;
        }
        //
        try {
            logger.info("### Launching webclient (url=\"" + webclientUrl + "\")");
            Desktop.getDesktop().browse(new URI(webclientUrl));
            hasWebclientLaunched = true;
        } catch (Exception e) {
            logger.warning("### Launching webclient failed (" + e + ")");
            logger.warning("### To launch it manually: " + webclientUrl);
        }
    }

    // ---

    @Override
    public void preUpdateTopic(Topic topic, TopicModel newModel, Directives directives) {
        if (topic.getTypeUri().equals("dm4.files.file") && newModel.getTypeUri().equals("dm4.webclient.icon")) {
            String iconUrl = "/filerepo/" + topic.getCompositeValue().getString("dm4.files.path");
            logger.info("### Retyping a file to an icon (iconUrl=" + iconUrl + ")");
            newModel.setSimpleValue(iconUrl);
        }
    }

    /**
     * Once a view configuration is updated in the DB we must update the cached view configuration model.
     */
    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel, ClientState clientState,
                                                                                       Directives directives) {
        if (topic.getTypeUri().equals("dm4.webclient.view_config")) {
            Type type = getType(topic);
            logger.info("### Updating view configuration for topic type \"" + type.getUri() + "\" (" + topic + ")");
            type.getViewConfig().getModel().updateConfigTopic(topic.getModel());
            directives.add(Directive.UPDATE_TOPIC_TYPE, type);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Search ===

    private Set<Topic> findSearchableUnits(Set<Topic> topics, ClientState clientState) {
        Set<Topic> searchableUnits = new LinkedHashSet<Topic>();
        for (Topic topic : topics) {
            if (isSearchableUnit(topic)) {
                searchableUnits.add(topic);
            } else {
                ResultSet<RelatedTopic> relatedTopics = topic.getRelatedTopics((List<String>) null,
                    "dm4.core.part", "dm4.core.whole", null, false, false, 0, clientState);
                Set<Topic> parentTopics = DeepaMehtaUtils.<ResultSet<Topic>>cast(relatedTopics).getItems();
                if (parentTopics.isEmpty()) {
                    searchableUnits.add(topic);
                } else {
                    searchableUnits.addAll(findSearchableUnits(parentTopics, clientState));
                }
            }
        }
        return searchableUnits;
    }

    /**
     * Creates a "Search" topic (a bucket).
     */
    private Topic createSearchTopic(String searchTerm, Set<Topic> resultItems, ClientState clientState) {
        Topic searchTopic = dms.createTopic(new TopicModel("dm4.webclient.search", new CompositeValue()
            .put("dm4.webclient.search_term", searchTerm)
        ), clientState);
        //
        // associate result items
        logger.fine("Associating " + resultItems.size() + " result items to search (ID " + searchTopic.getId() + ")");
        for (Topic resultItem : resultItems) {
            logger.fine("Associating " + resultItem);
            dms.createAssociation(new AssociationModel("dm4.webclient.search_result_item",
                new TopicRoleModel(searchTopic.getId(), "dm4.core.default"),
                new TopicRoleModel(resultItem.getId(), "dm4.core.default")
            ), clientState);
        }
        return searchTopic;
    }

    // ---

    private boolean isSearchableUnit(Topic topic) {
        TopicType topicType = dms.getTopicType(topic.getTypeUri(), null);           // FIXME: clientState=null
        Boolean isSearchableUnit = (Boolean) getViewConfig(topicType, "is_searchable_unit");
        return isSearchableUnit != null ? isSearchableUnit.booleanValue() : false;  // default is false
    }

    /**
     * Read out a view configuration setting.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   topicType   The topic type whose view configuration is read out.
     * @param   setting     Last component of the setting URI, e.g. "icon".
     *
     * @return  The setting value, or <code>null</code> if there is no such setting
     */
    private Object getViewConfig(TopicType topicType, String setting) {
        return topicType.getViewConfig("dm4.webclient.view_config", "dm4.webclient." + setting);
    }

    // === View Configuration ===

    private Type getType(Topic viewConfig) {
        Topic typeTopic = viewConfig.getRelatedTopic("dm4.core.aggregation",
            "dm4.core.view_config", "dm4.core.type", null, false, false, null);
        return dms.getTopicType(typeTopic.getUri(), null);  // ### FIXME: handle assoc types
    }

    // === Webclient Start ===

    private String getWebclientUrl() {
        boolean isHttpsEnabled = Boolean.valueOf(System.getProperty("org.apache.felix.https.enable"));
        String protocol, port;
        if (isHttpsEnabled) {
            // Note: if both protocols are enabled HTTPS takes precedence
            protocol = "https";
            port = System.getProperty("org.osgi.service.http.port.secure");
        } else {
            protocol = "http";
            port = System.getProperty("org.osgi.service.http.port");
        }
        return protocol + "://localhost:" + port + "/de.deepamehta.webclient/";
    }
}
