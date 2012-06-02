package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Permission;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.Role;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.facets.service.FacetsService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
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
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends Plugin implements AccessControlService {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "";
    private static final String ENCRYPTED_PASSWORD_PREFIX = "-SHA256-";  // don't change this

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

    // ---

    @POST
    @Path("/topic/{topicId}/owner/{userId}")
    @Override
    public void setOwner(@PathParam("topicId") long topicId, @PathParam("userId") long userId) {
        dms.getTopic(topicId, false, null).setCompositeValue(new CompositeValue().put("dm4.accesscontrol.owner",
            new CompositeValue().put("dm4.accesscontrol.user_account", "ref_id:" + userId)), null, null);
        // ### FIXME: "ref_id:" not expanded by put(). TODO: Add a put_ref method().
    }

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
        /* ### TODO: adapt to DM4
        dms.createRelation(RelationType.WORKSPACE_MEMBER.name(), workspaceId, userId, null); */    // properties=null
    }



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void postInstallPluginHook() {
        Topic user = createUserAccount(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        logger.info("Creating \"admin\" user => ID=" + user.getId());
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
    public void enrichTopicHook(Topic topic, ClientState clientState) {
        Map permissions = new HashMap();
        permissions.put("write", hasPermission(topic, getUserAccount(clientState), Permission.WRITE));
        topic.setEnrichment("permissions", permissions);
    }

    @Override
    public void enrichTopicTypeHook(TopicType topicType, ClientState clientState) {
        Topic user = getUserAccount(clientState);
        Map permissions = new HashMap();
        permissions.put("write",  hasPermission(topicType, user, Permission.WRITE));
        permissions.put("create", hasPermission(topicType, user, Permission.CREATE));
        topicType.setEnrichment("permissions", permissions);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount(String username, String password) {
        return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new CompositeValue()
            .put("dm4.accesscontrol.user_name", username)
            .put("dm4.accesscontrol.password", encryptPassword(password))), null);  // clientState=null
    }

    // ---

    /**
     * Returns a user account by username, or <code>null</code> if no such user account exists.
     *
     * @return  a Topic of type <code>dm4.accesscontrol.user_account</code>.
     */
    private Topic getUserAccount(String username) {
        Topic userName = dms.getTopic("dm4.accesscontrol.user_name", new SimpleValue(username), false, null);
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
     * Returns true if the user has a permission for a topic.
     */
    private boolean hasPermission(Topic topic, Topic user, Permission permission) {
        String roleName = null;
        try {
            logger.fine("Determining permission of user " + user + " to " + permission + " " + topic);
            for (RelatedTopic relTopic : getACLEntries(topic.getId())) {
                roleName = relTopic.getTopic().getProperty("de/deepamehta/core/property/rolename").toString();
                Role role = Role.valueOf(roleName.toUpperCase());   // throws IllegalArgumentException
                logger.fine("There is an ACL entry for role " + role);
                boolean allowedForRole = relTopic.getRelation().getProperty(permission.s()).booleanValue();
                logger.fine("value=" + allowedForRole);
                if (allowedForRole && userOccupiesRole(topic, user, role)) {
                    logger.fine("=> ALLOWED");
                    return true;
                }
            }
            logger.fine("=> DENIED");
            return false;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unexpected role \"" + roleName + "\" in ACL entry for " + topic, e);
        }
    }

    private boolean userOccupiesRole(Topic topic, Topic user, Role role) {
        //
        if (role.equals(Role.EVERYONE)) {
            return true;
        }
        //
        if (user == null) {
            return false;
        }
        //
        if (role.equals(Role.MEMBER)) {
            if (userIsMember(user, topic)) {
                return true;
            }
        } else if (role.equals(Role.OWNER)) {
            if (userIsOwner(user, topic)) {
                return true;
            }
        } else if (role.equals(Role.CREATOR)) {
            if (userIsCreator(user, topic)) {
                return true;
            }
        } else {
            throw new RuntimeException("Role \"" + role + "\" not yet handled");
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
        String typeUri = topic.getProperty("de/deepamehta/core/property/TypeURI").toString();
        // String typeUri = topic.typeUri;
        //
        Set<RelatedTopic> relTopics = wsService.getWorkspaces(topic.getId());
        logger.fine("Topic type " + typeUri + " is assigned to " + relTopics.size() + " workspaces");
        for (RelatedTopic relTopic : relTopics) {
            long workspaceId = relTopic.getTopic().getId();
            if (isMemberOfWorkspace(user.getId(), workspaceId)) {
                logger.fine("User " + user + " IS member of workspace " + relTopic.getTopic());
                return true;
            } else {
                logger.fine("User " + user + " is NOT member of workspace " + relTopic.getTopic());
            }
        }
        return false;
    }

    /**
     * Returns true if the user is the owner of the topic.
     */
    private boolean userIsOwner(Topic user, Topic topic) {
        Topic owner = getOwner(topic.getId());
        logger.fine("The owner is " + owner);
        return owner != null && user.getId() == owner.getId();
    }

    /**
     * Returns true if the user is the creator of the topic.
     */
    private boolean userIsCreator(Topic user, Topic topic) {
        Topic creator = getCreator(topic.getId());
        logger.fine("The creator is " + creator);
        return creator != null && user.getId() == creator.getId();
    }

    // ---

    private List<RelatedTopic> getACLEntries(long topicId) {
        return dms.getRelatedTopics(topicId,
            asList("de/deepamehta/core/topictype/role"),
            asList(RelationType.ACCESS_CONTROL.name() + ";INCOMING"), null);
    }

    // ---

    /**
     * Returns the topic's creator (a user topic), or <code>null</code> if no creator is set.
     */
    private Topic getCreator(long topicId) {
        List<RelatedTopic> users = dms.getRelatedTopics(topicId, asList("de/deepamehta/core/topictype/user"),
            asList(RelationType.TOPIC_CREATOR.name() + ";INCOMING"), null);
        //
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new RuntimeException("Ambiguity: topic " + topicId + " has " + users.size() + " creators");
        }
        //
        return users.get(0).getTopic();
    }

    /**
     * Returns the topic's owner (a user topic), or <code>null</code> if no owner is set.
     */
    private Topic getOwner(long topicId) {
        List<RelatedTopic> users = dms.getRelatedTopics(topicId, asList("de/deepamehta/core/topictype/user"),
            asList(RelationType.TOPIC_OWNER.name() + ";INCOMING"), null);
        //
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new RuntimeException("Ambiguity: topic " + topicId + " has " + users.size() + " owners");
        }
        //
        return users.get(0).getTopic();
    }

    private boolean isMemberOfWorkspace(long userId, long workspaceId) {
        return dms.getRelation(workspaceId, userId, RelationType.WORKSPACE_MEMBER.name(), true) != null;
    }
}
