package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    public Topic getUserAccount(ClientState clientState);

    public Topic getOwnedTopic(long userId, String typeUri);

    // ---

    public void setOwner(long topicId, long userId);

    public void createACLEntry(long topicId, Role role, Permissions permissions);

    // ---

    public void joinWorkspace(long workspaceId, long userId);
}
