package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.accesscontrol.SharingMode;



public interface WorkspacesService {

    // ------------------------------------------------------------------------------------------------------- Constants

    static final String DEEPAMEHTA_WORKSPACE_NAME = "DeepaMehta";
    static final String DEEPAMEHTA_WORKSPACE_URI = "dm4.workspaces.deepamehta";
    static final SharingMode DEEPAMEHTA_WORKSPACE_SHARING_MODE = SharingMode.PUBLIC;

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @param   uri     may be null
     */
    Topic createWorkspace(String name, String uri, SharingMode sharingMode);

    // ---

    /**
     * Returns a workspace by URI.
     *
     * @return  The workspace (a topic of type "Workspace").
     *
     * @throws  RuntimeException    If no workspace exists for the given URI.
     */
    Topic getWorkspace(String uri);

    /**
     * Returns all topics of the given type that are assigned to the given workspace.
     */
    ResultList<RelatedTopic> getAssignedTopics(long workspaceId, String typeUri);

    /**
     * Returns the workspace a topic or association is assigned to.
     *
     * @param   id      a topic ID, or an association ID
     *
     * @return  The assigned workspace (a topic of type "Workspace"),
     *          or <code>null</code> if no workspace is assigned.
     */
    Topic getAssignedWorkspace(long objectId);

    /**
     * Checks weather the given topic or association is assigned to the given workspace.
     */
    boolean isAssignedToWorkspace(long objectId, long workspaceId);

    // ---

    /**
     * Assigns the given object to the given workspace.
     */
    void assignToWorkspace(DeepaMehtaObject object, long workspaceId);

    /**
     * Assigns the given type and all its view configuration topics to the given workspace.
     */
    void assignTypeToWorkspace(Type type, long workspaceId);
}
