package de.deepamehta.webclient;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreCreateTopicTypeListener;
import de.deepamehta.core.service.event.PreCreateAssociationTypeListener;
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
                                                                PreCreateTopicTypeListener,
                                                                PreCreateAssociationTypeListener,
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
     * TODO: proper modularization. Either let the Type Search plugin provide its own REST service or make the
     * Type Search plugin an integral part of the Webclient plugin.
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
    @Path("/object/{id}/related_topics")
    public List<RelatedTopic> getRelatedTopics(@PathParam("id") long objectId) {
        DeepaMehtaObject object = dm4.getObject(objectId);
        List<RelatedTopic> relTopics = object.getRelatedTopics(null);   // assocTypeUri=null
        Iterator<RelatedTopic> i = relTopics.iterator();
        int removed = 0;
        while (i.hasNext()) {
            RelatedTopic relTopic = i.next();
            if (isDirectModelledChildTopic(object, relTopic)) {
                i.remove();
                removed++;
            }
        }
        logger.fine("### " + removed + " topics are removed from result set of object " + objectId);
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
            logger.info("### Launching webclient (url=\"" + webclientUrl + "\") SKIPPED -- already launched");
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

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateTopicType(TopicTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateAssociationType(AssociationTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Once a view configuration is updated in the DB we must update the cached view configuration model.
     */
    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
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
        Boolean searchableAsUnit = (Boolean) getViewConfigValue(topicType, "searchable_as_unit");
        return searchableAsUnit != null ? searchableAsUnit.booleanValue() : false;  // default is false
    }

    /**
     * Convenience method to lookup a Webclient view config value.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   topicType   The topic type whose view configuration is used for lookup.
     * @param   setting     Last component of the child type URI whose value to lookup, e.g. "icon".
     *
     * @return  The config value, or <code>null</code> if no value is set
     */
    private Object getViewConfigValue(TopicType topicType, String setting) {
        return topicType.getViewConfigValue("dm4.webclient.view_config", "dm4.webclient." + setting);
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
            // ### FIXME: handle association definitions
        }
    }

    // ---

    private void updateTopicType(Topic type, Topic viewConfig) {
        logger.info("### Updating view config of topic type \"" + type.getUri() + "\"");
        TopicType topicType = dm4.getTopicType(type.getUri());
        updateViewConfig(topicType, viewConfig);
        Directives.get().add(Directive.UPDATE_TOPIC_TYPE, topicType);           // ### TODO: should be implicit
    }

    private void updateAssociationType(Topic type, Topic viewConfig) {
        logger.info("### Updating view config of assoc type \"" + type.getUri() + "\"");
        AssociationType assocType = dm4.getAssociationType(type.getUri());
        updateViewConfig(assocType, viewConfig);
        Directives.get().add(Directive.UPDATE_ASSOCIATION_TYPE, assocType);     // ### TODO: should be implicit
    }

    // ---

    private void updateViewConfig(DeepaMehtaType type, Topic viewConfig) {
        type.getModel().getViewConfig().updateConfigTopic(viewConfig.getModel());
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

    // --- Default Value ---

    /**
     * Add a default view config topic to the given type model in case no one is set already.
     * <p>
     * This ensures a programmatically created type (through a migration) will
     * have a view config in any case, for being edited interactively afterwards.
     */
    private void addDefaultViewConfig(TypeModel typeModel) {
        // This would create View Config topics without any child topics.
        // Now with the ValueUpdater we can't create empty composites.
        // See also WebclientPlugin Migration3.java
        // ### TODO: rethink about this.
        /*
        ViewConfigurationModel viewConfig = typeModel.getViewConfig();
        TopicModel configTopic = viewConfig.getConfigTopic("dm4.webclient.view_config");
        if (configTopic == null) {
            viewConfig.addConfigTopic(mf.newTopicModel("dm4.webclient.view_config"));
        }
        */
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

    private boolean isDirectModelledChildTopic(DeepaMehtaObject parentObject, RelatedTopic childTopic) {
        // association definition
        if (hasAssocDef(parentObject, childTopic)) {
            // role types
            Association assoc = childTopic.getRelatingAssociation();
            return assoc.matches("dm4.core.parent", parentObject.getId(), "dm4.core.child", childTopic.getId());
        }
        return false;
    }

    private boolean hasAssocDef(DeepaMehtaObject parentObject, RelatedTopic childTopic) {
        // Note: the user might have no explicit READ permission for the type.
        // DeepaMehtaObject's getType() has *implicit* READ permission.
        DeepaMehtaType parentType = parentObject.getType();
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
