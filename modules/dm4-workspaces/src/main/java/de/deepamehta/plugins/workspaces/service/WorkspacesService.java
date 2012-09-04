package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

import java.util.Set;



public interface WorkspacesService extends PluginService {

    Topic createWorkspace(String name);

    // ---

    /**
     * Assigns a workspace to the specified object.
     */
    void assignWorkspace(DeepaMehtaObject object, long workspaceId);

    /**
     * Fetches the workspaces assigned to the specified object.
     */
    Set<RelatedTopic> getAssignedWorkspaces(DeepaMehtaObject object);
}
