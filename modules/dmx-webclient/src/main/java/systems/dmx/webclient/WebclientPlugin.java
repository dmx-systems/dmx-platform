package systems.dmx.webclient;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Role;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Directive;
import systems.dmx.core.service.Directives;
import systems.dmx.core.service.event.AllPluginsActiveListener;
import systems.dmx.core.service.event.IntroduceTopicTypeListener;
import systems.dmx.core.service.event.IntroduceAssociationTypeListener;
import systems.dmx.core.service.event.PostUpdateTopicListener;
import systems.dmx.core.service.event.PreCreateTopicTypeListener;
import systems.dmx.core.service.event.PreCreateAssociationTypeListener;
import systems.dmx.core.service.Transactional;

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
            List<Topic> singleTopics = dmx.searchTopics(searchTerm, fieldUri);
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
            String searchTerm = dmx.getTopicType(typeUri).getSimpleValue() + "(s)";
            List<Topic> topics = dmx.getTopicsByType(typeUri);
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
        DMXObject object = dmx.getObject(objectId);
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
        if (topic.getTypeUri().equals("dmx.webclient.view_config")) {
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
                List<RelatedTopic> parentTopics = topic.getRelatedTopics((String) null, "dmx.core.child",
                    "dmx.core.parent", null);
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
            return dmx.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    Topic searchTopic = dmx.createTopic(mf.newTopicModel("dmx.webclient.search",
                        mf.newChildTopicsModel().put("dmx.webclient.search_term", searchTerm)
                    ));
                    // associate result items
                    for (Topic resultItem : resultItems) {
                        dmx.createAssociation(mf.newAssociationModel("dmx.webclient.search_result_item",
                            mf.newTopicRoleModel(searchTopic.getId(), "dmx.core.default"),
                            mf.newTopicRoleModel(resultItem.getId(), "dmx.core.default")
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
        TopicType topicType = dmx.getTopicType(topic.getTypeUri());
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
        return topicType.getViewConfigValue("dmx.webclient.view_config", "dmx.webclient." + setting);
    }



    // === View Configuration ===

    /**
     * Updates type cache once a view config topic has been updated, and adds an UPDATE-TYPE directive.
     */
    private void updateType(Topic viewConfig) {
        // type to be updated
        Topic type = viewConfig.getRelatedTopic("dmx.core.composition", "dmx.core.view_config", "dmx.core.type", null);
        // ID of the assoc def to be updated.
        // -1 if the update does not target an assoc def (but a type)
        long assocDefId = -1;
        if (type == null) {
            Association assocDef = viewConfig.getRelatedAssociation("dmx.core.composition", "dmx.core.view_config",
                "dmx.core.assoc_def", null);
            if (assocDef == null) {
                throw new RuntimeException("Orphaned view config: " + viewConfig);
            }
            type = (Topic) assocDef.getPlayer("dmx.core.parent_type");
            assocDefId = assocDef.getId();
        }
        //
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dmx.core.topic_type") || typeUri.equals("dmx.core.meta_type")) {
            updateType(
                dmx.getTopicType(type.getUri()),
                assocDefId, viewConfig, Directive.UPDATE_TOPIC_TYPE
            );
        } else if (typeUri.equals("dmx.core.assoc_type")) {
            updateType(
                dmx.getAssociationType(type.getUri()),
                assocDefId, viewConfig, Directive.UPDATE_ASSOCIATION_TYPE
            );
        } else {
            throw new RuntimeException("View config " + viewConfig.getId() + " is associated unexpectedly, type=" +
                type + ", assocDefId=" + assocDefId + ", viewConfig=" + viewConfig);
        }
    }

    private void updateType(DMXType type, long assocDefId, Topic viewConfig, Directive dir) {
        logger.info("### Updating view config of type \"" + type.getUri() + "\" (assocDefId=" + assocDefId + ")");
        updateViewConfig(type, assocDefId, viewConfig);
        Directives.get().add(dir, type);        // ### TODO: should be implicit
    }

    private void updateViewConfig(DMXType type, long assocDefId, Topic viewConfig) {
        ViewConfigurationModel vcm;
        if (assocDefId == -1) {
            vcm = type.getModel().getViewConfig();
        } else {
            vcm = getAssocDef(type.getModel(), assocDefId).getViewConfig();
        }
        vcm.updateConfigTopic(viewConfig.getModel());
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
        // Now with the ValueIntegrator we can't create empty composites.
        // See also WebclientPlugin Migration3.java
        // ### TODO: rethink about this.
        /*
        ViewConfigurationModel viewConfig = typeModel.getViewConfig();
        TopicModel configTopic = viewConfig.getConfigTopic("dmx.webclient.view_config");
        if (configTopic == null) {
            viewConfig.addConfigTopic(mf.newTopicModel("dmx.webclient.view_config"));
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
        return protocol + "://localhost:" + port + "/systems.dmx.webclient/";
    }



    // === Misc ===

    private boolean isDirectModelledChildTopic(DMXObject parentObject, RelatedTopic childTopic) {
        // association definition
        if (hasAssocDef(parentObject, childTopic)) {
            // role types
            Association assoc = childTopic.getRelatingAssociation();
            return assoc.matches("dmx.core.parent", parentObject.getId(), "dmx.core.child", childTopic.getId());
        }
        return false;
    }

    private boolean hasAssocDef(DMXObject parentObject, RelatedTopic childTopic) {
        // Note: the user might have no explicit READ permission for the type.
        // DMXObject's getType() has *implicit* READ permission.
        DMXType parentType = parentObject.getType();
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

    private AssociationDefinitionModel getAssocDef(TypeModel type, long assocDefId) {
        for (AssociationDefinitionModel assocDef : type.getAssocDefs()) {
            if (assocDef.getId() == assocDefId) {
                return assocDef;
            }
        }
        throw new RuntimeException("Assoc def " + assocDefId + " not found in type \"" + type.getUri() + "\"");
    }
}
