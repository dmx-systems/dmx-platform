package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
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

import java.util.logging.Logger;



class AccessControlImpl implements AccessControl {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Type URIs
    // ### TODO: move to dm4.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String TYPE_MEMBERSHIP = "dm4.accesscontrol.membership";
    private static final String TYPE_USERNAME   = "dm4.accesscontrol.username";

    // Property URIs
    // ### TODO: move to dm4.core namespace?
    // ### TODO: copy in AccessControlPlugin.java
    private static final String PROP_OWNER = "dm4.accesscontrol.owner";
    // ### TODO: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // ### TODO: copy in AccessControlPlugin.java
    private static final String SYSTEM_WORKSPACE_URI = "dm4.workspaces.system";
    private long systemWorkspaceId = -1;    // initialized lazily

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AccessControlImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean checkCredentials(Credentials cred) {
        TopicModel usernameTopic = null;
        try {
            usernameTopic = getUsernameTopic(cred.username);
            if (usernameTopic == null) {
                return false;
            }
            return matches(usernameTopic, cred.password);
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
            long workspaceId;
            if (typeUri.equals("dm4.workspaces.workspace")) {
                workspaceId = objectId;
            } else {
                workspaceId = getAssignedWorkspaceId(objectId);
                //
                if (workspaceId == -1) {
                    switch (operation) {
                    case READ:
                        logger.warning("object " + objectId + " (typeUri=\"" + typeUri +
                            "\") is not assigned to any workspace -- READ permission is granted");
                        return true;
                    case WRITE:
                        logger.warning("object " + objectId + " (typeUri=\"" + typeUri +
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
        // 1) create assignment association
        dms.associationFactory(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(object.getId(), "dm4.core.parent"),
            new TopicRoleModel(workspaceId, "dm4.core.child")
        ));
        // 2) store assignment property
        object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     *
     * @param   password    The encoded password.
     */
    private boolean matches(TopicModel usernameTopic, String password) {
        return getPassword(getUserAccount(usernameTopic)).equals(password);
    }

    /**
     * Prerequisite: usernameTopic is not <code>null</code>.
     */
    private TopicModel getUserAccount(TopicModel usernameTopic) {
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

    private TopicModel getUsernameTopic(String username) {
        // Note: username topics are not readable by <anonymous>.
        // So direct storage access is required here.
        return dms.storageDecorator.fetchTopic(TYPE_USERNAME, new SimpleValue(username));
    }

    private TopicModel getUsernameTopicOrThrow(String username) {
        TopicModel usernameTopic = getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }

    // ---

    long getSystemWorkspaceId() {
        if (systemWorkspaceId != -1) {
            return systemWorkspaceId;
        }
        // Note: fetching the System workspace topic though the Core service would involve a permission check
        // and run in a vicious circle. So direct storage access is required here.
        TopicModel workspace = dms.storageDecorator.fetchTopic("uri", new SimpleValue(SYSTEM_WORKSPACE_URI));
        // Note: the Access Control plugin creates the System workspace before it performs its first permission check.
        if (workspace == null) {
            throw new RuntimeException("The System workspace does not exist");
        }
        //
        systemWorkspaceId = workspace.getId();
        //
        return systemWorkspaceId;
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
