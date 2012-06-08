package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Permission;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.facets.service.FacetsService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.util.JavaUtils;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends Plugin implements AccessControlService {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "";
    private static final String ENCRYPTED_PASSWORD_PREFIX = "-SHA256-";  // don't change this

    private static final String OPERATION_WRITE_URI = "dm4.accesscontrol.operation_write";
    private static final String OPERATION_CREATE_URI = "dm4.accesscontrol.operation_create";

    // association type semantics ### TODO: to be dropped. Model-driven manipulators required.
    private static final String WORKSPACE_MEMBERSHIP = "dm4.accesscontrol.membership";
    private static final String ROLE_TYPE_USER       = "dm4.core.default";
    private static final String ROLE_TYPE_WORKSPACE  = "dm4.core.default";

    private static enum RelationType {
        TOPIC_CREATOR,      // Creator of a topic.    Direction is from topic to user.
        TOPIC_OWNER,        // Owner of a topic.      Direction is from topic to user.
        ACCESS_CONTROL,     // ACL of a topic.        Direction is from topic to role.
        WORKSPACE_MEMBER    // Member of a workspace. Direction is from workspace to user.
    }

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static final Permissions DEFAULT_CREATOR_PERMISSIONS = new Permissions();
    static {
        DEFAULT_CREATOR_PERMISSIONS.add(Permission.WRITE, true);
        DEFAULT_CREATOR_PERMISSIONS.add(Permission.CREATE, true);
    }

    private FacetsService facetsService;
    private WorkspacesService wsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************************************
    // *** AccessControlService Implementation ***
    // *******************************************



    @GET
    @Path("/user/{username}")
    @Override
    public Topic lookupUserAccount(@PathParam("username") String username) {
        logger.info("username=\"" + username + "\"");
        return getUserAccount(username);
    }

    /**
     * Returns the user that is represented by the client state, or <code>null</code> if no user is logged in.
     */
    @GET
    @Path("/user")
    @Override
    public Topic getUserAccount(@HeaderParam("Cookie") ClientState clientState) {
        if (clientState == null) {      // some callers to dms.getTopic() doesn't pass a client state
            return null;
        }
        String username = clientState.get("dm4_username");
        if (username == null) {
            return null;
        }
        return getUserAccount(username);
    }

    // ---

    @GET
    @Path("/owner/{userId}/{typeUri}")
    @Override
    public Topic getOwnedTopic(@PathParam("userId") long userId, @PathParam("typeUri") String typeUri) {
        /* ### TODO: adapt to DM4
        List<RelatedTopic> topics = dms.getRelatedTopics(userId, asList(typeUri),
            asList(RelationType.TOPIC_OWNER.name() + ";OUTGOING"), null);
        //
        if (topics.size() == 0) {
            return null;
        } else if (topics.size() > 1) {
            throw new RuntimeException("Ambiguity: owner " + userId + " has " +
                topics.size() + " " + typeUri + " topics");
        }
        //
        return topics.get(0).getTopic(); */
        return null;
    }

    @POST
    @Path("/topic/{topicId}/owner/{userId}")
    @Override
    public void setOwner(@PathParam("topicId") long topicId, @PathParam("userId") long userId) {
        dms.getTopic(topicId, false, null).setCompositeValue(new CompositeValue().put("dm4.accesscontrol.owner",
            new CompositeValue().put("dm4.accesscontrol.user_account", "ref_id:" + userId)), null, null);
        // ### FIXME: "ref_id:" not expanded by put(). TODO: Add a put_ref method().
    }

    // ---

    @POST
    @Path("/topic/{topicId}/role/{role}")
    @Override
    public void createACLEntry(@PathParam("topicId") long topicId, @PathParam("role") Role role,
                                                                                      Permissions permissions) {
        /* ### TODO: adapt to DM4
        dms.createRelation(RelationType.ACCESS_CONTROL.name(), topicId, getRoleTopic(role).id,
                           new Properties(permissions)); */
    }

    // ---

    @POST
    @Path("/user/{userId}/{workspaceId}")
    @Override
    public void joinWorkspace(@PathParam("workspaceId") long workspaceId, @PathParam("userId") long userId) {
        dms.createAssociation(new AssociationModel(WORKSPACE_MEMBERSHIP,
            new TopicRoleModel(userId, ROLE_TYPE_USER),
            new TopicRoleModel(workspaceId, ROLE_TYPE_WORKSPACE)), null);
    }



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void postInstallPluginHook() {
        Topic user = createUserAccount(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        logger.info("Creating \"admin\" user account => ID=" + user.getId());
    }

    @Override
    public void serviceArrived(PluginService service) {
        logger.info("########## Service arrived: " + service);
        if (service instanceof FacetsService) {
            facetsService = (FacetsService) service;
        } else if (service instanceof WorkspacesService) {
            wsService = (WorkspacesService) service;
        }
    }

    @Override
    public void serviceGone(PluginService service) {
        logger.info("########## Service gone: " + service);
        if (service == facetsService) {
            facetsService = null;
        } else if (service == wsService) {
            wsService = null;
        }
    }

    // Note: we must use the postCreateHook to create the relation because at pre_create the topic has no ID yet.
    @Override
    public void postCreateHook(Topic topic, ClientState clientState, Directives directives) {
        /* check precondition 4
        if (topic.id == user.id) {
            logger.warning(topic + " can't be related to user \"" + username + "\" (the topic is the user itself!)");
            return;
        }*/
        //
        setCreator(topic, clientState);
        createACLEntry(topic.getId(), Role.CREATOR, DEFAULT_CREATOR_PERMISSIONS);
    }

    /* ### TODO: adapt to DM4
    @Override
    public void preUpdateHook(Topic topic, Properties newProperties) {
        // encrypt password of new users
        if (topic.typeUri.equals("de/deepamehta/core/topictype/user")) {
            // we recognize a new user (or changed password) if password doesn't begin with ENCRYPTED_PASSWORD_PREFIX
            String password = newProperties.get("de/deepamehta/core/property/password").toString();
            if (!password.startsWith(ENCRYPTED_PASSWORD_PREFIX)) {
                newProperties.put("de/deepamehta/core/property/password", encryptPassword(password));
            }
        }
    } */

    /* ### TODO: adapt to DM4
    @Override
    public void modifyTopicTypeHook(TopicType topicType, ClientState clientState) {
        addCreatorFieldToType(topicType);
        addOwnerFieldToType(topicType);
        //
        setCreator(topicType, clientState);
        createACLEntry(topicType.id, Role.CREATOR, DEFAULT_CREATOR_PERMISSIONS);
    } */

    // ---

    /* ### TODO: adapt to DM4
    @Override
    public void providePropertiesHook(Topic topic) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/role")) {
            String roleName = dms.getTopicProperty(topic.id, "de/deepamehta/core/property/rolename").toString();
            topic.setProperty("de/deepamehta/core/property/rolename", roleName);
        }
    } */

    /* ### TODO: adapt to DM4
    @Override
    public void providePropertiesHook(Relation relation) {
        if (relation.typeId.equals(RelationType.ACCESS_CONTROL.name())) {
            // transfer all relation properties
            Properties properties = dms.getRelation(relation.id).getProperties();
            relation.setProperties(properties);
        }
    } */

    // ---

    @Override
    public void postFetchTopicHook(Topic topic, ClientState clientState, Directives directives) {
        logger.info("### Enriching topic " + topic.getId() + " with its permissions");
        Topic user = getUserAccount(clientState);
        enrichWithPermissions(topic,
            hasPermission(user, OPERATION_WRITE_URI, topic));
    }

    @Override
    public void postFetchTopicTypeHook(TopicType topicType, ClientState clientState, Directives directives) {
        String typeUri = topicType.getUri();
        // Note: there are 2 types whose permissions must be set manually as they can't be calculated the usual way:
        // - "Access Control List Facet": endless recursion would occur.
        // - "Meta Meta Type": doesn't exist in DB. Retrieving its ACL would fail.
        if (typeUri.equals("dm4.accesscontrol.acl_facet") || typeUri.equals("dm4.core.meta_meta_type")) {
            enrichWithPermissions(topicType, false, false);     // write=false, create=false
            return;
        }
        //
        logger.info("### Enriching topic type \"" + typeUri + "\" with its permissions");
        Topic user = getUserAccount(clientState);
        enrichWithPermissions(topicType,
            hasPermission(user, OPERATION_WRITE_URI, topicType),
            hasPermission(user, OPERATION_CREATE_URI, topicType));
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount(String username, String password) {
        return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new CompositeValue()
            .put("dm4.accesscontrol.username", username)
            .put("dm4.accesscontrol.password", encryptPassword(password))), null);  // clientState=null
    }

    // ---

    /**
     * Returns a user account by username, or <code>null</code> if no such user account exists.
     *
     * @return  a Topic of type <code>dm4.accesscontrol.user_account</code>.
     */
    private Topic getUserAccount(String username) {
        Topic userName = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(username), false, null);
        if (userName == null) {
            return null;
        }
        return userName.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole",
            "dm4.accesscontrol.user_account", true, false, null);  // fetchComposite=true, fetchRelatingComposite=false
    }

    private Topic getAdminUser() {
        Topic user = getUserAccount(DEFAULT_USERNAME);
        if (user == null) {
            throw new RuntimeException("The \"" + DEFAULT_USERNAME + "\" user doesn't exist");
        }
        return user;
    }

    // ---

    private String encryptPassword(String password) {
        return ENCRYPTED_PASSWORD_PREFIX + JavaUtils.encodeSHA256(password);
    }

    // ---

    /* ### TODO: adapt to DM4
    private void addCreatorFieldToType(TopicType topicType) {
        DataField creatorField = new DataField("Creator", "reference");
        creatorField.setUri("de/deepamehta/core/property/creator");
        creatorField.setRefTopicTypeUri("de/deepamehta/core/topictype/user");
        creatorField.setRefRelationTypeId(RelationType.TOPIC_CREATOR.name());
        creatorField.setEditor("checkboxes");
        //
        topicType.addDataField(creatorField);
    } */

    /* ### TODO: adapt to DM4
    private void addOwnerFieldToType(TopicType topicType) {
        DataField ownerField = new DataField("Owner", "reference");
        ownerField.setUri("de/deepamehta/core/property/owner");
        ownerField.setRefTopicTypeUri("de/deepamehta/core/topictype/user");
        ownerField.setRefRelationTypeId(RelationType.TOPIC_OWNER.name());
        ownerField.setEditor("checkboxes");
        //
        topicType.addDataField(ownerField);
    } */

    // ---

    private void setCreator(Topic topic, ClientState clientState) {
        Topic user = getUserAccount(clientState);
        if (user == null) {
            logger.warning("Assigning a creator to " + topic + " failed (no user is logged in). " +
                "Assigning user \"admin\" instead.");
            user = getAdminUser();
        }
        setCreator(topic.getId(), user.getId());
    }

    private void setCreator(long topicId, long userId) {
        dms.getTopic(topicId, false, null).setCompositeValue(new CompositeValue().put("dm4.accesscontrol.creator",
            new CompositeValue().put("dm4.accesscontrol.user_account", "ref_id:" + userId)), null, null);
        // ### FIXME: "ref_id:" not expanded by put(). TODO: Add a put_ref method().
    }

    // === ACL Entries ===

    /* ### TODO: adapt to DM4
    private Topic getRoleTopic(Role role) {
        Topic roleTopic = dms.getTopic("de/deepamehta/core/property/rolename", new PropValue(role.s()));
        if (roleTopic == null) {
            throw new RuntimeException("Role topic \"" + role.s() + "\" doesn't exist");
        }
        return roleTopic;
    } */

    // ---

    /**
     * Returns true if the user is allowed to perform an operation on a topic.
     */
    private boolean hasPermission(Topic user, String operationUri, Topic topic) {
        logger.fine("Determining permission of user " + user + " to \"" + operationUri + "\" " + topic);
        for (RelatedTopic aclEntry : getACLEntries(topic)) {
            String roleUri = aclEntry.getCompositeValue().getTopic("dm4.accesscontrol.role").getUri();
            logger.fine("There is an ACL entry for role \"" + roleUri + "\"");
            boolean allowedForRole = getAllowed(aclEntry, operationUri);
            logger.fine("value=" + allowedForRole);
            if (allowedForRole && userOccupiesRole(topic, user, roleUri)) {
                logger.fine("=> ALLOWED");
                return true;
            }
        }
        logger.fine("=> DENIED");
        return false;
    }

    private boolean userOccupiesRole(Topic topic, Topic user, String roleUri) {
        //
        if (roleUri.equals("dm4.accesscontrol.role_everyone")) {
            return true;
        }
        //
        if (user == null) {
            return false;
        }
        //
        if (roleUri.equals("dm4.accesscontrol.role_member")) {
            if (userIsMember(user, topic)) {
                return true;
            }
        } else if (roleUri.equals("dm4.accesscontrol.role_owner")) {
            if (userIsOwner(user, topic)) {
                return true;
            }
        } else if (roleUri.equals("dm4.accesscontrol.role_creator")) {
            if (userIsCreator(user, topic)) {
                return true;
            }
        } else {
            throw new RuntimeException("\"" + roleUri + "\" is an unexpected role URI");
        }
        return false;
    }

    // ---

    /**
     * Returns true if the user is a member of a workspace the topic type is assigned to.
     *
     * FIXME: for the moment only implemented for types, not for regular topics.
     *
     * @param   topic   actually a topic type.
     */
    private boolean userIsMember(Topic user, Topic topic) {
        Set<RelatedTopic> workspaces = wsService.getWorkspaces(topic.getId());
        logger.fine("Topic type \"" + topic.getUri() + "\" is assigned to " + workspaces.size() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            if (isMemberOfWorkspace(user.getId(), workspace.getId())) {
                logger.fine("User " + user + " IS member of workspace " + workspace);
                return true;
            } else {
                logger.fine("User " + user + " is NOT member of workspace " + workspace);
            }
        }
        return false;
    }

    /**
     * Returns true if the user is the owner of the topic.
     */
    private boolean userIsOwner(Topic user, Topic topic) {
        Topic owner = getOwner(topic);
        logger.fine("The owner is " + owner);
        return owner != null && user.getId() == owner.getId();
    }

    /**
     * Returns true if the user is the creator of the topic.
     */
    private boolean userIsCreator(Topic user, Topic topic) {
        Topic creator = getCreator(topic);
        logger.fine("The creator is " + creator);
        return creator != null && user.getId() == creator.getId();
    }

    // ---

    private Set<RelatedTopic> getACLEntries(Topic topic) {
        return facetsService.getFacets(topic, "dm4.accesscontrol.acl_facet");
    }

    private boolean getAllowed(Topic aclEntry, String operationUri) {
        for (TopicModel permission : aclEntry.getCompositeValue().getTopics("dm4.accesscontrol.permission")) {
            if (permission.getCompositeValue().getTopic("dm4.accesscontrol.operation").getUri().equals(operationUri)) {
                return permission.getCompositeValue().getBoolean("dm4.accesscontrol.allowed");
            }
        }
        return false;
    }

    // ---

    /**
     * Returns the creator of a topic, or <code>null</code> if no creator is set.
     *
     * @return  a Topic of type <code>dm4.accesscontrol.user_account</code>.
     */
    private Topic getCreator(Topic topic) {
        Topic creator = facetsService.getFacet(topic, "dm4.accesscontrol.creator_facet");
        if (creator == null) {
            return null;
        }
        return creator.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.user_account", true, false, null);  // fetchComposite=true, fetchRelatingComposite=false
    }

    /**
     * Returns the owner of a topic, or <code>null</code> if no owner is set.
     *
     * @return  a Topic of type <code>dm4.accesscontrol.user_account</code>.
     */
    private Topic getOwner(Topic topic) {
        Topic owner = facetsService.getFacet(topic, "dm4.accesscontrol.owner_facet");
        if (owner == null) {
            return null;
        }
        return owner.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.user_account", true, false, null);  // fetchComposite=true, fetchRelatingComposite=false
    }

    // ---

    private boolean isMemberOfWorkspace(long userId, long workspaceId) {
        return dms.getAssociation(WORKSPACE_MEMBERSHIP, userId, workspaceId,
            ROLE_TYPE_USER, ROLE_TYPE_WORKSPACE) != null;
    }

    // ---

    private void enrichWithPermissions(Topic topic, boolean write) {
        // Note: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are transient data, not stored in DB, recalculated for each request.
        topic.getCompositeValue().put("dm4.accesscontrol.permissions", permissions(write));
    }

    private void enrichWithPermissions(TopicType topicType, boolean write, boolean create) {
        // Note: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are transient data, not stored in DB, recalculated for each request.
        topicType.getCompositeValue().put("dm4.accesscontrol.permissions", permissions(write, create));
    }

    // ---

    private CompositeValue permissions(boolean write) {
        CompositeValue permissions = new CompositeValue();
        permissions.put(OPERATION_WRITE_URI, write);
        return permissions;
    }

    private CompositeValue permissions(boolean write, boolean create) {
        CompositeValue permissions = new CompositeValue();
        permissions.put(OPERATION_WRITE_URI, write);
        permissions.put(OPERATION_CREATE_URI, create);
        return permissions;
    }
}
