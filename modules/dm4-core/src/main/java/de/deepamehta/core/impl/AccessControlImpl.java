package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.service.accesscontrol.SharingMode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



class AccessControlImpl implements AccessControl {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Type URIs
    // ### TODO: move to dm4.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String TYPE_MEMBERSHIP    = "dm4.accesscontrol.membership";
    private static final String TYPE_USERNAME      = "dm4.accesscontrol.username";
    private static String TOPIC_TYPE_LOGIN_ENABLED = "dm4.accesscontrol.login_enabled";
    //
    private static final String TYPE_EMAIL_ADDRESS = "dm4.contacts.email_address";
    // ### TODO: copy in ConfigPlugin.java
    private static String ASSOC_TYPE_CONFIGURATION = "dm4.config.configuration";
    private static String ROLE_TYPE_CONFIGURABLE   = "dm4.config.configurable";
    private static String ROLE_TYPE_DEFAULT = "dm4.core.default";

    // Property URIs
    // ### TODO: move to dm4.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_OWNER = "dm4.accesscontrol.owner";
    // ### TODO: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // Workspace URIs
    // ### TODO: copy in WorkspaceService.java
    private static final String DEEPAMEHTA_WORKSPACE_URI = "dm4.workspaces.deepamehta";
    // ### TODO: copy in AccessControlPlugin.java
    private static final String SYSTEM_WORKSPACE_URI = "dm4.workspaces.system";

    private long systemWorkspaceId = -1;    // initialized lazily

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // standard workspace assignment suppression
    private ThreadLocal<Integer> suppressionLevel = new ThreadLocal() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AccessControlImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Topic checkCredentials(Credentials cred) {
        Topic usernameTopic = null;
        try {
            usernameTopic = getUsernameTopic(cred.username);
            if (usernameTopic == null) {
                return null;
            }
            return matches(usernameTopic, cred.password) ? usernameTopic : null;
        } catch (Exception e) {
            throw new RuntimeException("Checking credentials for user \"" + cred.username +
                "\" failed (usernameTopic=" + usernameTopic + ")", e);
        }
    }

    @Override
    public boolean getLoginEnabled(Topic usernameTopic) {
        // Note: "Login enabled" is checked by <anonymous> and this config topic belongs to System.
        // So direct storage access is required here.
        RelatedTopicModel loginEnabled = dms.storageDecorator.fetchTopicRelatedTopic(usernameTopic.getId(),
            ASSOC_TYPE_CONFIGURATION, ROLE_TYPE_CONFIGURABLE, ROLE_TYPE_DEFAULT, TOPIC_TYPE_LOGIN_ENABLED);
        if (loginEnabled == null) {
            throw new RuntimeException("The \"Login enabled\" configuration topic of user \"" +
                usernameTopic.getSimpleValue() + "\" is missing");
        }
        return loginEnabled.getSimpleValue().booleanValue();
    }

    @Override
    public boolean hasPermission(String username, Operation operation, long objectId) {
        String typeUri = null;
        try {
            typeUri = getTypeUri(objectId);
            long workspaceId;
            if (typeUri.equals("dm4.workspaces.workspace")) {
                workspaceId = objectId;
            } else {
                workspaceId = getAssignedWorkspaceId(objectId);
                //
                if (workspaceId == -1) {
                    switch (operation) {
                    case READ:
                        // ### TODO: remove this workaround
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
            }
            //
            return _hasPermission(username, operation, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Checking permission for object " + objectId + " (typeUri=\"" + typeUri +
                "\") failed (" + userInfo(username) + ", operation=" + operation + ")", e);
        }
    }



    // === Workspaces ===

    @Override
    public Topic getWorkspace(String uri) {
        Topic workspace = dms.getTopic("uri", new SimpleValue(uri));
        if (workspace == null) {
            throw new RuntimeException("Workspace \"" + uri + "\" does not exist");
        }
        return workspace;
    }

    // ---

    @Override
    public long getDeepaMehtaWorkspaceId() {
        return getWorkspace(DEEPAMEHTA_WORKSPACE_URI).getId();
    }

    @Override
    public long getSystemWorkspaceId() {
        if (systemWorkspaceId == -1) {
            // Note: fetching the System workspace topic though the Core service would involve a permission check
            // and run in a vicious circle. So direct storage access is required here.
            TopicModel workspace = fetchTopic("uri", SYSTEM_WORKSPACE_URI);
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
    public boolean isMember(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            // Note: direct storage access is required here
            AssociationModel membership = dms.storageDecorator.fetchAssociation(TYPE_MEMBERSHIP,
                getUsernameTopicOrThrow(username).getId(), workspaceId, "dm4.core.default", "dm4.core.default");
            return membership != null;
        } catch (Exception e) {
            throw new RuntimeException("Checking membership of user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        try {
            // 1) create assignment association
            dms.associationFactory(new AssociationModel("dm4.core.aggregation",
                object.getModel().createRoleModel("dm4.core.parent"),
                new TopicRoleModel(workspaceId, "dm4.core.child")
            ));
            // 2) store assignment property
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + object + " to workspace " + workspaceId + " failed", e);
        }
    }

    // ---

    @Override
    public <V> V runWithoutWorkspaceAssignment(Callable<V> callable) throws Exception {
        int level = suppressionLevel.get();
        try {
            suppressionLevel.set(level + 1);
            return callable.call();     // throws exception
        } finally {
            suppressionLevel.set(level);
        }
    }

    @Override
    public boolean workspaceAssignmentIsSuppressed() {
        return suppressionLevel.get() > 0;
    }



    // === Usernames ===

    @Override
    public Topic getUsernameTopic(String username) {
        TopicModel usernameTopic = _getUsernameTopic(username);
        if (usernameTopic == null) {
            return null;
        }
        // instantiate topic without performing permission check
        return new AttachedTopic(usernameTopic, dms);
    }

    @Override
    public Topic getUsernameTopic(HttpServletRequest request) {
        String username = getUsername(request);
        return username != null ? getUsernameTopic(username) : null;
    }

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
    public String username(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Session data inconsistency: \"username\" attribute is missing");
        }
        return username;
    }



    // === Email Addresses ===

    @Override
    public boolean emailAddressExists(String emailAddress) {
        return !queryTopics(TYPE_EMAIL_ADDRESS, emailAddress).isEmpty();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     *
     * @param   password    The encoded password.
     */
    private boolean matches(Topic usernameTopic, String password) {
        return getPassword(getUserAccount(usernameTopic)).equals(password);
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModel getUserAccount(Topic usernameTopic) {
        // Note: checking the credentials is performed by <anonymous> and User Accounts are private.
        // So direct storage access is required here.
        RelatedTopicModel userAccount = dms.storageDecorator.fetchTopicRelatedTopic(usernameTopic.getId(),
            "dm4.core.composition", "dm4.core.child", "dm4.core.parent", "dm4.accesscontrol.user_account");
        if (userAccount == null) {
            throw new RuntimeException("Data inconsistency: there is no User Account topic for username \"" +
                usernameTopic.getSimpleValue() + "\" (usernameTopic=" + usernameTopic + ")");
        }
        return userAccount;
    }

    /**
     * @return  The encoded password of the specified User Account.
     */
    private String getPassword(TopicModel userAccount) {
        // Note: we only have a (User Account) topic model at hand and we don't want instantiate a Topic.
        // So we use direct storage access here.
        RelatedTopicModel password = dms.storageDecorator.fetchTopicRelatedTopic(userAccount.getId(),
            "dm4.core.composition", "dm4.core.parent", "dm4.core.child", "dm4.accesscontrol.password");
        if (password == null) {
            throw new RuntimeException("Data inconsistency: there is no Password topic for User Account \"" +
                userAccount.getSimpleValue() + "\" (userAccount=" + userAccount + ")");
        }
        return password.getSimpleValue().toString();
    }

    // ---

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
     * @param   username        the logged in user, or <code>null</code> if no user is logged in.
     * @param   workspaceId     the ID of the workspace that is relevant for the permission check. Is never -1.
     */
    private boolean hasReadPermission(String username, long workspaceId) {
        SharingMode sharingMode = getSharingMode(workspaceId);
        switch (sharingMode) {
        case PRIVATE:
            return isOwner(username, workspaceId);
        case CONFIDENTIAL:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case COLLABORATIVE:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case PUBLIC:
            // Note: the System workspace is special: although it is a public workspace
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
    private boolean hasWritePermission(String username, long workspaceId) {
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

    // ---

    // ### TODO: copy in WorkspacesPlugin.java
    private long getAssignedWorkspaceId(long objectId) {
        return dms.hasProperty(objectId, PROP_WORKSPACE_ID) ? (Long) dms.getProperty(objectId, PROP_WORKSPACE_ID) : -1;
    }

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
        TopicModel sharingMode = dms.storageDecorator.fetchTopicRelatedTopic(workspaceId, "dm4.core.aggregation",
            "dm4.core.parent", "dm4.core.child", "dm4.workspaces.sharing_mode");
        if (sharingMode == null) {
            throw new RuntimeException("No sharing mode is assigned to workspace " + workspaceId);
        }
        return SharingMode.fromString(sharingMode.getUri());
    }

    // ---

    private String getOwner(long workspaceId) {
        // Note: direct storage access is required here
        if (!dms.storageDecorator.hasProperty(workspaceId, PROP_OWNER)) {
            throw new RuntimeException("No owner is assigned to workspace " + workspaceId);
        }
        return (String) dms.storageDecorator.fetchProperty(workspaceId, PROP_OWNER);
    }

    private String getTypeUri(long objectId) {
        // Note: direct storage access is required here
        return (String) dms.storageDecorator.fetchProperty(objectId, "type_uri");
    }

    // ---

    private TopicModel _getUsernameTopic(String username) {
        // Note: username topics are not readable by <anonymous>.
        // So direct storage access is required here.
        return fetchTopic(TYPE_USERNAME, username);
    }

    private TopicModel getUsernameTopicOrThrow(String username) {
        TopicModel usernameTopic = _getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }

    // ---

    /**
     * Fetches a topic by key/value via direct storage access.
     * <p>
     * IMPORTANT: only applicable to values indexed with <code>dm4.core.key</code>.
     *
     * @return  the topic, or <code>null</code> if no such topic exists.
     */
    private TopicModel fetchTopic(String key, Object value) {
        return dms.storageDecorator.fetchTopic(key, new SimpleValue(value));
    }

    /**
     * Queries topics by key/value via direct storage access.
     * <p>
     * IMPORTANT: only applicable to values indexed with <code>dm4.core.fulltext</code> or
     * <code>dm4.core.fulltext_key</code>.
     *
     * @return  a list, possibly empty.
     */
    private List<TopicModel> queryTopics(String key, Object value) {
        return dms.storageDecorator.queryTopics(key, new SimpleValue(value));
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
