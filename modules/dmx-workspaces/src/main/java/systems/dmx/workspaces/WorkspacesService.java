package systems.dmx.workspaces;

import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RoleType;
import systems.dmx.core.Topic;
import systems.dmx.core.service.accesscontrol.SharingMode;

import java.util.List;



public interface WorkspacesService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // DMX workspace
    static final String      DMX_WORKSPACE_NAME = "DMX";
    static final String      DMX_WORKSPACE_URI = "dmx.workspaces.dmx";
    static final SharingMode DMX_WORKSPACE_SHARING_MODE = SharingMode.PUBLIC;

    // Property URIs
    static final String PROP_WORKSPACE_ID = "dmx.workspaces.workspace_id";

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @param   uri     may be null
     */
    Topic createWorkspace(String name, String uri, SharingMode sharingMode);

    void deleteWorkspace(long workspaceId);

    // ---

    /**
     * Returns a workspace by URI.
     *
     * This is a privileged method: READ permission for the workspace is not required.
     * ### TODO: why is this a privileged method?
     *
     * @return  The workspace (a topic of type "Workspace").
     *
     * @throws  RuntimeException    If no workspace exists for the given URI.
     */
    Topic getWorkspace(String uri);

    /**
     * Returns the workspace the given topic/association is assigned to.
     *
     * @param   objectId    a topic ID, or an association ID
     *
     * @return  The assigned workspace (a topic of type "Workspace"),
     *          or <code>null</code> if no workspace is assigned.
     *
     * @throws  RuntimeException    If no object with the given ID exists.
     * @throws  RuntimeException    If the current user has no READ permission for the workspace (and thus for the given
     *                              topic/association).
     */
    Topic getAssignedWorkspace(long objectId);

    // ---

    /**
     * Assigns an object to a workspace.
     *
     * @param   workspaceId     The ID of the workspace to assign the object to.
     *                          If -1 is given an existing workspace assignment is removed.
     */
    void assignToWorkspace(DMXObject object, long workspaceId);

    /**
     * Assigns a type as well as its "parts" to a workspace. In particular:
     * <ul>
     * <li>the type</li>
     * <li>the type's view config</li>
     * <li>the direct comp defs (not recursively)</li>
     * <li>the direct comp def's view configs</li>
     * </ul>
     *
     * @param   workspaceId     The ID of the workspace to assign the type (and its parts) to.
     *                          If -1 is given the existing workspace assignments are removed.
     */
    void assignTypeToWorkspace(DMXType type, long workspaceId);

    /**
     * Assigns a role type as well as its "parts" to a workspace. In particular:
     * <ul>
     * <li>the role type</li>
     * <li>the role type's view config</li>
     * </ul>
     *
     * @param   workspaceId     The ID of the workspace to assign the role type (and its parts) to.
     *                          If -1 is given the existing workspace assignments are removed.
     */
    void assignRoleTypeToWorkspace(RoleType roleType, long workspaceId);

    // ---

    /**
     * Returns all topics assigned to the given workspace.
     */
    List<Topic> getAssignedTopics(long workspaceId);

    /**
     * Returns all associations assigned to the given workspace.
     */
    List<Assoc> getAssignedAssocs(long workspaceId);

    // ---

    /**
     * Returns all topics of the given type that are assigned to the given workspace.
     */
    List<Topic> getAssignedTopics(long workspaceId, String topicTypeUri);

    /**
     * Returns all associations of the given type that are assigned to the given workspace.
     */
    List<Assoc> getAssignedAssocs(long workspaceId, String assocTypeUri);
}
