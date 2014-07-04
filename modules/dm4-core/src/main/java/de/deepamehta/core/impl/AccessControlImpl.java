package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.service.accesscontrol.WorkspaceType;

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
    private static String PROP_OWNER = "dm4.accesscontrol.owner";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AccessControlImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Checks if a user is permitted to perform an operation on an object (topic or association).
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     * @param   objectId    a topic ID, or an association ID.
     *
     * @return  <code>true</code> if permission is granted, <code>false</code> otherwise.
     */
    @Override
    public boolean hasPermission(String username, Operation operation, long objectId) {
        String typeUri = null;
        try {
            typeUri = typeUri(objectId);
            // ### TODO: remove endless recursion when checking the permission for these types
            if (operation == Operation.READ && (/* typeUri.equals("dm4.topicmaps.topicmap")     || */
                                                typeUri.equals("dm4.core.topic_type")        ||
                                                typeUri.equals("dm4.webclient.view_config"))) {
                                                // typeUri.equals("dm4.accesscontrol.username") ||
                return true;    // ### FIXME
            }
            //
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
                    case CREATE:
                        return true;    // ### TODO
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

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean _hasPermission(String username, Operation operation, long workspaceId) {
        switch (operation) {
        case READ:
            return hasReadPermission(username, workspaceId);
        case WRITE:
            return hasWritePermission(username, workspaceId);
        case CREATE:
            return true;    // ### TODO
        default:
            throw new RuntimeException(operation + " is an unsupported operation");
        }
    }

    // ---

    /**
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    private boolean hasReadPermission(String username, long workspaceId) {
        WorkspaceType workspaceType = workspaceType(workspaceId);
        switch (workspaceType) {
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
            throw new RuntimeException(workspaceType + " is an unsupported workspace type");
        }
    }

    /**
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     */
    private boolean hasWritePermission(String username, long workspaceId) {
        WorkspaceType workspaceType = workspaceType(workspaceId);
        switch (workspaceType) {
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
            throw new RuntimeException(workspaceType + " is an unsupported workspace type");
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

    private WorkspaceType workspaceType(long workspaceId) {
        // Note: direct storage access is required here
        TopicModel workspaceType = dms.storageDecorator.fetchTopicRelatedTopic(workspaceId, "dm4.core.aggregation",
            "dm4.core.parent", "dm4.core.child", "dm4.workspaces.type");
        if (workspaceType == null) {
            throw new RuntimeException("No workspace type is assigned to workspace " + workspaceId);
        }
        return WorkspaceType.fromUri(workspaceType.getUri());
    }

    // ---

    private String owner(long workspaceId) {
        // Note: direct storage access is required here
        if (!dms.storageDecorator.hasProperty(workspaceId, PROP_OWNER)) {
            throw new RuntimeException("No owner is assigned to workspace " + workspaceId);
        }
        return (String) dms.storageDecorator.fetchProperty(workspaceId, PROP_OWNER);
    }

    private TopicModel usernameTopic(String username) {
        TopicModel usernameTopic = dms.storageDecorator.fetchTopic(TYPE_USERNAME, new SimpleValue(username));
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }

    private String typeUri(long objectId) {
        // Note: direct storage access is required here
        return (String) dms.storageDecorator.fetchProperty(objectId, "type_uri");
    }



    // === Logging ===

    // ### TODO: there is a copy in AccessControlPlugin.java
    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }
}
