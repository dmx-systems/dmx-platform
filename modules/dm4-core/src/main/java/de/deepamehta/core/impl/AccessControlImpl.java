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
        Topic username = null;
        try {
            username = getUsernameTopic(cred.username);
            if (username == null) {
                return false;
            }
            return matches(username, cred.password);
        } catch (Exception e) {
            throw new RuntimeException("Checking credentials for user \"" + cred.username + "\" failed (username " +
                username + ")", e);
        }
    }

    @Override
    public boolean hasPermission(String username, Operation operation, long objectId) {
        String typeUri = null;
        try {
            typeUri = typeUri(objectId);
            long workspaceId;
            if (typeUri.equals("dm4.workspaces.workspace")) {
                workspaceId = objectId;
            } else {
                workspaceId = assignedWorkspaceId(objectId);
                //
                if (workspaceId == -1) {
                    switch (operation) {
                    case READ:
                        logger.warning("object " + objectId + " (typeUri=\"" + typeUri +
                            "\") has no workspace assignment -- READ permission is granted");
                        return true;
                    case WRITE:
                        logger.warning("object " + objectId + " (typeUri=\"" + typeUri +
                            "\") has no workspace assignment -- WRITE permission is refused");
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
        return _isMember(username, workspaceId);
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
     * Prerequisite: username is not <code>null</code>.
     *
     * @param   password    The encoded password.
     */
    private boolean matches(Topic username, String password) {
        return password(fetchUserAccount(username)).equals(password);
    }

    /**
     * Prerequisite: username is not <code>null</code>.
     */
    private TopicModel fetchUserAccount(Topic username) {
        // Note: checking the credentials is performed by <anonymous> and User Accounts are private.
        // So direct storage access is required here.
        RelatedTopicModel userAccount = dms.storageDecorator.fetchTopicRelatedTopic(username.getId(),
            "dm4.core.composition", "dm4.core.child", "dm4.core.parent", "dm4.accesscontrol.user_account");
        if (userAccount == null) {
            throw new RuntimeException("Data inconsistency: there is no User Account topic for username \"" +
                username.getSimpleValue() + "\" (username=" + username + ")");
        }
        return userAccount;
    }

    /**
     * @return  The encoded password of the specified User Account.
     */
    private String password(TopicModel userAccount) {
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
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    private boolean hasReadPermission(String username, long workspaceId) {
        SharingMode sharingMode = sharingMode(workspaceId);
        switch (sharingMode) {
        case PRIVATE:
            return isOwner(username, workspaceId);
        case CONFIDENTIAL:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case COLLABORATIVE:
            return isOwner(username, workspaceId) || isMember(username, workspaceId);
        case PUBLIC:
            return true;
        case COMMON:
            return true;
        default:
            throw new RuntimeException(sharingMode + " is an unsupported sharing mode");
        }
    }

    /**
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    private boolean hasWritePermission(String username, long workspaceId) {
        SharingMode sharingMode = sharingMode(workspaceId);
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

    private long assignedWorkspaceId(long objectId) {
        // Note: direct storage access is required here
        TopicModel workspace = dms.storageDecorator.fetchRelatedTopic(objectId, "dm4.core.aggregation",
            "dm4.core.parent", "dm4.core.child", "dm4.workspaces.workspace");
        return workspace != null ? workspace.getId() : -1;
    }

    /**
     * Checks if a user is the owner of a workspace.
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     *
     * @return  <code>true</code> of the user is the owner, <code>false</code> otherwise.
     */
    private boolean isOwner(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            return owner(workspaceId).equals(username);
        } catch (Exception e) {
            throw new RuntimeException("Checking ownership of workspace " + workspaceId + " and user \"" +
                username + "\" failed", e);
        }
    }

    /**
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    private boolean _isMember(String username, long workspaceId) {
        try {
            if (username == null) {
                return false;
            }
            // Note: direct storage access is required here
            AssociationModel membership = dms.storageDecorator.fetchAssociation(TYPE_MEMBERSHIP,
                usernameTopic(username).getId(), workspaceId, "dm4.core.default", "dm4.core.default");
            return membership != null;
        } catch (Exception e) {
            throw new RuntimeException("Checking membership of user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    private SharingMode sharingMode(long workspaceId) {
        // Note: direct storage access is required here
        TopicModel sharingMode = dms.storageDecorator.fetchTopicRelatedTopic(workspaceId, "dm4.core.aggregation",
            "dm4.core.parent", "dm4.core.child", "dm4.workspaces.sharing_mode");
        if (sharingMode == null) {
            throw new RuntimeException("No sharing mode is assigned to workspace " + workspaceId);
        }
        return SharingMode.fromString(sharingMode.getUri());
    }

    // ---

    private String owner(long workspaceId) {
        // Note: direct storage access is required here
        if (!dms.storageDecorator.hasProperty(workspaceId, PROP_OWNER)) {
            throw new RuntimeException("No owner is assigned to workspace " + workspaceId);
        }
        return (String) dms.storageDecorator.fetchProperty(workspaceId, PROP_OWNER);
    }

    private String typeUri(long objectId) {
        // Note: direct storage access is required here
        return (String) dms.storageDecorator.fetchProperty(objectId, "type_uri");
    }

    // ---

    // ### TODO: copy in AccessControlService
    private Topic getUsernameTopic(String username) {
        return dms.getTopic("dm4.accesscontrol.username", new SimpleValue(username));
    }

    private TopicModel usernameTopic(String username) {
        // ### TODO: direct storage access is not required anymore
        TopicModel usernameTopic = dms.storageDecorator.fetchTopic(TYPE_USERNAME, new SimpleValue(username));
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
