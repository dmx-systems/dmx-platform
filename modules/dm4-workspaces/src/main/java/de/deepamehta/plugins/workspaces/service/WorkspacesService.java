package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

import java.util.Set;



public interface WorkspacesService extends PluginService {

    Topic createWorkspace(String name);

    void assignTopic(long workspaceId, long topicId);

    void assignType(long workspaceId, long typeId);

    /**
     * Returns the workspaces a type is assigned to.
     *
     * Note: takes a type ID instead of a type URI to avoid endless recursion through dms.getTopicType().
     * Consider the Access Control plugin: determining the permissions for a type with MEMBER role would involve
     * retrieving the type itself. This in turn would involve determining its permissions ...
     * See AccessControlPlugin.userIsMember() ### TODO: still true?
     */
    Set<RelatedTopic> getWorkspaces(long typeId);
}
