package de.deepamehta.plugins.webclient;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreUpdateTopicListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.DeepaMehtaUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



@Path("/webclient")
@Consumes("application/json")
@Produces("application/json")
public class WebclientPlugin extends PluginActivator implements AllPluginsActiveListener,
                                                                IntroduceTopicTypeListener,
                                                                IntroduceAssociationTypeListener,
                                                                PreUpdateTopicListener,
                                                                PostUpdateTopicListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String VIEW_CONFIG_LABEL = "View Configuration";

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
            Set<Topic> singleTopics = dms.searchTopics(searchTerm, fieldUri);
            Set<Topic> topics = findSearchableUnits(singleTopics);
            logger.info(singleTopics.size() + " single topics found, " + topics.size() + " searchable units");
            //
            Topic searchTopic = createSearchTopic(searchTerm, topics, clientState);
            tx.success();
            return searchTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed", e);
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
            String searchTerm = dms.getTopicType(typeUri).getSimpleValue() + "(s)";
            Set<RelatedTopic> topics = dms.getTopics(typeUri, false, maxResultSize).getItems();
            // fetchComposite=false
            //
            Topic searchTopic = createSearchTopic(searchTerm, topics, clientState);
            tx.success();
            return searchTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed", e);
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
            updateType(topic, directives);
            setConfigTopicLabel(topic);
        }
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        setViewConfigLabel(topicType.getViewConfig());
    }

    @Override
    public void introduceAssociationType(AssociationType assocType, ClientState clientState) {
        setViewConfigLabel(assocType.getViewConfig());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Search ===

    // ### TODO: use Collection instead of Set
    private Set<Topic> findSearchableUnits(Set<? extends Topic> topics) {
        Set<Topic> searchableUnits = new LinkedHashSet();
        for (Topic topic : topics) {
            if (searchableAsUnit(topic)) {
                searchableUnits.add(topic);
            } else {
                Set<RelatedTopic> parentTopics = topic.getRelatedTopics((String) null, "dm4.core.child",
                    "dm4.core.parent", null, false, false, 0).getItems();
                if (parentTopics.isEmpty()) {
                    searchableUnits.add(topic);
                } else {
                    searchableUnits.addAll(findSearchableUnits(parentTopics));
                }
            }
        }
        return searchableUnits;
    }

    /**
     * Creates a "Search" topic (a bucket).
     */
    private Topic createSearchTopic(String searchTerm, Set<? extends Topic> resultItems, ClientState clientState) {
        Topic searchTopic = dms.createTopic(new TopicModel("dm4.webclient.search", new CompositeValueModel()
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

    private boolean searchableAsUnit(Topic topic) {
        TopicType topicType = dms.getTopicType(topic.getTypeUri());
        Boolean searchableAsUnit = (Boolean) getViewConfig(topicType, "searchable_as_unit");
        return searchableAsUnit != null ? searchableAsUnit.booleanValue() : false;  // default is false
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

    private void updateType(Topic viewConfig, Directives directives) {
        Topic type = viewConfig.getRelatedTopic("dm4.core.aggregation", "dm4.core.view_config", "dm4.core.type", null,
            false, false);
        if (type != null) {
            String typeUri = type.getTypeUri();
            if (typeUri.equals("dm4.core.topic_type") || typeUri.equals("dm4.core.meta_type")) {
                updateTopicType(type, viewConfig, directives);
            } else if (typeUri.equals("dm4.core.assoc_type")) {
                updateAssociationType(type, viewConfig, directives);
            } else {
                throw new RuntimeException("View Configuration " + viewConfig.getId() + " is associated to an " +
                    "unexpected topic (type=" + type + "\nviewConfig=" + viewConfig + ")");
            }
        } else {
            // ### TODO: association definitions
        }
    }

    // ---

    private void updateTopicType(Topic type, Topic viewConfig, Directives directives) {
        logger.info("### Updating view configuration of topic type \"" + type.getUri() + "\" (viewConfig=" +
            viewConfig + ")");
        TopicType topicType = dms.getTopicType(type.getUri());
        updateViewConfig(topicType, viewConfig);
        directives.add(Directive.UPDATE_TOPIC_TYPE, topicType);
    }

    private void updateAssociationType(Topic type, Topic viewConfig, Directives directives) {
        logger.info("### Updating view configuration of association type \"" + type.getUri() + "\" (viewConfig=" +
            viewConfig + ")");
        AssociationType assocType = dms.getAssociationType(type.getUri());
        updateViewConfig(assocType, viewConfig);
        directives.add(Directive.UPDATE_ASSOCIATION_TYPE, assocType);
    }

    // ---

    private void updateViewConfig(Type type, Topic viewConfig) {
        type.getViewConfig().updateConfigTopic(viewConfig.getModel());
    }

    // --- Label ---

    private void setViewConfigLabel(ViewConfiguration viewConfig) {
        for (Topic configTopic : viewConfig.getConfigTopics()) {
            setConfigTopicLabel(configTopic);
        }
    }

    private void setConfigTopicLabel(Topic viewConfig) {
        viewConfig.setSimpleValue(VIEW_CONFIG_LABEL);
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
