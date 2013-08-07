package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;

import static java.util.Arrays.asList;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, IntroduceTopicTypeListener,
                                                                                    IntroduceAssociationTypeListener,
                                                                                    PostCreateTopicListener,
                                                                                    PostCreateAssociationListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_WORKSPACE_NAME = "DeepaMehta";
    private static final String DEFAULT_WORKSPACE_URI = "de.workspaces.deepamehta";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FacetsService facetsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** WorkspacesService Implementation ***
    // ****************************************



    @Override
    public Set<RelatedTopic> getAssignedWorkspaces(DeepaMehtaObject object) {
        return facetsService.getFacets(object, "dm4.workspaces.workspace_facet");
    }

    @Override
    public boolean isAssignedToWorkspace(Topic topic, long workspaceId) {
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
    public Topic createWorkspace(String name) {
        return createWorkspace(name, null);
    }

    @Override
    public Topic createWorkspace(String name, String uri) {
        logger.info("Creating workspace \"" + name + "\"");
        return dms.createTopic(new TopicModel(uri, "dm4.workspaces.workspace", new CompositeValueModel()
            .put("dm4.workspaces.name", name)
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
        createWorkspace(DEFAULT_WORKSPACE_NAME, DEFAULT_WORKSPACE_URI);
    }

    // ---

    @Override
    @ConsumesService("de.deepamehta.plugins.facets.service.FacetsService")
    public void serviceArrived(PluginService service) {
        facetsService = (FacetsService) service;
    }

    @Override
    public void serviceGone(PluginService service) {
        facetsService = null;
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
     * Every created topic is assigned to the current workspace.
     */
    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        long workspaceId = -1;
        try {
            // Note: we do not assign Workspaces to a workspace
            if (topic.getTypeUri().equals("dm4.workspaces.workspace")) {
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
     * Every created association is assigned to the current workspace.
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
        String workspaceId = clientState.get("dm4_workspace_id");
        if (workspaceId == null) {
            return -1;
        }
        //
        return Long.parseLong(workspaceId);
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
        // Note: workspace_facet is a multi-facet. So we must pass a (one-element) list.
        facetsService.updateFacets(object, "dm4.workspaces.workspace_facet", asList(new TopicModel(workspaceId)),
            null, new Directives());    // clientState=null
    }

    // --- Helper ---

    private Topic fetchDefaultWorkspace() {
        return dms.getTopic("uri", new SimpleValue(DEFAULT_WORKSPACE_URI), false);      // fetchComposite=false
    }

    private boolean isDeepaMehtaStandardType(Type type) {
        return type.getUri().startsWith("dm4.");
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
