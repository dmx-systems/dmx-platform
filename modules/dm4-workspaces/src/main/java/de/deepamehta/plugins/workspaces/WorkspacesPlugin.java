package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.listener.PluginServiceArrivedListener;
import de.deepamehta.core.service.listener.PluginServiceGoneListener;
import de.deepamehta.core.service.listener.PostCreateTopicListener;
import de.deepamehta.core.service.listener.PostInstallPluginListener;

import static java.util.Arrays.asList;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, PostInstallPluginListener,
                                                                                    PostCreateTopicListener,
                                                                                    PluginServiceArrivedListener,
                                                                                    PluginServiceGoneListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_WORKSPACE_NAME = "Default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FacetsService facetsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** WorkspacesService Implementation ***
    // ****************************************



    @Override
    public Topic createWorkspace(String name) {
        logger.info("Creating workspace \"" + name + "\"");
        return dms.createTopic(new TopicModel("dm4.workspaces.workspace", new CompositeValue()
            .put("dm4.workspaces.name", name)
        ), null);   // FIXME: clientState=null
    }

    // ---

    @Override
    public void assignWorkspace(DeepaMehtaObject object, long workspaceId) {
        checkWorkspaceId(workspaceId);
        facetsService.updateFacet(object, "dm4.workspaces.workspace_facet", new TopicModel(workspaceId), null, null);
    }

    @Override
    public Set<RelatedTopic> getAssignedWorkspaces(DeepaMehtaObject object) {
        return facetsService.getFacets(object, "dm4.workspaces.workspace_facet");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    /**
     * Creates the "Default" workspace.
     */
    @Override
    public void postInstallPlugin() {
        createWorkspace(DEFAULT_WORKSPACE_NAME);
    }

    /**
     * Assigns a newly created topic to the current workspace.
     */
    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        long workspaceId = -1;
        try {
            // check precondition 1
            if (topic.getTypeUri().equals("dm4.webclient.search") ||
                topic.getTypeUri().equals("dm4.workspaces.workspace")) {
                // Note 1: we do not relate search topics to a workspace.
                // Note 2: we do not relate workspaces to a workspace.
                logger.info("Assigning topic to a workspace ABORTED: searches and workspaces are not assigned (" +
                    topic + ")");
                return;
            }
            // check precondition 2
            if (clientState == null) {
                // ### logger.warning("Assigning " + topic + " to a workspace failed (current workspace is unknown " +
                // ###     "(client context is not initialzed))");
                return;
            }
            // check precondition 3
            String wsId = clientState.get("dm4_workspace_id");
            if (wsId == null) {
                logger.warning("Assigning " + topic + " to a workspace failed (current workspace is unknown " +
                    "(no setting found in client context))");
                return;
            }
            // assign topic to workspace
            workspaceId = Long.parseLong(wsId);
            assignWorkspace(topic, workspaceId);
        } catch (Exception e) {
            logger.warning("Assigning topic " + topic.getId() + " to workspace " + workspaceId + " failed (" + e +
                ").\n    => This can happen after a DB reset if there is a stale \"dm4_workspace_id\" browser cookie.");
        }
    }

    // ---

    @Override
    public void pluginServiceArrived(PluginService service) {
        logger.info("########## Service arrived: " + service);
        if (service instanceof FacetsService) {
            facetsService = (FacetsService) service;
        }
    }

    @Override
    public void pluginServiceGone(PluginService service) {
        logger.info("########## Service gone: " + service);
        if (service == facetsService) {
            facetsService = null;
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = dms.getTopic(workspaceId, false, null).getTypeUri();   // fetchComposite=false
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but of type \"" +
                typeUri + "\")");
        }
    }
}
