package de.deepamehta.webclient;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreUpdateTopicListener;
import de.deepamehta.core.service.Transactional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.awt.Desktop;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
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
     * Performs a fulltext search and creates a search result topic.
     */
    @GET
    @Path("/search")
    @Transactional
    public Topic searchTopics(@QueryParam("search") String searchTerm, @QueryParam("field")  String fieldUri) {
        try {
            logger.info("searchTerm=\"" + searchTerm + "\", fieldUri=\"" + fieldUri + "\"");
            List<Topic> singleTopics = dm4.searchTopics(searchTerm, fieldUri);
            Set<Topic> topics = findSearchableUnits(singleTopics);
            logger.info(singleTopics.size() + " single topics found, " + topics.size() + " searchable units");
            //
            return createSearchTopic(searchTerm, topics);
        } catch (Exception e) {
            throw new RuntimeException("Searching topics failed", e);
        }
    }

    /**
     * Performs a by-type search and creates a search result topic.
     * <p>
     * Note: this resource method is actually part of the Type Search plugin.
     * TODO: proper modularization. Either let the Type Search plugin provide its own REST resource (with
     * another namespace again) or make the Type Search plugin an integral part of the Webclient plugin.
     */
    @GET
    @Path("/search/by_type/{type_uri}")
    @Transactional
    public Topic getTopics(@PathParam("type_uri") String typeUri) {
        try {
            logger.info("typeUri=\"" + typeUri + "\"");
            String searchTerm = dm4.getTopicType(typeUri).getSimpleValue() + "(s)";
            List<Topic> topics = dm4.getTopicsByType(typeUri);
            //
            return createSearchTopic(searchTerm, topics);
        } catch (Exception e) {
            throw new RuntimeException("Searching topics of type \"" + typeUri + "\" failed", e);
        }
    }

    // ---

    @GET
    @Path("/topic/{id}/related_topics")
    public List<RelatedTopic> getRelatedTopics(@PathParam("id") long topicId) {
        Topic topic = dm4.getTopic(topicId);
        List<RelatedTopic> relTopics = topic.getRelatedTopics(null);   // assocTypeUri=null
        Iterator<RelatedTopic> i = relTopics.iterator();
        int removed = 0;
        while (i.hasNext()) {
            RelatedTopic relTopic = i.next();
            if (isDirectModelledChildTopic(topic, relTopic)) {
                i.remove();
                removed++;
            }
        }
        logger.fine("### " + removed + " topics are removed from result set of topic " + topicId);
        return relTopics;
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
    public void preUpdateTopic(Topic topic, TopicModel newModel) {
        if (topic.getTypeUri().equals("dm4.files.file") && newModel.getTypeUri().equals("dm4.webclient.icon")) {
            String iconUrl = "/filerepo/" + topic.getChildTopics().getString("dm4.files.path");
            logger.info("### Retyping a file to an icon (iconUrl=" + iconUrl + ")");
            newModel.setSimpleValue(iconUrl);
        }
    }

    /**
     * Once a view configuration is updated in the DB we must update the cached view configuration model.
     */
    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel) {
        if (topic.getTypeUri().equals("dm4.webclient.view_config")) {
            updateType(topic);
            setConfigTopicLabel(topic);
        }
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType) {
        setViewConfigLabel(topicType.getViewConfig());
    }

    @Override
    public void introduceAssociationType(AssociationType assocType) {
        setViewConfigLabel(assocType.getViewConfig());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Search ===

    // ### TODO: use Collection instead of Set
    private Set<Topic> findSearchableUnits(List<? extends Topic> topics) {
        Set<Topic> searchableUnits = new LinkedHashSet();
        for (Topic topic : topics) {
            if (searchableAsUnit(topic)) {
                searchableUnits.add(topic);
            } else {
                List<RelatedTopic> parentTopics = topic.getRelatedTopics((String) null, "dm4.core.child",
                    "dm4.core.parent", null);
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
     * Creates a "Search" topic.
     */
    private Topic createSearchTopic(final String searchTerm, final Collection<Topic> resultItems) {
        try {
            // We suppress standard workspace assignment here as a Search topic requires a special assignment.
            // That is done by the Access Control module. ### TODO: refactoring. Do the assignment here.
            return dm4.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    Topic searchTopic = dm4.createTopic(mf.newTopicModel("dm4.webclient.search",
                        mf.newChildTopicsModel().put("dm4.webclient.search_term", searchTerm)
                    ));
                    // associate result items
                    for (Topic resultItem : resultItems) {
                        dm4.createAssociation(mf.newAssociationModel("dm4.webclient.search_result_item",
                            mf.newTopicRoleModel(searchTopic.getId(), "dm4.core.default"),
                            mf.newTopicRoleModel(resultItem.getId(), "dm4.core.default")
                        ));
                    }
                    //
                    return searchTopic;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Creating search topic for \"" + searchTerm + "\" failed", e);
        }
    }

    // ---

    private boolean searchableAsUnit(Topic topic) {
        TopicType topicType = dm4.getTopicType(topic.getTypeUri());
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

    private void updateType(Topic viewConfig) {
        Topic type = viewConfig.getRelatedTopic("dm4.core.aggregation", "dm4.core.view_config", "dm4.core.type", null);
        if (type != null) {
            String typeUri = type.getTypeUri();
            if (typeUri.equals("dm4.core.topic_type") || typeUri.equals("dm4.core.meta_type")) {
                updateTopicType(type, viewConfig);
            } else if (typeUri.equals("dm4.core.assoc_type")) {
                updateAssociationType(type, viewConfig);
            } else {
                throw new RuntimeException("View Configuration " + viewConfig.getId() + " is associated to an " +
                    "unexpected topic (type=" + type + "\nviewConfig=" + viewConfig + ")");
            }
        } else {
            // ### TODO: association definitions
        }
    }

    // ---

    private void updateTopicType(Topic type, Topic viewConfig) {
        logger.info("### Updating view configuration of topic type \"" + type.getUri() + "\" (viewConfig=" +
            viewConfig + ")");
        TopicType topicType = dm4.getTopicType(type.getUri());
        updateViewConfig(topicType, viewConfig);
        Directives.get().add(Directive.UPDATE_TOPIC_TYPE, topicType);           // ### TODO: should be implicit
    }

    private void updateAssociationType(Topic type, Topic viewConfig) {
        logger.info("### Updating view configuration of association type \"" + type.getUri() + "\" (viewConfig=" +
            viewConfig + ")");
        AssociationType assocType = dm4.getAssociationType(type.getUri());
        updateViewConfig(assocType, viewConfig);
        Directives.get().add(Directive.UPDATE_ASSOCIATION_TYPE, assocType);     // ### TODO: should be implicit
    }

    // ---

    private void updateViewConfig(DeepaMehtaType type, Topic viewConfig) {
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
        boolean isHttpsEnabled = Boolean.getBoolean("org.apache.felix.https.enable");
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



    // === Misc ===

    private boolean isDirectModelledChildTopic(Topic parentTopic, RelatedTopic childTopic) {
        // association definition
        if (hasAssocDef(parentTopic, childTopic)) {
            // role types
            Association assoc = childTopic.getRelatingAssociation();
            if (assoc.isPlayer(mf.newTopicRoleModel(parentTopic.getId(), "dm4.core.parent")) &&
                assoc.isPlayer(mf.newTopicRoleModel(childTopic.getId(),  "dm4.core.child"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAssocDef(Topic parentTopic, RelatedTopic childTopic) {
        // Note: the user might have no explicit READ permission for the type.
        // We must enforce the *implicit* READ permission.
        TopicType parentType = dm4.getTopicTypeImplicitly(parentTopic.getId());
        //
        String childTypeUri = childTopic.getTypeUri();
        String assocTypeUri = childTopic.getRelatingAssociation().getTypeUri();
        String assocDefUri = childTypeUri + "#" + assocTypeUri;
        if (parentType.hasAssocDef(assocDefUri)) {
            return true;
        } else if (parentType.hasAssocDef(childTypeUri)) {
            return parentType.getAssocDef(childTypeUri).getInstanceLevelAssocTypeUri().equals(assocTypeUri);
        }
        return false;
    }
}
