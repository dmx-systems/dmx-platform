package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.service.PluginService;

import java.util.Set;



public interface WorkspacesService extends PluginService {

    /**
     * Fetches the workspaces the specified object is assigned to.
     */
    Set<RelatedTopic> getAssignedWorkspaces(DeepaMehtaObject object);

    /**
     * Checks weather the specified topic is assigned to the specified workspace
     */
    boolean isAssignedToWorkspace(Topic topic, long workspaceId);

    // ---

    /**
     * Fetches the default workspace ("DeepaMehta").
     *
     * @return  The default workspace (a topic of type "Workspace" / "dm4.workspaces.workspace"),
     *          or <code>null</code> if it doesn't exist.
     *          Note: the default workspace is NOT required to exist ### TODO: think about it
     */
    Topic getDefaultWorkspace();

    // ---

    /**
     * Assigns the specified object to a workspace.
     */
    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);

    /**
     * Assigns the specified type and all its view configuration topics to a workspace.
     */
    void assignTypeToWorkspace(Type type, long workspaceId);

    // ---

    Topic createWorkspace(String name);

    Topic createWorkspace(String name, String uri);
}
