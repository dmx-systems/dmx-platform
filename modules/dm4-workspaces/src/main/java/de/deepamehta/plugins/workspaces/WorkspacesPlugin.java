package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;

import static java.util.Arrays.asList;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, IntroduceTopicTypeListener,
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

    // ---

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        checkWorkspaceId(workspaceId);
        // Note: workspace_facet is a multi-facet. So we must pass a (one-element) list.
        facetsService.updateFacets(object, "dm4.workspaces.workspace_facet", asList(new TopicModel(workspaceId)),
            null, new Directives());    // clientState=null
    }

    // ---

    @Override
    public Set<RelatedTopic> getWorkspaces(DeepaMehtaObject object) {
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
            workspaceId = workspaceId(clientState);
            if (workspaceId != -1) {
                assignToWorkspace(topicType, workspaceId);
            } else {
                // assign types of the DeepaMehta standard distribution to the default workspace
                if (isDeepaMehtaStandardType(topicType)) {
                    Topic defaultWorkspace = fetchDefaultWorkspace();
                    // Note: the default workspace is NOT required to exist ### TODO: think about it
                    if (defaultWorkspace != null) {
                        workspaceId = defaultWorkspace.getId();
                        assignToWorkspace(topicType, workspaceId);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Assigning topic type \"" + topicType.getUri() + "\" to workspace " +
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

    private Topic fetchDefaultWorkspace() {
        return dms.getTopic("uri", new SimpleValue(DEFAULT_WORKSPACE_URI), false, null);    // fetchComposite=false
    }

    private boolean isDeepaMehtaStandardType(TopicType topicType) {
        return topicType.getUri().startsWith("dm4.");
    }

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

    /**
     * Checks if the topic with the specified ID exists and is a Workspace. If not, an exception is thrown.
     */
    private void checkWorkspaceId(long workspaceId) {
        String typeUri = dms.getTopic(workspaceId, false, null).getTypeUri();   // fetchComposite=false
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but of type \"" +
                typeUri + "\")");
        }
    }
}
