package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ModelFactory;
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
    //
    private static final String TYPE_EMAIL_ADDRESS = "dm4.contacts.email_address";
    // ### TODO: copy in ConfigPlugin.java
    private static final String ASSOC_TYPE_CONFIGURATION = "dm4.config.configuration";
    private static final String ROLE_TYPE_CONFIGURABLE   = "dm4.config.configurable";
    private static final String ROLE_TYPE_DEFAULT = "dm4.core.default";

    // Property URIs
    // ### TODO: move to dm4.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_CREATOR  = "dm4.accesscontrol.creator";
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_OWNER = "dm4.accesscontrol.owner";
    // ### TODO: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // Workspace URIs
    // ### TODO: copy in WorkspaceService.java
    private static final String DEEPAMEHTA_WORKSPACE_URI = "dm4.workspaces.deepamehta";
    // ### TODO: copy in AccessControlService.java
    private static final String ADMINISTRATION_WORKSPACE_URI = "dm4.workspaces.administration";
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
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AccessControlImpl(EmbeddedService dms) {
        this.dms = dms;
        this.mf = dms.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Topic checkCredentials(Credentials cred) {
        TopicModel usernameTopic = null;
        try {
            usernameTopic = _getUsernameTopic(cred.username);
            if (usernameTopic == null) {
                return null;
            }
            if (!matches(usernameTopic, cred.password)) {
                return null;
            }
            return instantiate(usernameTopic);
        } catch (Exception e) {
            throw new RuntimeException("Checking credentials for user \"" + cred.username +
                "\" failed (usernameTopic=" + usernameTopic + ")", e);
        }
    }

    @Override
    public boolean hasPermission(String username, Operation operation, long objectId) {
        String typeUri = null;
        try {
            typeUri = getTypeUri(objectId);
            //
            // Note: private topicmaps are treated special. The topicmap's workspace assignment doesn't matter here.
            // Also "operation" doesn't matter as READ/WRITE access is always granted/denied together.
            if (typeUri.equals("dm4.topicmaps.topicmap") && isTopicmapPrivate(objectId)) {
                return isCreator(username, objectId);
            }
            //
            long workspaceId;
            if (typeUri.equals("dm4.workspaces.workspace")) {
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
            throw new RuntimeException("Checking permission for object " + objectId + " failed (typeUri=\"" + typeUri +
                "\", " + userInfo(username) + ", operation=" + operation + ")", e);
        }
    }

    @Override
    public String getCreator(long objectId) {
        return dms.hasProperty(objectId, PROP_CREATOR) ? (String) dms.getProperty(objectId, PROP_CREATOR) : null;
    }



    // === Workspaces / Memberships ===

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
    public long getAdministrationWorkspaceId() {
        return getWorkspace(ADMINISTRATION_WORKSPACE_URI).getId();
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
    public long getAssignedWorkspaceId(long objectId) {
        long workspaceId = -1;
        try {
            if (dms.hasProperty(objectId, PROP_WORKSPACE_ID)) {
                workspaceId = (Long) dms.getProperty(objectId, PROP_WORKSPACE_ID);
                checkWorkspaceId(workspaceId);
            }
            return workspaceId;
        } catch (Exception e) {
            throw new RuntimeException("Object " + objectId + " is assigned to workspace " + workspaceId, e);
        }
    }

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        try {
            // 1) create assignment association
            dms.associationFactory(mf.newAssociationModel("dm4.core.aggregation",
                mf.createRoleModel(object.getModel(), "dm4.core.parent"),
                mf.newTopicRoleModel(workspaceId, "dm4.core.child")
            ));
            // 2) store assignment property
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, true);   // addToIndex=true
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



    // === User Accounts ===

    @Override
    public Topic getUsernameTopic(String username) {
        TopicModel usernameTopic = _getUsernameTopic(username);
        return usernameTopic != null ? instantiate(usernameTopic) : null;
    }

    @Override
    public Topic getUsernameTopic(HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) {
            return null;
        }
        return instantiate(_getUsernameTopicOrThrow(username));
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

    // ---

    @Override
    public Topic getPrivateWorkspace(String username) {
        TopicModel passwordTopic = getPasswordTopic(_getUsernameTopicOrThrow(username));
        long workspaceId = getAssignedWorkspaceId(passwordTopic.getId());
        if (workspaceId == -1) {
            throw new RuntimeException("User \"" + username + "\" has no private workspace");
        }
        return instantiate(dms.storageDecorator.fetchTopic(workspaceId));
    }

    @Override
    public boolean isMember(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            // Note: direct storage access is required here
            AssociationModel membership = dms.storageDecorator.fetchAssociation(TYPE_MEMBERSHIP,
                _getUsernameTopicOrThrow(username).getId(), workspaceId, "dm4.core.default", "dm4.core.default");
            return membership != null;
        } catch (Exception e) {
            throw new RuntimeException("Checking membership of user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }



    // === Config Service ===

    @Override
    public RelatedTopic getConfigTopic(String configTypeUri, long topicId) {
        try {
            RelatedTopicModel configTopic = dms.storageDecorator.fetchTopicRelatedTopic(topicId,
                ASSOC_TYPE_CONFIGURATION, ROLE_TYPE_CONFIGURABLE, ROLE_TYPE_DEFAULT, configTypeUri);
            if (configTopic == null) {
                throw new RuntimeException("The \"" + configTypeUri + "\" configuration topic for topic " + topicId +
                    " is missing");
            }
            return new AttachedRelatedTopic(configTopic, dms);
        } catch (Exception e) {
            throw new RuntimeException("Getting the \"" + configTypeUri + "\" configuration topic for topic " +
                topicId + " failed", e);
        }
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
    private TopicModel _getUserAccount(TopicModel usernameTopic) {
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
     * Prerequisite: userAccount is not <code>null</code>.
     */
    private TopicModel _getPasswordTopic(TopicModel userAccount) {
        // Note: we only have a (User Account) topic model at hand and we don't want instantiate a Topic.
        // So we use direct storage access here.
        RelatedTopicModel password = dms.storageDecorator.fetchTopicRelatedTopic(userAccount.getId(),
            "dm4.core.composition", "dm4.core.parent", "dm4.core.child", "dm4.accesscontrol.password");
        if (password == null) {
            throw new RuntimeException("Data inconsistency: there is no Password topic for User Account \"" +
                userAccount.getSimpleValue() + "\" (userAccount=" + userAccount + ")");
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

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = getTypeUri(workspaceId);
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new RuntimeException("Object " + workspaceId + " is not a workspace (but of type \"" + typeUri +
                "\")");
        }
    }

    // ---

    private boolean isTopicmapPrivate(long topicmapId) {
        TopicModel privateFlag = dms.storageDecorator.fetchTopicRelatedTopic(topicmapId, "dm4.core.composition",
            "dm4.core.parent", "dm4.core.child", "dm4.topicmaps.private");
        if (privateFlag == null) {
            // Note: migrated topicmaps might not have a Private child topic ### TODO: throw exception?
            return false;   // treat as non-private
        }
        return privateFlag.getSimpleValue().booleanValue();
    }

    private boolean isCreator(String username, long objectId) {
        return username != null ? username.equals(getCreator(objectId)) : false;
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

    private TopicModel _getUsernameTopicOrThrow(String username) {
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

    // ---

    /**
     * Instantiates a topic without performing permission check.
     */
    private Topic instantiate(TopicModel model) {
        return new AttachedTopic(model, dms);
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
