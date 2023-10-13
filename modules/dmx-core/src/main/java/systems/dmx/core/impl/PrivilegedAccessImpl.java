package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.Operation;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.service.accesscontrol.SharingMode;
import systems.dmx.core.util.ContextTracker;
import systems.dmx.core.util.JavaUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



class PrivilegedAccessImpl implements PrivilegedAccess {

    // ------------------------------------------------------------------------------------------------------- Constants

    // ### TODO: copies in Constants.java of various plugins

    // Topic Types
    private static final String USER_ACCOUNT         = "dmx.accesscontrol.user_account";
    private static final String USERNAME             = "dmx.accesscontrol.username";
    private static final String PASSWORD             = "dmx.accesscontrol.password";
    private static final String WORKSPACE            = "dmx.workspaces.workspace";
    private static final String SHARING_MODE         = "dmx.workspaces.sharing_mode";
    private static final String EMAIL_ADDRESS        = "dmx.contacts.email_address";

    // Assoc Types
    private static final String MEMBERSHIP           = "dmx.accesscontrol.membership";
    private static final String WORKSPACE_ASSIGNMENT = "dmx.workspaces.workspace_assignment";
    private static final String TOPICMAP_CONTEXT     = "dmx.topicmaps.topicmap_context";
    private static final String CONFIGURATION        = "dmx.config.configuration";
    private static final String USER_MAILBOX         = "dmx.base.user_mailbox";

    // Role Types
    private static final String CONFIGURABLE         = "dmx.config.configurable";

    // Property URIs
    private static final String PROP_CREATOR         = "dmx.accesscontrol.creator";
    private static final String PROP_OWNER           = "dmx.accesscontrol.owner";
    private static final String PROP_WORKSPACE_ID    = "dmx.workspaces.workspace_id";

    // Workspace URIs
    private static final String DMX_WORKSPACE_URI    = "dmx.workspaces.dmx";
    private static final String ADMIN_WORKSPACE_URI  = "dmx.workspaces.administration";
    private static final String SYSTEM_WORKSPACE_URI = "dmx.workspaces.system";

    // ---

    private static final String ENCODED_PASSWORD_PREFIX = "-SHA256-";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // used for workspace assignment suppression
    private ContextTracker contextTracker = new ContextTracker();

    private AccessLayer al;
    private ModelFactoryImpl mf;

    private long dmxWorkspaceId    = -1;    // initialized lazily
    private long adminWorkspaceId  = -1;    // initialized lazily
    private long systemWorkspaceId = -1;    // initialized lazily

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    PrivilegedAccessImpl(AccessLayer al) {
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Permissions ===

    @Override
    public boolean hasPermission(String username, Operation operation, long objectId) {
        String typeUri = null;
        try {
            long workspaceId;
            typeUri = getTypeUri(objectId);
            if (typeUri.equals(WORKSPACE)) {
                workspaceId = objectId;
            } else {
                workspaceId = getAssignedWorkspaceId(objectId);
                if (workspaceId == -1) {
                    // fallback when no workspace is assigned
                    return permissionIfNoWorkspaceIsAssigned(operation, objectId, typeUri);
                }
            }
            //
            return _hasPermission(username, operation, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Checking permission for object " + objectId + " failed, typeUri=" + typeUri +
                ", " + userInfo(username) + ", operation=" + operation, e);
        }
    }

    // ---

    /**
     * @param   username        the logged in user, or <code>null</code> if no user is logged in.
     * @param   workspaceId     the ID of the workspace that is relevant for the permission check. Is never -1.
     */
    @Override
    public boolean hasReadPermission(String username, long workspaceId) {
        SharingMode sharingMode = getSharingMode(workspaceId);
        switch (sharingMode) {
        case PRIVATE:
            return isOwner(username, workspaceId);
        case CONFIDENTIAL:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case COLLABORATIVE:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case PUBLIC:
            // Note: the System workspace is treated special: although it is a public workspace
            // its content is readable only for logged in users.
            return workspaceId != getSystemWorkspaceId() || username != null;
        case COMMON:
            return true;
        default:
            throw new RuntimeException(sharingMode + " is an unsupported sharing mode");
        }
    }

    /**
     * @param   username        the logged in user, or <code>null</code> if no user is logged in.
     * @param   workspaceId     the ID of the workspace that is relevant for the permission check. Is never -1.
     */
    @Override
    public boolean hasWritePermission(String username, long workspaceId) {
        SharingMode sharingMode = getSharingMode(workspaceId);
        switch (sharingMode) {
        case PRIVATE:
            return isOwner(username, workspaceId);
        case CONFIDENTIAL:
            return isOwner(username, workspaceId);
        case COLLABORATIVE:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case PUBLIC:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case COMMON:
            return true;
        default:
            throw new RuntimeException(sharingMode + " is an unsupported sharing mode");
        }
    }



    // === User Accounts ===

    @Override
    public Topic checkCredentials(Credentials cred) {
        TopicModelImpl usernameTopic = null;
        try {
            usernameTopic = _getUsernameTopic(cred.username);
            if (usernameTopic == null) {
                return null;
            }
            if (!matches(usernameTopic, cred.password)) {
                return null;
            }
            return usernameTopic.instantiate();
        } catch (Exception e) {
            throw new RuntimeException("Checking credentials for user \"" + cred.username + "\" failed, " +
                "usernameTopic=" + usernameTopic, e);
        }
    }

    @Override
    public void changePassword(Credentials cred) {
        try {
            logger.info("##### Changing password for user \"" + cred.username + "\"");
            TopicModelImpl password = getPasswordTopic(_getUsernameTopicOrThrow(cred.username));
            password._updateSimpleValue(new SimpleValue(encodePassword(cred.password)));
        } catch (Exception e) {
            throw new RuntimeException("Changing password for user \"" + cred.username + "\" failed", e);
        }
    }

    // ---

    @Override
    public Topic getUsernameTopic(String username) {
        TopicModelImpl usernameTopic = _getUsernameTopic(username);
        return usernameTopic != null ? usernameTopic.instantiate() : null;
    }

    @Override
    public Topic getPrivateWorkspace(String username) {
        try {
            for (TopicModelImpl workspace : fetchTopicsByOwner(username, WORKSPACE)) {
                if (getSharingMode(workspace.getId()) == SharingMode.PRIVATE) {
                    return workspace.instantiate();
                }
            }
            throw new RuntimeException("User \"" + username + "\" has no private workspace");
        } catch (Exception e) {
            throw new RuntimeException("Getting private workspace of user \"" + username + "\" failed", e);
        }
    }

    @Override
    public void createMembership(String username, long workspaceId) {
        try {
            // 1) create membership
            Assoc membership = runInWorkspaceContext(-1, () ->
                al.createAssoc(mf.newAssocModel(MEMBERSHIP,
                    mf.newTopicPlayerModel(_getUsernameTopicOrThrow(username).getId(), DEFAULT),
                    mf.newTopicPlayerModel(workspaceId, DEFAULT)
                )).instantiate()
            );
            // 2) assign membership to the involved workspace
            // Note: the current user has not necessarily WRITE access to the involved workspace.
            // Privileged assignToWorkspace() is required (instead of using WorkspacesService).
            // This is to support the "DMX Tendu" 3rd-party plugin.
            assignToWorkspace(membership, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Creating membership for user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public boolean isMember(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            // Note: direct storage access is required here
            AssocModel membership = al.sd.fetchAssoc(MEMBERSHIP, _getUsernameTopicOrThrow(username).getId(),
                workspaceId, DEFAULT, DEFAULT);
            return membership != null;
        } catch (Exception e) {
            throw new RuntimeException("Checking membership of user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public String getCreator(long objectId) {
        return al.db.hasProperty(objectId, PROP_CREATOR) ? (String) al.db.fetchProperty(objectId, PROP_CREATOR) : null;
    }



    // === Session ===

    @Override
    public String getUsername(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);    // create=false
            if (session == null) {
                return null;
            }
            return username(session);
        } catch (IllegalStateException e) {
            // Note: this happens if request is a proxy object (injected by Jersey) and a request method is called
            // outside request scope. This is the case while system startup.
            return null;    // user is unknown
        }
    }

    @Override
    public Topic getUsernameTopic(HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) {
            return null;
        }
        return _getUsernameTopicOrThrow(username).instantiate();
    }

    @Override
    public String username(HttpSession session) {
        return (String) session.getAttribute("username");
    }



    // === Workspaces / Memberships ===

    @Override
    public Topic getWorkspace(String uri) {
        return _getWorkspace(uri).instantiate();
    }

    // ---

    @Override
    public long getDMXWorkspaceId() {
        if (dmxWorkspaceId == -1) {
            dmxWorkspaceId = _getWorkspace(DMX_WORKSPACE_URI).getId();
        }
        return dmxWorkspaceId;
    }

    @Override
    public long getAdminWorkspaceId() {
        if (adminWorkspaceId == -1) {
            adminWorkspaceId = _getWorkspace(ADMIN_WORKSPACE_URI).getId();
        }
        return adminWorkspaceId;
    }

    @Override
    public long getSystemWorkspaceId() {
        if (systemWorkspaceId == -1) {
            systemWorkspaceId = _getWorkspace(SYSTEM_WORKSPACE_URI).getId();
        }
        return systemWorkspaceId;
    }

    // ---

    @Override
    public long getAssignedWorkspaceId(long objectId) {
        try {
            long workspaceId = -1;
            if (al.db.hasProperty(objectId, PROP_WORKSPACE_ID)) {
                workspaceId = (Long) al.db.fetchProperty(objectId, PROP_WORKSPACE_ID);
                checkWorkspaceId(workspaceId);
            }
            return workspaceId;
        } catch (Exception e) {
            throw new RuntimeException("Getting workspace assignment of object " + objectId + " failed", e);
        }
    }

    @Override
    public void assignToWorkspace(DMXObject object, long workspaceId) {
        try {
            long _workspaceId = getAssignedWorkspaceId(object.getId());
            if (_workspaceId == -1) {
                // create assignment association
                al.createAssoc(WORKSPACE_ASSIGNMENT,
                    object.getModel().createPlayerModel(PARENT),
                    mf.newTopicPlayerModel(workspaceId, CHILD)
                );
                // store assignment property
                object.setProperty(PROP_WORKSPACE_ID, workspaceId, true);   // addToIndex=true
            } else if (_workspaceId != workspaceId) {
                throw new RuntimeException("object " + object.getId() + " is already assigned to workspace " +
                    _workspaceId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Assigning object " + object.getId() + " to workspace " + workspaceId +
                " failed, object=" + object, e);
        }
    }

    // ---

    @Override
    public <V> V runInWorkspaceContext(long workspaceId, Callable<V> callable) throws Exception {
        return contextTracker.run(workspaceId, callable);
    }

    @Override
    public Long getWorkspaceContext() {
        return contextTracker.getValue();
    }

    // ---

    @Override
    public void deleteWorkspaceTopic(long workspaceId) {
        TopicModelImpl workspace = al.getTopic(workspaceId);
        String typeUri = workspace.getTypeUri();
        if (!typeUri.equals(WORKSPACE)) {
            throw new RuntimeException("Topic " + workspaceId + " is not a workspace (but a \"" + typeUri + "\")");
        }
        al.deleteTopic(workspace);
    }



    // === Topicmaps ===

    @Override
    public void deleteAssocMapcontext(Assoc assoc) {
        if (!assoc.getTypeUri().equals(TOPICMAP_CONTEXT)) {
            throw new RuntimeException("Assoc " + assoc.getId() + " not eligible for privileged deletion, assoc=" +
                assoc);
        }
        ((AssocImpl) assoc).getModel().delete();
    }



    // === Config Service ===

    @Override
    public RelatedTopic getConfigTopic(String configTypeUri, long topicId) {
        try {
            RelatedTopicModelImpl configTopic = al.sd.fetchTopicRelatedTopic(topicId, CONFIGURATION, CONFIGURABLE,
                DEFAULT, configTypeUri);
            if (configTopic == null) {
                throw new RuntimeException("The \"" + configTypeUri + "\" configuration topic for topic " + topicId +
                    " is missing");
            }
            return configTopic.instantiate();
        } catch (Exception e) {
            throw new RuntimeException("Getting the \"" + configTypeUri + "\" configuration topic for topic " +
                topicId + " failed", e);
        }
    }



    // === Email Addresses ===

    @Override
    public String getUsername(String emailAddress) {
        try {
            String username = _getUsername(emailAddress);
            if (username == null) {
                throw new RuntimeException("No username is assigned to email address \"" + emailAddress + "\"");
            }
            return username;
        } catch (Exception e) {
            throw new RuntimeException("Getting the username for email address \"" + emailAddress + "\" failed", e);
        }
    }

    @Override
    public String getEmailAddress(String username) {
        try {
            String emailAddress = _getEmailAddress(username);
            if (emailAddress == null) {
                throw new RuntimeException("No email address is assigned to username \"" + username + "\"");
            }
            return emailAddress;
        } catch (Exception e) {
            throw new RuntimeException("Getting the email address for username \"" + username + "\" failed", e);
        }
    }

    // ---

    @Override
    public boolean emailAddressExists(String emailAddress) {
        return _getUsername(emailAddress) != null;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     *
     * @param   password    password in plain text
     */
    private boolean matches(TopicModel usernameTopic, String password) {
        String _password = getPasswordTopic(usernameTopic).getSimpleValue().toString();     // SHA256 encoded
        if (!_password.startsWith(ENCODED_PASSWORD_PREFIX)) {
            throw new RuntimeException("Stored password is not SHA256 encoded");
        }
        return _password.equals(encodePassword(password));
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModelImpl getPasswordTopic(TopicModel usernameTopic) {
        return _getPasswordTopic(_getUserAccount(usernameTopic));
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModelImpl _getUserAccount(TopicModel usernameTopic) {
        // Note: checking the credentials is performed by <anonymous> and User Accounts are private.
        // So direct storage access is required here.
        RelatedTopicModelImpl userAccount = al.sd.fetchTopicRelatedTopic(usernameTopic.getId(), COMPOSITION, CHILD,
            PARENT, USER_ACCOUNT);
        if (userAccount == null) {
            throw new RuntimeException("No User Account topic for username \"" + usernameTopic.getSimpleValue() +
                "\", usernameTopic=" + usernameTopic);
        }
        return userAccount;
    }

    /**
     * Prerequisite: userAccount is not <code>null</code>.
     */
    private TopicModelImpl _getPasswordTopic(TopicModel userAccount) {
        // Note: we only have a (User Account) topic model at hand and we don't want instantiate a Topic.
        // So we use direct storage access here.
        RelatedTopicModelImpl password = al.sd.fetchTopicRelatedTopic(userAccount.getId(), COMPOSITION, PARENT,
            CHILD, PASSWORD);
        if (password == null) {
            throw new RuntimeException("No Password topic for User Account \"" + userAccount.getSimpleValue() +
                "\", userAccount=" + userAccount);
        }
        return password;
    }

    // ### TODO: copy in AccessControlPlugin.java
    private String encodePassword(String password) {
        return ENCODED_PASSWORD_PREFIX + JavaUtils.encodeSHA256(password);
    }

    // ---

    private boolean permissionIfNoWorkspaceIsAssigned(Operation operation, long objectId, String typeUri) {
        switch (operation) {
        case READ:
            logger.fine("Object " + objectId + " (typeUri=" + typeUri +
                ") is not assigned to any workspace -- READ permission is granted");
            return true;
        case WRITE:
            logger.warning("Object " + objectId + " (typeUri=" + typeUri +
                ") is not assigned to any workspace -- WRITE permission is refused");
            return false;
        default:
            throw new RuntimeException(operation + " is an unsupported operation");
        }
    }

    private boolean _hasPermission(String username, Operation operation, long workspaceId) {
        switch (operation) {
        case READ:
            return hasReadPermission(username, workspaceId);
        case WRITE:
            return hasWritePermission(username, workspaceId);
        default:
            throw new RuntimeException(operation + " is an unsupported operation");
        }
    }

    // ---

    /**
     * Checks if a user is the owner of a workspace.
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     *
     * @return  <code>true</code> if the user is the owner, <code>false</code> otherwise.
     */
    private boolean isOwner(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            return getOwner(workspaceId).equals(username);
        } catch (Exception e) {
            throw new RuntimeException("Checking ownership of workspace " + workspaceId + " and user \"" +
                username + "\" failed", e);
        }
    }

    private SharingMode getSharingMode(long workspaceId) {
        // Note: direct storage access is required here
        TopicModel sharingMode = al.sd.fetchTopicRelatedTopic(workspaceId, COMPOSITION, PARENT, CHILD, SHARING_MODE);
        if (sharingMode == null) {
            throw new RuntimeException("No sharing mode is assigned to workspace " + workspaceId);
        }
        return SharingMode.fromString(sharingMode.getUri());
    }

    private void checkWorkspaceId(long workspaceId) {
        try {
            String typeUri = getTypeUri(workspaceId);
            if (!typeUri.equals(WORKSPACE)) {
                throw new RuntimeException("Object " + workspaceId + " is not a workspace (but a \"" + typeUri + "\")");
            }
        } catch (Exception e) {
            throw new RuntimeException("Checking workspace ID " + workspaceId + " failed", e);
        }
    }

    // ---

    private String getOwner(long workspaceId) {
        // Note: direct storage access is required here
        if (!al.db.hasProperty(workspaceId, PROP_OWNER)) {
            throw new RuntimeException("No owner is assigned to workspace " + workspaceId);
        }
        return (String) al.db.fetchProperty(workspaceId, PROP_OWNER);
    }

    private String getTypeUri(long objectId) {
        // Note: direct storage access is required here
        return (String) al.db.fetchProperty(objectId, "typeUri");
    }

    // ---

    private TopicModelImpl _getUsernameTopic(String username) {
        // Note: username topics are not readable by <anonymous>.
        // So direct storage access is required here.
        return al.sd.queryTopicFulltext(USERNAME, username);        // username search is case-insensitive
    }

    private TopicModelImpl _getUsernameTopicOrThrow(String username) {
        TopicModelImpl usernameTopic = _getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("Unknown user \"" + username + "\"");
        }
        return usernameTopic;
    }

    // ---

    private String _getUsername(String emailAddress) {
        // email address search is case-insensitive
        TopicModelImpl emailAddressTopic = al.sd.queryTopicFulltext(EMAIL_ADDRESS, emailAddress);
        if (emailAddressTopic != null) {
            TopicModel usernameTopic = emailAddressTopic.getRelatedTopic(USER_MAILBOX, CHILD, PARENT, USERNAME);
            if (usernameTopic != null) {
                return usernameTopic.getSimpleValue().toString();
            }
        }
        return null;
    }

    private String _getEmailAddress(String username) {
        TopicModel emailAddress = _getUsernameTopicOrThrow(username).getRelatedTopic(USER_MAILBOX, PARENT, CHILD,
            EMAIL_ADDRESS);
        return emailAddress != null ? emailAddress.getSimpleValue().toString() : null;
    }



    // === Direct Storage Access ===

    /**
     * Fetches topics by owner, and filter by type.
     *
     * ### TODO: drop "typeUri" parameter. Throw if fetched topic is not a workspace.
     * Note: only for workspace topics the "dmx.accesscontrol.owner" property is set.
     */
    private List<TopicModelImpl> fetchTopicsByOwner(String username, String typeUri) {
        List<TopicModelImpl> topics = new ArrayList();
        for (TopicModelImpl topic : al.db.fetchTopicsByProperty(PROP_OWNER, username)) {
            if (topic.getTypeUri().equals(typeUri)) {
                topics.add(topic);
            }
        }
        return topics;
    }

    /**
     * Fetches a workspace topic by URI.
     */
    private TopicModelImpl _getWorkspace(String uri) {
        // Note: fetching Admin/System workspace topic though the Core service would involve a permission check
        // and run in a vicious circle. So direct storage access is required here.
        TopicModelImpl workspace = al.sd.fetchTopic("uri", uri);
        // Note: the Access Control plugin creates the System workspace before it performs its first permission check.
        if (workspace == null) {
            throw new RuntimeException("Unknown workspace \"" + uri + "\"");
        }
        if (!workspace.getTypeUri().equals(WORKSPACE)) {
            throw new RuntimeException("Topic \"" + uri + "\" is not a workspace (but a \"" + workspace.getTypeUri() +
                "\")");
        }
        //
        return workspace;
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
