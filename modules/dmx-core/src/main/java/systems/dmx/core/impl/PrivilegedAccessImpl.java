package systems.dmx.core.impl;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



class PrivilegedAccessImpl implements PrivilegedAccess {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Type URIs
    // ### TODO: move to dmx.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String TYPE_MEMBERSHIP = "dmx.accesscontrol.membership";
    private static final String TYPE_USERNAME   = "dmx.accesscontrol.username";
    // ### TODO: copy in TopicmapsPlugin.java
    private static final String TOPICMAP_CONTEXT = "dmx.topicmaps.topicmap_context";
    //
    private static final String TYPE_EMAIL_ADDRESS = "dmx.contacts.email_address";
    // ### TODO: copy in ConfigPlugin.java
    private static final String ASSOC_TYPE_USER_MAILBOX = "org.deepamehta.signup.user_mailbox";
    private static final String ASSOC_TYPE_CONFIGURATION = "dmx.config.configuration";
    private static final String ROLE_TYPE_CONFIGURABLE   = "dmx.config.configurable";
    private static final String ROLE_TYPE_DEFAULT = "dmx.core.default";

    // Property URIs
    // ### TODO: move to dmx.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_CREATOR  = "dmx.accesscontrol.creator";
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_OWNER = "dmx.accesscontrol.owner";
    // ### TODO: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dmx.workspaces.workspace_id";

    // Workspace URIs
    // ### TODO: copy in WorkspaceService.java
    private static final String DMX_WORKSPACE_URI = "dmx.workspaces.dmx";
    // ### TODO: copy in AccessControlService.java
    private static final String ADMINISTRATION_WORKSPACE_URI = "dmx.workspaces.administration";
    private static final String SYSTEM_WORKSPACE_URI = "dmx.workspaces.system";

    private long systemWorkspaceId = -1;    // initialized lazily

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // used for workspace assignment suppression
    private ContextTracker contextTracker = new ContextTracker();

    private AccessLayer al;
    private ModelFactoryImpl mf;

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
            if (typeUri.equals("dmx.workspaces.workspace")) {
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
            throw new RuntimeException("Checking permission for object " + objectId + " failed, typeUri=\"" + typeUri +
                "\", " + userInfo(username) + ", operation=" + operation, e);
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
            TopicModelImpl userAccount = _getUserAccount(_getUsernameTopicOrThrow(cred.username));
            userAccount.update(mf.newTopicModel(mf.newChildTopicsModel()
                .put("dmx.accesscontrol.password", cred.password)
            ));
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
            for (TopicModelImpl workspace : fetchTopicsByOwner(username, "dmx.workspaces.workspace")) {
                if (getSharingMode(workspace.getId()) == SharingMode.PRIVATE) {
                    return workspace.instantiate();
                }
            }
            throw new RuntimeException("User \"" + username + "\" has no private workspace");
        } catch (Exception e) {
            throw new RuntimeException("Private workspace of user \"" + username + "\" can't be determined", e);
        }
    }

    @Override
    public boolean isMember(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            // Note: direct storage access is required here
            AssocModel membership = al.sd.fetchAssoc(TYPE_MEMBERSHIP, _getUsernameTopicOrThrow(username).getId(),
                workspaceId, "dmx.core.default", "dmx.core.default");
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
        TopicModelImpl workspace = al.db.fetchTopic("uri", uri);
        if (workspace == null) {
            throw new RuntimeException("Workspace \"" + uri + "\" does not exist");
        }
        return workspace.instantiate();
    }

    // ---

    @Override
    public long getDMXWorkspaceId() {
        return getWorkspace(DMX_WORKSPACE_URI).getId();
    }

    @Override
    public long getAdministrationWorkspaceId() {
        return getWorkspace(ADMINISTRATION_WORKSPACE_URI).getId();
    }

    @Override
    public long getSystemWorkspaceId() {
        if (systemWorkspaceId == -1) {
            // Note: fetching the System workspace topic though the Core service would involve a permission check
            // and run in a vicious circle. So direct storage access is required here.
            TopicModel workspace = al.db.fetchTopic("uri", SYSTEM_WORKSPACE_URI);
            // Note: the Access Control plugin creates the System workspace before it performs its first permission
            // check.
            if (workspace == null) {
                throw new RuntimeException("The System workspace does not exist");
            }
            //
            systemWorkspaceId = workspace.getId();
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
            throw new RuntimeException("Workspace assignment of object " + objectId + " can't be determined", e);
        }
    }

    @Override
    public void assignToWorkspace(DMXObject object, long workspaceId) {
        try {
            long _workspaceId = getAssignedWorkspaceId(object.getId());
            if (_workspaceId == -1) {
                // create assignment association
                al.createAssoc("dmx.workspaces.workspace_assignment",
                    object.getModel().createPlayerModel("dmx.core.parent"),
                    mf.newTopicPlayerModel(workspaceId, "dmx.core.child")
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
    public <V> V runWithoutWorkspaceAssignment(Callable<V> callable) throws Exception {
        return contextTracker.run(callable);
    }

    @Override
    public boolean workspaceAssignmentIsSuppressed() {
        return contextTracker.runsInTrackedContext();
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
            RelatedTopicModelImpl configTopic = al.sd.fetchTopicRelatedTopic(topicId, ASSOC_TYPE_CONFIGURATION,
                ROLE_TYPE_CONFIGURABLE, ROLE_TYPE_DEFAULT, configTypeUri);
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
     * @param   password    The encoded password.
     */
    private boolean matches(TopicModel usernameTopic, String password) {
        String _password = getPasswordTopic(usernameTopic).getSimpleValue().toString();  // encoded
        return _password.equals(password);
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModel getPasswordTopic(TopicModel usernameTopic) {
        return _getPasswordTopic(_getUserAccount(usernameTopic));
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModelImpl _getUserAccount(TopicModel usernameTopic) {
        // Note: checking the credentials is performed by <anonymous> and User Accounts are private.
        // So direct storage access is required here.
        RelatedTopicModelImpl userAccount = al.sd.fetchTopicRelatedTopic(usernameTopic.getId(), "dmx.core.composition",
            "dmx.core.child", "dmx.core.parent", "dmx.accesscontrol.user_account");
        if (userAccount == null) {
            throw new RuntimeException("Data inconsistency: there is no User Account topic for username \"" +
                usernameTopic.getSimpleValue() + "\", usernameTopic=" + usernameTopic);
        }
        return userAccount;
    }

    /**
     * Prerequisite: userAccount is not <code>null</code>.
     */
    private TopicModel _getPasswordTopic(TopicModel userAccount) {
        // Note: we only have a (User Account) topic model at hand and we don't want instantiate a Topic.
        // So we use direct storage access here.
        RelatedTopicModel password = al.sd.fetchTopicRelatedTopic(userAccount.getId(), "dmx.core.composition",
            "dmx.core.parent", "dmx.core.child", "dmx.accesscontrol.password");
        if (password == null) {
            throw new RuntimeException("Data inconsistency: there is no Password topic for User Account \"" +
                userAccount.getSimpleValue() + "\", userAccount=" + userAccount);
        }
        return password;
    }

    // ---

    // ### TODO: remove this workaround
    private boolean permissionIfNoWorkspaceIsAssigned(Operation operation, long objectId, String typeUri) {
        switch (operation) {
        case READ:
            logger.fine("Object " + objectId + " (typeUri=\"" + typeUri +
                "\") is not assigned to any workspace -- READ permission is granted");
            return true;
        case WRITE:
            logger.warning("Object " + objectId + " (typeUri=\"" + typeUri +
                "\") is not assigned to any workspace -- WRITE permission is refused");
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
        TopicModel sharingMode = al.sd.fetchTopicRelatedTopic(workspaceId, "dmx.core.composition", "dmx.core.parent",
            "dmx.core.child", "dmx.workspaces.sharing_mode");
        if (sharingMode == null) {
            throw new RuntimeException("No sharing mode is assigned to workspace " + workspaceId);
        }
        return SharingMode.fromString(sharingMode.getUri());
    }

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = getTypeUri(workspaceId);
        if (!typeUri.equals("dmx.workspaces.workspace")) {
            throw new RuntimeException("Object " + workspaceId + " is not a workspace, but a \"" + typeUri + "\"");
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
        return al.db.fetchTopic(TYPE_USERNAME, username);
    }

    private TopicModelImpl _getUsernameTopicOrThrow(String username) {
        TopicModelImpl usernameTopic = _getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }

    // ---

    private String _getUsername(String emailAddress) {
        String username = null;
        for (TopicModelImpl emailAddressTopic : al.db.fetchTopics(TYPE_EMAIL_ADDRESS, emailAddress)) {
            TopicModel usernameTopic = emailAddressTopic.getRelatedTopic(ASSOC_TYPE_USER_MAILBOX,
                "dmx.core.child", "dmx.core.parent", TYPE_USERNAME);
            if (usernameTopic != null) {
                if (username != null) {
                    throw new RuntimeException("Ambiguity: the Username assignment for email address \"" +
                        emailAddress + "\" is not unique");
                }
                username = usernameTopic.getSimpleValue().toString();
            }
        }
        return username;
    }

    private String _getEmailAddress(String username) {
        TopicModel emailAddress = _getUsernameTopicOrThrow(username).getRelatedTopic(ASSOC_TYPE_USER_MAILBOX,
            "dmx.core.parent", "dmx.core.child", TYPE_EMAIL_ADDRESS);
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



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
