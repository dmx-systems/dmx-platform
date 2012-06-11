package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    Topic lookupUserAccount(String username);

    Topic getUserAccount(ClientState clientState);

    // ---

    Topic getOwnedTopic(long userId, String typeUri);

    void setOwner(long topicId, long userId);

    // ---

    void createACLEntry(long topicId, Role role, Permissions permissions);
    void createACLEntry(Topic topic,  Role role, Permissions permissions);

    // ---

    void joinWorkspace(long workspaceId, long userId);
}
