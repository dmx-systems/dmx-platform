package de.deepamehta.plugins.workspaces.service;

import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;

import java.util.List;



public interface WorkspacesService {

    public Topic createWorkspace(String name);

    public void assignType(long workspaceId, long typeId);

    /**
     * Returns the workspaces a type is assigned to.
     */
    public List<RelatedTopic> getWorkspaces(long typeId);
}
