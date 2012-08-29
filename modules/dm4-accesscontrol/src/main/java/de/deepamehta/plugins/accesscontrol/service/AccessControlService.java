package de.deepamehta.plugins.accesscontrol.service;

import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface AccessControlService extends PluginService {

    /**
     * Checks weather the credentials in the authorization string match an existing User Account,
     * and if so, creates an HTTP session. ### FIXDOC
     *
     * @param   authHeader  the authorization string containing the credentials. ### FIXDOC
     *                      Formatted like a "Authorization" HTTP header value. That is, "Basic " appended by the
     *                      Base64 encoded form of "{username}:{password}".
     *
     * @return  The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    Topic login();

    /**
     * @return  A <code>true</code> value instructs the webclient to shutdown. That is, its GUI must no longer be
     *          presented to the user.
     *          This is used for "private" DM installations. The webclient of a "private" installation is only
     *          accessible when logged in. A DM installation is made "private" by setting the config property
     *          <code>dm4.security.read_requires_login</code> to <code>true</code> (in global <code>pom.xml</code>).
     */
    boolean logout();

    // ---

    /**
     * Returns the username of the logged in user.
     *
     * @return  the username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no user is logged in.
     */
    Topic getUsername();

    // ---

    Permissions getTopicPermissions(long topicId);

    // ---

    Topic getOwnedTopic(long userId, String typeUri);

    void setOwner(long topicId, long userId);

    // ---

    void createACLEntry(long topicId, Role role, Permissions permissions);
    void createACLEntry(Topic topic,  Role role, Permissions permissions);

    // ---

    void joinWorkspace(long workspaceId, long userId);
}
