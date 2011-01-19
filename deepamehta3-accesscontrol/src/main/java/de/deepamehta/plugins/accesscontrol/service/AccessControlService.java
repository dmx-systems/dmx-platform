package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.AccessControlPlugin.Role;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.PluginService;

import java.util.Map;



public interface AccessControlService extends PluginService {

    public Topic getUser(ClientContext clientContext);

    public Topic getTopicByOwner(long userId, String typeUri);

    // ---

    public void setOwner(long topicId, long userId);

    public void createACLEntry(long topicId, Role role, Permissions permissions);

    // ---

    public void joinWorkspace(long workspaceId, long userId);
}
