package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.model.FacetValue;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import java.util.logging.Logger;



public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, IntroduceTopicTypeListener,
                                                                                    IntroduceAssociationTypeListener,
                                                                                    PostCreateTopicListener,
                                                                                    PostCreateAssociationListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_WORKSPACE_NAME = "DeepaMehta";
    private static final String DEFAULT_WORKSPACE_URI = "de.workspaces.deepamehta";     // ### TODO: "dm4.workspaces..."
    private static final String DEFAULT_WORKSPACE_TYPE_URI = "dm4.workspaces.type.public";

    // Property URIs
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private FacetsService facetsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** WorkspacesService Implementation ***
    // ****************************************



    @Override
    public Topic getAssignedWorkspace(long id) {
        if (!dms.hasProperty(id, PROP_WORKSPACE_ID)) {
            return null;
        }
        //
        long workspaceId = (Long) dms.getProperty(id, PROP_WORKSPACE_ID);
        return dms.getTopic(workspaceId, true);     // fetchComposite=true
    }

    @Override
    public boolean isAssignedToWorkspace(Topic topic, long workspaceId) {
        // ### TODO: check property instead facet
        return facetsService.hasFacet(topic.getId(), "dm4.workspaces.workspace_facet", workspaceId);
    }

    // ---

    @Override
    public Topic getDefaultWorkspace() {
        return fetchDefaultWorkspace();
    }

    // ---

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        checkArgument(workspaceId);
        //
        _assignToWorkspace(object, workspaceId);
    }

    @Override
    public void assignTypeToWorkspace(Type type, long workspaceId) {
        checkArgument(workspaceId);
        //
        _assignToWorkspace(type, workspaceId);
        for (Topic configTopic : type.getViewConfig().getConfigTopics()) {
            _assignToWorkspace(configTopic, workspaceId);
        }
    }

    // ---

    @Override
    public Topic createWorkspace(String name, String uri, String workspaceTypeUri) {
        logger.info("Creating workspace \"" + name + "\"");
        return dms.createTopic(new TopicModel(uri, "dm4.workspaces.workspace", new CompositeValueModel()
            .put("dm4.workspaces.name", name)
            .putRef("dm4.workspaces.type", workspaceTypeUri)
        ), null);   // FIXME: clientState=null
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    /**
     * Creates the "Default" workspace.
     */
    @Override
    public void postInstall() {
        createWorkspace(DEFAULT_WORKSPACE_NAME, DEFAULT_WORKSPACE_URI, DEFAULT_WORKSPACE_TYPE_URI);
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        long workspaceId = -1;
        try {
            workspaceId = workspaceIdForType(topicType, clientState);
            if (workspaceId == -1) {
                return;
            }
            //
            assignTypeToWorkspace(topicType, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning topic type \"" + topicType.getUri() + "\" to workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public void introduceAssociationType(AssociationType assocType, ClientState clientState) {
        long workspaceId = -1;
        try {
            workspaceId = workspaceIdForType(assocType, clientState);
            if (workspaceId == -1) {
                return;
            }
            //
            assignTypeToWorkspace(assocType, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning association type \"" + assocType.getUri() + "\" to workspace " +
                workspaceId + " failed", e);
        }
    }

    // ---

    /**
     * Assigns every created topic to the current workspace.
     */
    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        long workspaceId = -1;
        try {
            // Note: we must avoid vicious circles
            if (isWorkspacesPluginTopic(topic)) {
                return;
            }
            //
            workspaceId = workspaceId(clientState);
            // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
            // the default workspace. This would not help in gaining data consistency because the topics created so far
            // (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
            // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
            // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
            // the other hand we don't have such a mechanism (and don't want one either).
            if (workspaceId == -1) {
                return;
            }
            //
            assignToWorkspace(topic, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning topic " + topic.getId() + " to workspace " + workspaceId +
                " failed", e);
        }
    }

    /**
     * Assigns every created association to the current workspace.
     */
    @Override
    public void postCreateAssociation(Association assoc, ClientState clientState, Directives directives) {
        long workspaceId = -1;
        try {
            workspaceId = workspaceId(clientState);
            // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
            // the default workspace. This would not help in gaining data consistency because the associations created
            // so far (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
            // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
            // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
            // the other hand we don't have such a mechanism (and don't want one either).
            if (workspaceId == -1) {
                return;
            }
            //
            assignToWorkspace(assoc, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning association " + assoc.getId() + " to workspace " + workspaceId +
                " failed", e);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private long workspaceId(ClientState clientState) {
        if (clientState == null) {
            return -1;
        }
        //
        if (!clientState.has("dm4_workspace_id")) {
            return -1;
        }
        //
        return clientState.getLong("dm4_workspace_id");
    }

    private long workspaceIdForType(Type type, ClientState clientState) {
        long workspaceId = workspaceId(clientState);
        if (workspaceId != -1) {
            return workspaceId;
        } else {
            // assign types of the DeepaMehta standard distribution to the default workspace
            if (isDeepaMehtaStandardType(type)) {
                Topic defaultWorkspace = fetchDefaultWorkspace();
                // Note: the default workspace is NOT required to exist ### TODO: think about it
                if (defaultWorkspace != null) {
                    return defaultWorkspace.getId();
                }
            }
        }
        return -1;
    }

    // ---

    private void _assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        // 1) create assignment association
        // Note 1: we are refering to an existing workspace. So we must add a topic reference.
        // Note 2: workspace_facet is a multi-facet. So we must call addRef() (as opposed to putRef()).
        FacetValue value = new FacetValue("dm4.workspaces.workspace").addRef(workspaceId);
        facetsService.updateFacet(object, "dm4.workspaces.workspace_facet", value, null, new Directives());
        // clientState=null
        //
        // 2) store assignment property
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Storing workspace assignment of object " + object.getId() +
                " failed (workspaceId=" + workspaceId + ")", e);
        } finally {
            tx.finish();
        }
    }

    // --- Helper ---

    private boolean isDeepaMehtaStandardType(Type type) {
        return type.getUri().startsWith("dm4.");
    }

    private boolean isWorkspacesPluginTopic(Topic topic) {
        return topic.getTypeUri().startsWith("dm4.workspaces.");
    }

    // ---

    private Topic fetchDefaultWorkspace() {
        return dms.getTopic("uri", new SimpleValue(DEFAULT_WORKSPACE_URI), false);      // fetchComposite=false
    }

    /**
     * Checks if the topic with the specified ID exists and is a Workspace. If not, an exception is thrown.
     */
    private void checkArgument(long topicId) {
        String typeUri = dms.getTopic(topicId, false).getTypeUri();     // fetchComposite=false
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + topicId + " is not a workspace (but of type \"" + typeUri +
                "\")");
        }
    }
}
