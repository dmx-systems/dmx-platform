package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.service.PluginService;



public interface WorkspacesService extends PluginService {

    /**
     * Returns the workspace a topic or association is assigned to.
     *
     * @param   id      a topic ID, or an association ID
     *
     * @return  The assigned workspace (a "Workspace" topic, including its child topics),
     *          or <code>null</code> if no workspace is assigned.
     */
    Topic getAssignedWorkspace(long id);

    /**
     * Checks weather the specified topic is assigned to the specified workspace
     */
    boolean isAssignedToWorkspace(Topic topic, long workspaceId);

    // ---

    /**
     * Fetches the default workspace ("DeepaMehta").
     *
     * @return  The default workspace (a topic of type "Workspace"),
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

    /**
     * @param   uri     may be null
     */
    Topic createWorkspace(String name, String uri, String workspaceTypeUri);
}
