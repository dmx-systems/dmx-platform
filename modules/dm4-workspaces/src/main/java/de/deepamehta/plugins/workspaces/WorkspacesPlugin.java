package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.listener.IntroduceTopicTypeListener;
import de.deepamehta.core.service.listener.PluginServiceArrivedListener;
import de.deepamehta.core.service.listener.PluginServiceGoneListener;
import de.deepamehta.core.service.listener.PostCreateTopicListener;
import de.deepamehta.core.service.listener.PostInstallPluginListener;

import static java.util.Arrays.asList;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, PostInstallPluginListener,
                                                                                    IntroduceTopicTypeListener,
                                                                                    PostCreateTopicListener,
                                                                                    PluginServiceArrivedListener,
                                                                                    PluginServiceGoneListener {

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
        return dms.createTopic(new TopicModel(uri, "dm4.workspaces.workspace", new CompositeValue()
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
        createWorkspace(DEFAULT_WORKSPACE_NAME, DEFAULT_WORKSPACE_URI);
    }

    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        try {
            long workspaceId = workspaceId(clientState);
            if (workspaceId != -1) {
                assignWorkspace(topicType, workspaceId);
            } else if (isDeepaMehtaStandardType(topicType)) {
                Topic defaultWorkspace = fetchDefaultWorkspace();
                if (defaultWorkspace != null) {
                    assignWorkspace(topicType, defaultWorkspace.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Assigning a workspace to topic type \"" + topicType.getUri() + "\" failed", e);
        }
    }

    // ---

    /**
     * Assigns a every created topic to the current workspace.
     */
    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        long workspaceId = -1;
        try {
            // Note: we do not assign a workspace to Searches and to Workspaces
            if (topic.getTypeUri().equals("dm4.webclient.search") ||
                topic.getTypeUri().equals("dm4.workspaces.workspace")) {
                return;
            }
            //
            workspaceId = workspaceId(clientState);
            //
            if (workspaceId == -1) {
                return;
            }
            //
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

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = dms.getTopic(workspaceId, false, null).getTypeUri();   // fetchComposite=false
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but of type \"" +
                typeUri + "\")");
        }
    }
}
