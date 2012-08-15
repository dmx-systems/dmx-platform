package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.plugins.accesscontrol.model.Operation;
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
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.listener.InitializePluginListener;
import de.deepamehta.core.service.listener.IntroduceTopicTypeListener;
import de.deepamehta.core.service.listener.PostCreateTopicListener;
import de.deepamehta.core.service.listener.PreSendTopicListener;
import de.deepamehta.core.service.listener.PreSendTopicTypeListener;
import de.deepamehta.core.service.listener.PostInstallPluginListener;
import de.deepamehta.core.service.listener.PluginServiceArrivedListener;
import de.deepamehta.core.service.listener.PluginServiceGoneListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import javax.ws.rs.core.Context;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, InitializePluginListener,
                                                                    SecurityContext,      PostCreateTopicListener,
                                                                                          PreSendTopicListener,
                                                                                          PreSendTopicTypeListener,
                                                                                          PostInstallPluginListener,
                                                                                          IntroduceTopicTypeListener,
                                                                                          PluginServiceArrivedListener,
                                                                                          PluginServiceGoneListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "";

    // association type semantics ### TODO: to be dropped. Model-driven manipulators required.
    private static final String WORKSPACE_MEMBERSHIP = "dm4.accesscontrol.membership";
    private static final String ROLE_TYPE_USER       = "dm4.core.default";
    private static final String ROLE_TYPE_WORKSPACE  = "dm4.core.default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static final Permissions DEFAULT_TOPIC_PERMISSIONS = new Permissions();
    private static final Permissions DEFAULT_TYPE_PERMISSIONS  = new Permissions();
    static {
        DEFAULT_TOPIC_PERMISSIONS.add(Operation.WRITE, true);
        DEFAULT_TYPE_PERMISSIONS.add(Operation.WRITE, true);
        DEFAULT_TYPE_PERMISSIONS.add(Operation.CREATE, true);
    }

    private FacetsService facetsService;
    private WorkspacesService wsService;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************************************
    // *** AccessControlService Implementation ***
    // *******************************************



    @GET
    @Path("/login/{username}/{password}")
    @Override
    public Topic login(@PathParam("username") String username, @PathParam("password") String password) {
        return login(username, password, request);
    }

    @GET
    @Path("/user/{username}")
    @Override
    public Topic lookupUserAccount(@PathParam("username") String username) {
        logger.info("username=\"" + username + "\"");
        return getUserAccount(username);
    }

    @GET
    @Path("/user")
    @Override
    public Topic getUsername() {
        HttpSession session = request.getSession(false);    // create=false
        if (session == null) {
            return null;
        }
        return getUsername(session);
    }

    // ---

    @GET
    @Path("/topic/{topic_id}")
    @Override
    public Permissions getTopicPermissions(@PathParam("topic_id") long topicId) {
        return permissions(hasPermission(getUsername(), Operation.WRITE, topicId));
    }

    // ---

    @GET
    @Path("/owner/{user_id}/{type_uri}")
    @Override
    public Topic getOwnedTopic(@PathParam("user_id") long userId, @PathParam("type_uri") String typeUri) {
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

    // ### FIXME: ref username instead of user account
    @POST
    @Path("/topic/{topic_id}/owner/{user_id}")
    @Override
    public void setOwner(@PathParam("topic_id") long topicId, @PathParam("user_id") long userId) {
        Topic topic = dms.getTopic(topicId, false, null);
        facetsService.updateFacet(topic, "dm4.accesscontrol.owner_facet", ownerModel(userId), null, null);
    }

    // ---

    @POST
    @Path("/topic/{topic_id}/role/{role_uri}")
    @Override
    public void createACLEntry(@PathParam("topic_id") long topicId, @PathParam("role_uri") Role role,
                                                                                           Permissions permissions) {
        createACLEntry(dms.getTopic(topicId, false, null), role, permissions);
    }

    @Override
    public void createACLEntry(Topic topic, Role role, Permissions permissions) {
        TopicModel aclEntry = aclEntryModel(role, permissions);
        facetsService.updateFacets(topic, "dm4.accesscontrol.acl_facet", asList(aclEntry), null, null);
    }

    // ---

    @POST
    @Path("/user/{user_id}/{workspace_id}")
    @Override
    public void joinWorkspace(@PathParam("workspace_id") long workspaceId, @PathParam("user_id") long userId) {
        dms.createAssociation(new AssociationModel(WORKSPACE_MEMBERSHIP,
            new TopicRoleModel(userId, ROLE_TYPE_USER),
            new TopicRoleModel(workspaceId, ROLE_TYPE_WORKSPACE)), null);
    }



    // **************************************
    // *** SecurityContext Implementation ***
    // **************************************



    /**
     * Checks weather the credentials match an existing User Account.
     *
     * @return  The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    @Override
    public Topic login(String username, String password, HttpServletRequest request) {
        Topic userName = checkCredentials(username, password);
        //
        if (userName != null) {
            HttpSession session = createSession(userName, request);
            logger.info("#####      Logging in as \"" + username + "\" => SUCCESSFUL!" +
                "\n      #####      Creating new " + info(session));
            return userName;
        } else {
            logger.info("#####      Logging in as \"" + username + "\" => FAILED!");
            return null;
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void initializePlugin() {
        try {
            registerFilter(new RequestFilter(this));
        } catch (Exception e) {
            throw new RuntimeException("Registering the security filter failed", e);
        }
    }

    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        // ### TODO: explain
        if (isPluginTopic(topic)) {
            return;
        }
        //
        setCreator(topic);
        createACLEntry(topic, Role.CREATOR, DEFAULT_TOPIC_PERMISSIONS);
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

    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        // ### TODO: explain
        if (topicType.getUri().equals("dm4.core.meta_meta_type")) {
            return;
        }
        //
        setCreator(topicType);
        createACLEntry(topicType, Role.CREATOR, DEFAULT_TYPE_PERMISSIONS);
    }

    // ---

    @Override
    public void preSendTopic(Topic topic, ClientState clientState) {
        // ### TODO: explain
        if (isPluginTopic(topic)) {
            enrichWithPermissions(topic, false);    // write=false
            return;
        }
        //
        logger.info("### Enriching " + info(topic) + " with its permissions (clientState=" + clientState + ")");
        enrichWithPermissions(topic, hasPermission(getUsername(), Operation.WRITE, topic));
    }

    @Override
    public void preSendTopicType(TopicType topicType, ClientState clientState) {
        // Note: there are 2 types whose permissions must be set manually as they can't be calculated the usual way:
        // - "Access Control List Facet": endless recursion would occur. ### FIXDOC
        // - "Meta Meta Type": doesn't exist in DB. Retrieving its ACL would fail.
        if (isPluginType(topicType) || topicType.getUri().equals("dm4.core.meta_meta_type")) {
            enrichWithPermissions(topicType, false, false);     // write=false, create=false
            return;
        }
        //
        logger.info("### Enriching topic type \"" + topicType.getUri() + "\" with its permissions");
        Topic username = getUsername();
        enrichWithPermissions(topicType,
            hasPermission(username, Operation.WRITE, topicType),
            hasPermission(username, Operation.CREATE, topicType));
    }

    @Override
    public void postInstallPlugin() {
        Topic userAccount = createUserAccount(new Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));
        logger.info("Creating \"admin\" user account => ID=" + userAccount.getId());
    }

    @Override
    public void pluginServiceArrived(PluginService service) {
        logger.info("########## Service arrived: " + service);
        if (service instanceof FacetsService) {
            facetsService = (FacetsService) service;
        } else if (service instanceof WorkspacesService) {
            wsService = (WorkspacesService) service;
        }
    }

    @Override
    public void pluginServiceGone(PluginService service) {
        logger.info("########## Service gone: " + service);
        if (service == facetsService) {
            facetsService = null;
        } else if (service == wsService) {
            wsService = null;
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount(Credentials cred) {
        return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new CompositeValue()
            .put("dm4.accesscontrol.username", cred.username)
            .put("dm4.accesscontrol.password", cred.password)), null);  // clientState=null
    }

    // ---

    /**
     * Retrieves the User Account for the specified username.
     *
     * @return  the retrieved User Account (a Topic of type "User Account" /
     *          <code>dm4.accesscontrol.user_account</code>), or <code>null</code> if no such User Account exists.
     */
    private Topic getUserAccount(String username) {
        Topic userName = getUsername(username);
        if (userName == null) {
            return null;
        }
        return getUserAccount(userName);
    }

    /**
     * Prerequisite: username is not <code>null</code>.
     */
    private Topic getUserAccount(Topic username) {
        Topic userAccount = username.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole",
            "dm4.accesscontrol.user_account", true, false, null);  // fetchComposite=true, fetchRelatingComposite=false
        if (userAccount == null) {
            throw new RuntimeException("Data inconsistency: there is no User Account topic for username \"" +
                username.getSimpleValue() + "\" (username=" + username + ")");
        }
        return userAccount;
    }

    // ---

    /**
     * Retrieves the "Username" topic for the specified username.
     *
     * @return  The retrieved Username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *          or <code>null</code> if no such Username topic exists.
     */
    private Topic getUsername(String username) {
        return dms.getTopic("dm4.accesscontrol.username", new SimpleValue(username), false, null);
    }

    // ### FIXME: there is a principal copy in RequestFilter
    private Topic getUsername(HttpSession session) {
        Topic username = (Topic) session.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Session data inconsistency: \"username\" attribute is missing");
        }
        return username;
    }

    // ---

    /**
     * Retrieves the "Username" topic for the "admin" user.
     * If the "admin" user doesn't exist an exception is thrown.
     *
     * @return  The retrieved Username (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>).
     */
    private Topic getAdminUser() {
        Topic username = getUsername(DEFAULT_USERNAME);
        if (username == null) {
            throw new RuntimeException("The \"" + DEFAULT_USERNAME + "\" user doesn't exist");
        }
        return username;
    }

    // ---

    private void setCreator(Topic topic) {
        Topic username = getUsername();
        if (username == null) {
            logger.warning("Assigning a creator to " + info(topic) + " failed (no user is logged in). " +
                "Assigning user \"admin\" instead.");
            username = getAdminUser();
        }
        setCreator(topic, username.getId());
    }

    private void setCreator(Topic topic, long usernameId) {
        facetsService.updateFacet(topic, "dm4.accesscontrol.creator_facet", creatorModel(usernameId), null, null);
    }

    // === Login ===

    private Topic checkCredentials(String username, String password) {
        Topic userName = getUsername(username);
        if (userName == null) {
            return null;
        }
        if (!matches(userName, password)) {
            return null;
        }
        return userName;
    }

    /**
     * Prerequisite: username is not <code>null</code>.
     *
     * @param   password    The encrypted password.
     */
    private boolean matches(Topic username, String password) {
        return getPassword(getUserAccount(username)).equals(password);
    }

    /**
     * @return  The encryted password of the specified User Account.
     */
    private String getPassword(Topic userAccount) {
        return userAccount.getCompositeValue().getString("dm4.accesscontrol.password");
    }

    private HttpSession createSession(Topic username, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("username", username);
        return session;
    }

    // === ACL Entries ===

    /**
     * Checks if a user is allowed to perform an operation on a topic.
     * In this case <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean hasPermission(Topic username, Operation operation, long topicId) {
        Topic topic = dms.getTopic(topicId, false, null);
        return hasPermission(username, operation, topic);
    }

    /**
     * Checks if a user is allowed to perform an operation on a topic.
     * In this case <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean hasPermission(Topic username, Operation operation, Topic topic) {
        logger.fine("Determining permission for " + userInfo(username) + " to " + operation + " " + info(topic));
        for (RelatedTopic aclEntry : getACLEntries(topic)) {
            String roleUri = aclEntry.getCompositeValue().getTopic("dm4.accesscontrol.role").getUri();
            logger.fine("There is an ACL entry for role \"" + roleUri + "\"");
            boolean allowedForRole = getAllowed(aclEntry, operation);
            logger.fine("value=" + allowedForRole);
            if (allowedForRole && userOccupiesRole(topic, username, roleUri)) {
                logger.fine("=> ALLOWED");
                return true;
            }
        }
        logger.fine("=> DENIED");
        return false;
    }

    // ---

    /**
     * Checks if a user occupies a role with regard to the specified topic.
     * In this case <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean userOccupiesRole(Topic topic, Topic username, String roleUri) {
        //
        if (roleUri.equals("dm4.accesscontrol.role_everyone")) {
            return true;
        }
        //
        if (username == null) {
            return false;
        }
        //
        if (roleUri.equals("dm4.accesscontrol.role_user")) {
            return true;
        } else if (roleUri.equals("dm4.accesscontrol.role_member")) {
            if (userIsMember(username, topic)) {
                return true;
            }
        } else if (roleUri.equals("dm4.accesscontrol.role_owner")) {
            if (userIsOwner(username, topic)) {
                return true;
            }
        } else if (roleUri.equals("dm4.accesscontrol.role_creator")) {
            if (userIsCreator(username, topic)) {
                return true;
            }
        } else {
            throw new RuntimeException("\"" + roleUri + "\" is an unexpected role URI");
        }
        return false;
    }

    // ---

    /**
     * Checks if a user is a member of a workspace the topic type is assigned to.
     * In this case <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * FIXME: for the moment only implemented for types, not for regular topics.
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     * @param   topic       actually a topic type.
     */
    private boolean userIsMember(Topic username, Topic topic) {
        Set<RelatedTopic> workspaces = wsService.getWorkspaces(topic.getId());
        logger.fine("Topic type \"" + topic.getUri() + "\" is assigned to " + workspaces.size() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            if (isMemberOfWorkspace(username.getId(), workspace.getId())) {
                logger.fine(userInfo(username) + " IS member of workspace " + workspace);
                return true;
            } else {
                logger.fine(userInfo(username) + " is NOT member of workspace " + workspace);
            }
        }
        return false;
    }

    /**
     * Checks if a user is the owner of the topic.
     * In this case <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private boolean userIsOwner(Topic username, Topic topic) {
        Topic owner = getOwner(topic);
        logger.fine("The owner is " + userInfo(owner));
        return owner != null && username.getId() == owner.getId();
    }

    /**
     * Checks if a user is the creator of the topic.
     * In this case <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private boolean userIsCreator(Topic username, Topic topic) {
        Topic creator = getCreator(topic);
        logger.fine("The creator is " + userInfo(creator));
        return creator != null && username.getId() == creator.getId();
    }

    // ---

    private Set<RelatedTopic> getACLEntries(Topic topic) {
        return facetsService.getFacets(topic, "dm4.accesscontrol.acl_facet");
    }

    private boolean getAllowed(Topic aclEntry, Operation operation) {
        for (TopicModel permission : aclEntry.getCompositeValue().getTopics("dm4.accesscontrol.permission")) {
            if (permission.getCompositeValue().getTopic("dm4.accesscontrol.operation").getUri().equals(operation.uri)) {
                return permission.getCompositeValue().getBoolean("dm4.accesscontrol.allowed");
            }
        }
        return false;
    }

    // ---

    /**
     * Returns the creator of a topic, or <code>null</code> if no creator is set.
     *
     * @return  a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private Topic getCreator(Topic topic) {
        Topic creator = facetsService.getFacet(topic, "dm4.accesscontrol.creator_facet");
        if (creator == null) {
            return null;
        }
        return creator.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.username", false, false, null);  // fetchComposite=false, fetchRelatingComposite=false
    }

    /**
     * Returns the owner of a topic, or <code>null</code> if no owner is set.
     *
     * @return  a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private Topic getOwner(Topic topic) {
        Topic owner = facetsService.getFacet(topic, "dm4.accesscontrol.owner_facet");
        if (owner == null) {
            return null;
        }
        return owner.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.username", false, false, null);  // fetchComposite=false, fetchRelatingComposite=false
    }

    // ---

    private boolean isMemberOfWorkspace(long userId, long workspaceId) {
        return dms.getAssociation(WORKSPACE_MEMBERSHIP, userId, workspaceId,
            ROLE_TYPE_USER, ROLE_TYPE_WORKSPACE, false, null) != null;  // fetchComposite=false
    }

    // ---

    private void enrichWithPermissions(Topic topic, boolean write) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        CompositeValue permissions = getPermissions(topic);
        permissions.put(Operation.WRITE.uri, write);
    }

    private void enrichWithPermissions(TopicType topicType, boolean write, boolean create) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        CompositeValue permissions = getPermissions(topicType);
        permissions.put(Operation.WRITE.uri, write);
        permissions.put(Operation.CREATE.uri, create);
    }

    // ---

    private CompositeValue getPermissions(Topic topic) {
        // Note: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are transient data, not stored in DB, recalculated for each request.
        TopicModel permissionsTopic = topic.getCompositeValue().getTopic("dm4.accesscontrol.permissions", null);
        CompositeValue permissions;
        if (permissionsTopic != null) {
            permissions = permissionsTopic.getCompositeValue();
        } else {
            permissions = new CompositeValue();
            topic.getCompositeValue().put("dm4.accesscontrol.permissions", permissions);
        }
        return permissions;
    }

    // ---

    private TopicModel creatorModel(long usernameId) {
        return new TopicModel("dm4.accesscontrol.creator", new CompositeValue()
            .put_ref("dm4.accesscontrol.username", usernameId)
        );
    }

    // ### FIXME: ref username instead of user account
    private TopicModel ownerModel(long userId) {
        return new TopicModel("dm4.accesscontrol.owner", new CompositeValue()
            .put_ref("dm4.accesscontrol.user_account", userId)
        );
    }

    private TopicModel aclEntryModel(Role role, Permissions permissions) {
        return new TopicModel("dm4.accesscontrol.acl_entry", new CompositeValue()
            .put_ref("dm4.accesscontrol.role", role.uri)
            .put("dm4.accesscontrol.permission", permissions.asTopics())
        );
    }

    // ---

    private Permissions permissions(boolean write) {
        Permissions permissions = new Permissions();
        permissions.add(Operation.WRITE, write);
        return permissions;
    }

    private Permissions permissions(boolean write, boolean create) {
        Permissions permissions = permissions(write);
        permissions.add(Operation.CREATE, create);
        return permissions;
    }

    // ---

    private boolean isPluginTopic(Topic topic) {
        return topic.getTypeUri().startsWith("dm4.accesscontrol.");
    }

    private boolean isPluginType(TopicType type) {
        return type.getUri().startsWith("dm4.accesscontrol.");
    }

    // === Logging ===

    private String userInfo(Topic topic) {
        if (topic == null) {
            return "user <anonymous>";
        }
        return "user \"" + topic.getSimpleValue() + "\" (id=" + topic.getId() + ", typeUri=\"" + topic.getTypeUri() +
            "\")";
    }

    private String info(Topic topic) {
        if (topic == null) {
            return "topic <null>";
        }
        return "topic " + topic.getId() + " (typeUri=\"" + topic.getTypeUri() + "\", uri=\"" + topic.getUri() + "\")";
    }

    private String info(HttpSession session) {
        return "session" + (session != null ? " " + session.getId() +
            " (username=\"" + getUsername(session) + "\")" : ": null");
    }
}
