package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.facets.service.FacetsService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.InitializePluginListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PluginServiceArrivedListener;
import de.deepamehta.core.service.event.PluginServiceGoneListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostInstallPluginListener;
import de.deepamehta.core.service.event.PreSendAssociationListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.service.event.PreSendTopicTypeListener;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import static java.util.Arrays.asList;
import java.util.Set;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, PostInstallPluginListener,
                                                                    SecurityContext,      InitializePluginListener,
                                                                                          AllPluginsActiveListener,
                                                                                          PostCreateTopicListener,
                                                                                          PostCreateAssociationListener,
                                                                                          IntroduceTopicTypeListener,
                                                                                          PreSendTopicListener,
                                                                                          PreSendAssociationListener,
                                                                                          PreSendTopicTypeListener,
                                                                                          PluginServiceArrivedListener,
                                                                                          PluginServiceGoneListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // security settings
    private static final boolean READ_REQUIRES_LOGIN  = Boolean.valueOf(
                                                        System.getProperty("dm4.security.read_requires_login"));
    private static final boolean WRITE_REQUIRES_LOGIN = Boolean.valueOf(
                                                        System.getProperty("dm4.security.write_requires_login"));
    private static final String SUBNET_FILTER         = System.getProperty("dm4.security.subnet_filter");

    // default user
    private static final String DEFAULT_PASSWORD = "";

    // default user role
    private static final UserRole DEFAULT_USER_ROLE = UserRole.MEMBER;

    // default permissions
    private static final Permissions DEFAULT_TOPIC_PERMISSIONS       = new Permissions()
                                                                            .add(Operation.WRITE, true);
    private static final Permissions DEFAULT_ASSOCIATION_PERMISSIONS = new Permissions()
                                                                            .add(Operation.WRITE, true);
    private static final Permissions DEFAULT_TYPE_PERMISSIONS        = new Permissions()
                                                                            .add(Operation.WRITE, true)
                                                                            .add(Operation.CREATE, true);

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FacetsService facetsService;
    private WorkspacesService wsService;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************************************
    // *** AccessControlService Implementation ***
    // *******************************************



    @POST
    @Path("/login")
    @Override
    public Topic login() {
        // Note: the actual login is triggered by the RequestFilter. See checkRequest() below.
        return getUsername();
    }

    @POST
    @Path("/logout")
    @Produces("text/plain")
    @Override
    public boolean logout() {
        request.getSession(false).invalidate();                 // create=false
        return READ_REQUIRES_LOGIN;
    }

    // ---

    @GET
    @Path("/user")
    @Override
    public Topic getUsername() {
        try {
            HttpSession session = request.getSession(false);    // create=false
            if (session == null) {
                return null;
            }
            return username(session);
        } catch (IllegalStateException e) {
            // Note: if not invoked through network no request (and thus no session) is available.
            // This happens e.g. while starting up.
            return null;    // user is unknown
        }
    }

    @Override
    public Topic getUsername(String username) {
        return dms.getTopic("dm4.accesscontrol.username", new SimpleValue(username), false, null);
    }

    // ---

    @GET
    @Path("/topic/{topic_id}")
    @Override
    public Permissions getTopicPermissions(@PathParam("topic_id") long topicId) {
        return createPermissions(hasPermission(getUsername(), Operation.WRITE, topicId));
    }

    // ---

    @Override
    public void setCreator(DeepaMehtaObject object, long usernameId) {
        facetsService.updateFacet(object, "dm4.accesscontrol.creator_facet", createCreatorModel(usernameId),
            null, null);
    }

    @Override
    public void setOwner(DeepaMehtaObject object, long usernameId) {
        facetsService.updateFacet(object, "dm4.accesscontrol.owner_facet", createOwnerModel(usernameId),
            null, null);
    }

    // ---

    @Override
    public Topic getCreator(DeepaMehtaObject object) {
        Topic creator = facetsService.getFacet(object, "dm4.accesscontrol.creator_facet");
        if (creator == null) {
            return null;
        }
        return creator.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.username", false, false, null);  // fetchComposite=false, fetchRelatingComposite=false
    }

    @Override
    public Topic getOwner(DeepaMehtaObject object) {
        Topic owner = facetsService.getFacet(object, "dm4.accesscontrol.owner_facet");
        if (owner == null) {
            return null;
        }
        return owner.getRelatedTopic("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.username", false, false, null);  // fetchComposite=false, fetchRelatingComposite=false
    }

    // ---

    @POST
    @Path("/topic/{topic_id}/userrole/{user_role_uri}")
    @Override
    public void createACLEntry(@PathParam("topic_id") long topicId, @PathParam("user_role_uri") UserRole userRole,
                                                                                              Permissions permissions) {
        createACLEntry(dms.getTopic(topicId, false, null), userRole, permissions);
    }

    @Override
    public void createACLEntry(DeepaMehtaObject object, UserRole userRole, Permissions permissions) {
        TopicModel aclEntry = createAclEntryModel(userRole, permissions);
        // Note: acl_facet is a multi-facet. So we must pass a (one-element) list.
        facetsService.updateFacets(object, "dm4.accesscontrol.acl_facet", asList(aclEntry), null, null);
    }

    // ---

    @POST
    @Path("/user/{username_id}/{workspace_id}")
    @Override
    public void joinWorkspace(@PathParam("username_id") long usernameId, @PathParam("workspace_id") long workspaceId) {
        Topic username = dms.getTopic(usernameId, false, null);
        joinWorkspace(username, workspaceId);
    }

    @Override
    public void joinWorkspace(Topic username, long workspaceId) {
        try {
            wsService.assignToWorkspace(username, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Joining user " + username + " to workspace " + workspaceId + " failed", e);
        }
    }



    // **************************************
    // *** SecurityContext Implementation ***
    // **************************************



    /**
     * Called from {@link RequestFilter#doFilter}.
     */
    @Override
    public void checkRequest(HttpServletRequest request) throws AccessControlException {
        logger.fine("##### " + request.getMethod() + " " + request.getRequestURL() +
            "\n      ##### \"Authorization\"=\"" + request.getHeader("Authorization") + "\"" + 
            "\n      ##### " + info(request.getSession(false)));    // create=false
        //
        checkRequestOrigin(request);
        checkAuthorization(request);
    }

    // ---

    private void checkRequestOrigin(HttpServletRequest request) throws AccessControlException {
        String remoteAddr = request.getRemoteAddr();
        boolean isInRange = JavaUtils.isInRange(remoteAddr, SUBNET_FILTER);
        //
        logger.fine("Remote address=\"" + remoteAddr + "\", dm4.security.subnet_filter=\"" + SUBNET_FILTER +
            "\" => " + (isInRange ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!isInRange) {
            throw new AccessControlException("Request from \"" + remoteAddr + "\" is not allowed " +
                "(dm4.security.subnet_filter=\"" + SUBNET_FILTER + "\")", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void checkAuthorization(HttpServletRequest request) throws AccessControlException {
        boolean authorized = false;
        if (isLoginRequired(request)) {
            HttpSession session = request.getSession(false);    // create=false
            if (session != null) {
                authorized = true;
            } else {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    Topic username = login(new Credentials(authHeader), request);
                    if (username != null) {
                        authorized = true;
                    }
                }
            }
        } else {
            authorized = true;
        }
        //
        if (!authorized) {
            throw new AccessControlException("Request " + request + " is not authorized",
                HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    // ---

    private boolean isLoginRequired(HttpServletRequest request) {
        return request.getMethod().equals("GET") ? READ_REQUIRES_LOGIN : WRITE_REQUIRES_LOGIN;
    }

    /**
     * Checks weather the credentials match an existing User Account.
     *
     * @return  The username of the matched User Account (a Topic of type "Username" /
     *          <code>dm4.accesscontrol.username</code>), or <code>null</code> if there is no matching User Account.
     */
    private Topic login(Credentials cred, HttpServletRequest request) {
        Topic username = checkCredentials(cred);
        //
        if (username != null) {
            HttpSession session = createSession(username, request);
            logger.info("##### Logging in as \"" + cred.username + "\" => SUCCESSFUL!" +
                "\n      ##### Creating new " + info(session));
            return username;
        } else {
            logger.info("##### Logging in as \"" + cred.username + "\" => FAILED!");
            return null;
        }
    }

    private Topic checkCredentials(Credentials cred) {
        Topic username = getUsername(cred.username);
        if (username == null) {
            return null;
        }
        if (!matches(username, cred.password)) {
            return null;
        }
        return username;
    }

    private HttpSession createSession(Topic username, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("username", username);
        return session;
    }

    // ---

    /**
     * Prerequisite: username is not <code>null</code>.
     *
     * @param   password    The encrypted password.
     */
    private boolean matches(Topic username, String password) {
        return password(fetchUserAccount(username)).equals(password);
    }

    /**
     * Prerequisite: username is not <code>null</code>.
     */
    private Topic fetchUserAccount(Topic username) {
        Topic userAccount = username.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole",
            "dm4.accesscontrol.user_account", true, false, null);  // fetchComposite=true, fetchRelatingComposite=false
        if (userAccount == null) {
            throw new RuntimeException("Data inconsistency: there is no User Account topic for username \"" +
                username.getSimpleValue() + "\" (username=" + username + ")");
        }
        return userAccount;
    }

    // ---

    private Topic username(HttpSession session) {
        Topic username = (Topic) session.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Session data inconsistency: \"username\" attribute is missing");
        }
        return username;
    }

    /**
     * @return  The encryted password of the specified User Account.
     */
    private String password(Topic userAccount) {
        return userAccount.getCompositeValue().getString("dm4.accesscontrol.password");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postInstallPlugin() {
        Topic userAccount = createUserAccount(new Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));
        logger.info("Creating \"admin\" user account => ID=" + userAccount.getId());
    }

    @Override
    public void initializePlugin() {
        try {
            logger.info("Security settings:" +
                "\n    dm4.security.read_requires_login=" + READ_REQUIRES_LOGIN +
                "\n    dm4.security.write_requires_login=" + WRITE_REQUIRES_LOGIN +
                "\n    dm4.security.subnet_filter=\""+ SUBNET_FILTER + "\"");
            //
            registerFilter(new RequestFilter(this));
        } catch (Exception e) {
            throw new RuntimeException("Registering the request filter failed", e);
        }
    }

    @Override
    public void allPluginsActive() {
        //
        Topic defaultUser = fetchDefaultUser();
        assignToDefaultWorkspace(defaultUser, "default user (\"admin\")");
        //
        // Note: the Access Control plugin does not DEPEND on the Topicmaps plugin but is designed to work TOGETHER
        // with the Topicmaps plugin.
        // Currently the Access Control plugin needs to know some Topicmaps internals e.g. the URI of the default
        // topicmap. ### TODO: make "optional plugin dependencies" an explicit concept. Plugins must be able to ask
        // the core weather a certain plugin is installed (regardles weather it is activated already) and would wait
        // for its service only if installed.
        //
        Topic defaultTopicmap = fetchDefaultTopicmap();
        if (defaultTopicmap != null) {
            assignToDefaultWorkspace(defaultTopicmap, "default topicmap (\"untitled\")");
            //
            setupAccessControlForDefaultTopicmap(defaultTopicmap, defaultUser);
        }
    }

    // ---

    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        // ### TODO: explain
        if (isOwnTopic(topic)) {
            return;
        }
        //
        setupDefaultAccessControl(topic, DEFAULT_TOPIC_PERMISSIONS);
        //
        // when a workspace is created its creator joins automatically
        if (topic.getTypeUri().equals("dm4.workspaces.workspace")) {
            Topic username = getUsername();
            // Note: when the default workspace is created there is no user logged in yet.
            // The default user is assigned to the default workspace later on (see allPluginsActive()).
            if (username != null) {
                joinWorkspace(username, topic.getId());
            }
        }
    }

    @Override
    public void postCreateAssociation(Association assoc, ClientState clientState, Directives directives) {
        // ### TODO: explain
        if (isOwnAssociation(assoc)) {
            return;
        }
        //
        setupDefaultAccessControl(assoc, DEFAULT_ASSOCIATION_PERMISSIONS);
    }

    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        setupDefaultAccessControl(topicType, DEFAULT_TYPE_PERMISSIONS, fetchDefaultUser());
    }

    // ---

    @Override
    public void preSendTopic(Topic topic, ClientState clientState) {
        // ### TODO: explain
        if (isOwnTopic(topic)) {
            enrichWithPermissions(topic, false);    // write=false
            return;
        }
        //
        logger.fine("### Enriching " + info(topic) + " with its permissions (clientState=" + clientState + ")");
        enrichWithPermissions(topic, hasPermission(getUsername(), Operation.WRITE, topic));
    }

    @Override
    public void preSendAssociation(Association assoc, ClientState clientState) {
        // ### TODO: explain
        if (isOwnAssociation(assoc)) {
            enrichWithPermissions(assoc, false);    // write=false
            return;
        }
        //
        logger.fine("### Enriching " + info(assoc) + " with its permissions (clientState=" + clientState + ")");
        enrichWithPermissions(assoc, hasPermission(getUsername(), Operation.WRITE, assoc));
    }

    @Override
    public void preSendTopicType(TopicType topicType, ClientState clientState) {
        // Note: the permissions for "Meta Meta Type" must be set manually.
        // This type doesn't exist in DB. Fetching its ACL entries would fail.
        if (topicType.getUri().equals("dm4.core.meta_meta_type")) {
            enrichWithPermissions(topicType, false, false);     // write=false, create=false
            return;
        }
        //
        logger.fine("### Enriching topic type \"" + topicType.getUri() + "\" with its permissions");
        Topic username = getUsername();
        enrichWithPermissions(topicType,
            hasPermission(username, Operation.WRITE, topicType),
            hasPermission(username, Operation.CREATE, topicType));
    }

    // ---

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

    /**
     * Fetches the default user ("admin").
     *
     * @throws  RuntimeException    If the default user doesn't exist.
     *
     * @return  The default user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>).
     */
    private Topic fetchDefaultUser() {
        Topic username = getUsername(DEFAULT_USERNAME);
        if (username == null) {
            throw new RuntimeException("The default user (\"" + DEFAULT_USERNAME + "\") doesn't exist");
        }
        return username;
    }



    // === All Plugins Activated ===

    private void assignToDefaultWorkspace(Topic topic, String info) {
        String operation = "### Assigning the " + info + " to the default workspace (\"DeepaMehta\")";
        try {
            // abort if already assigned
            Set<RelatedTopic> workspaces = wsService.getWorkspaces(topic);
            if (workspaces.size() != 0) {
                logger.info("### Assigning the " + info + " to a workspace ABORTED -- " +
                    "already assigned (" + DeepaMehtaUtils.topicNames(workspaces) + ")");
                return;
            }
            //
            logger.info(operation);
            Topic defaultWorkspace = wsService.getDefaultWorkspace();
            wsService.assignToWorkspace(topic, defaultWorkspace.getId());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    private void setupAccessControlForDefaultTopicmap(Topic defaultTopicmap, Topic defaultUser) {
        String operation = "### Setup access control for the default topicmap (\"untitled\")";
        try {
            // Note: we only check for creator assignment.
            // If an object has a creator assignment it is expected to have an ACL entry as well.
            Topic username = getCreator(defaultTopicmap);
            if (username != null) {
                logger.info(operation + " ABORTED -- already setup");
                return;
            }
            //
            logger.info(operation);
            setupDefaultAccessControl(defaultTopicmap, DEFAULT_TOPIC_PERMISSIONS, defaultUser);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    private Topic fetchDefaultTopicmap() {
        return dms.getTopic("uri", new SimpleValue("dm4.topicmaps.default_topicmap"), false, null);
    }



    // === ACL Entries ===

    /**
     * Sets the logged in user as the creator of the specified object
     * and creates a default access control entry for it.
     *
     * If no user is logged in, nothing is performed.
     */
    private void setupDefaultAccessControl(DeepaMehtaObject object, Permissions permissions) {
        Topic username = getUsername();
        //
        if (username == null) {
            logger.fine("Assigning a creator and default access control entry to " +
                info(object) + " ABORTED -- no user is logged in");
            return;
        }
        //
        setupDefaultAccessControl(object, permissions, username);
    }

    private void setupDefaultAccessControl(DeepaMehtaObject object, Permissions permissions, Topic username) {
        setCreator(object, username.getId());
        createACLEntry(object, DEFAULT_USER_ROLE, permissions);
    }

    // ---

    /**
     * Checks if a user is allowed to perform an operation on a topic.
     * If so, <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean hasPermission(Topic username, Operation operation, long topicId) {
        Topic topic = dms.getTopic(topicId, false, null);
        return hasPermission(username, operation, topic);
    }

    /**
     * Checks if a user is allowed to perform an operation on an object.
     * If so, <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean hasPermission(Topic username, Operation operation, DeepaMehtaObject object) {
        try {
            logger.fine("Determining permission for " + userInfo(username) + " to " + operation + " " + info(object));
            for (RelatedTopic aclEntry : fetchACLEntries(object)) {
                String userRoleUri = aclEntry.getCompositeValue().getTopic("dm4.accesscontrol.user_role").getUri();
                logger.fine("There is an ACL entry for user role \"" + userRoleUri + "\"");
                boolean allowedForUserRole = allowed(aclEntry, operation);
                logger.fine("value=" + allowedForUserRole);
                if (allowedForUserRole && userOccupiesRole(object, username, userRoleUri)) {
                    logger.fine("=> ALLOWED");
                    return true;
                }
            }
            logger.fine("=> DENIED");
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Determining permission for " + info(object) + " failed (" +
                userInfo(username) + ", operation=" + operation + ")", e);
        }
    }

    // ---

    /**
     * Fetches all ACL entries of the specified object.
     */
    private Set<RelatedTopic> fetchACLEntries(DeepaMehtaObject object) {
        return facetsService.getFacets(object, "dm4.accesscontrol.acl_facet");
    }

    /**
     * For the specified ACL entry: reads out the "allowed" value for the specified operation.
     * If no "allowed" value is set for that operation <code>false</code> is returned.
     */
    private boolean allowed(Topic aclEntry, Operation operation) {
        for (TopicModel permission : aclEntry.getCompositeValue().getTopics("dm4.accesscontrol.permission")) {
            if (permission.getCompositeValue().getTopic("dm4.accesscontrol.operation").getUri().equals(operation.uri)) {
                return permission.getCompositeValue().getBoolean("dm4.accesscontrol.allowed");
            }
        }
        return false;
    }

    // ---

    /**
     * Checks if a user occupies a role with regard to the specified object.
     * If so, <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean userOccupiesRole(DeepaMehtaObject object, Topic username, String userRoleUri) {
        //
        if (userRoleUri.equals("dm4.accesscontrol.user_role.everyone")) {
            return true;
        }
        //
        if (username == null) {
            return false;
        }
        //
        if (userRoleUri.equals("dm4.accesscontrol.user_role.user")) {
            return true;
        } else if (userRoleUri.equals("dm4.accesscontrol.user_role.member")) {
            if (userIsMember(username, object)) {
                return true;
            }
        } else if (userRoleUri.equals("dm4.accesscontrol.user_role.owner")) {
            if (userIsOwner(username, object)) {
                return true;
            }
        } else if (userRoleUri.equals("dm4.accesscontrol.user_role.creator")) {
            if (userIsCreator(username, object)) {
                return true;
            }
        } else {
            throw new RuntimeException("\"" + userRoleUri + "\" is an unexpected user role URI");
        }
        return false;
    }

    // ---

    /**
     * Checks if a user is a member of any workspace the object is assigned to.
     * If so, <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     * @param   object      the object in question.
     */
    private boolean userIsMember(Topic username, DeepaMehtaObject object) {
        Set<RelatedTopic> workspaces = wsService.getWorkspaces(object);
        logger.fine(info(object) + " is assigned to " + workspaces.size() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            if (wsService.isAssignedToWorkspace(username, workspace.getId())) {
                logger.fine(userInfo(username) + " IS member of workspace " + workspace);
                return true;
            } else {
                logger.fine(userInfo(username) + " is NOT member of workspace " + workspace);
            }
        }
        return false;
    }

    /**
     * Checks if a user is the owner of the object.
     * If so, <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private boolean userIsOwner(Topic username, DeepaMehtaObject object) {
        Topic owner = getOwner(object);
        logger.fine("The owner is " + userInfo(owner));
        return owner != null && owner.getId() == username.getId();
    }

    /**
     * Checks if a user is the creator of the object.
     * If so, <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>).
     */
    private boolean userIsCreator(Topic username, DeepaMehtaObject object) {
        Topic creator = getCreator(object);
        logger.fine("The creator is " + userInfo(creator));
        return creator != null && creator.getId() == username.getId();
    }

    // ---

    private void enrichWithPermissions(DeepaMehtaObject object, boolean write) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        CompositeValue permissions = permissions(object);
        permissions.put(Operation.WRITE.uri, write);
    }

    private void enrichWithPermissions(TopicType topicType, boolean write, boolean create) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        CompositeValue permissions = permissions(topicType);
        permissions.put(Operation.WRITE.uri, write);
        permissions.put(Operation.CREATE.uri, create);
    }

    // ---

    private CompositeValue permissions(DeepaMehtaObject object) {
        // Note: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are transient data, not stored in DB, recalculated for each request.
        TopicModel permissionsTopic = object.getCompositeValue().getTopic("dm4.accesscontrol.permissions", null);
        CompositeValue permissions;
        if (permissionsTopic != null) {
            permissions = permissionsTopic.getCompositeValue();
        } else {
            permissions = new CompositeValue();
            object.getCompositeValue().put("dm4.accesscontrol.permissions", permissions);
        }
        return permissions;
    }

    // ---

    private TopicModel createCreatorModel(long usernameId) {
        return new TopicModel("dm4.accesscontrol.creator", new CompositeValue()
            .putRef("dm4.accesscontrol.username", usernameId)
        );
    }

    private TopicModel createOwnerModel(long usernameId) {
        return new TopicModel("dm4.accesscontrol.owner", new CompositeValue()
            .putRef("dm4.accesscontrol.username", usernameId)
        );
    }

    private TopicModel createAclEntryModel(UserRole userRole, Permissions permissions) {
        return new TopicModel("dm4.accesscontrol.acl_entry", new CompositeValue()
            .putRef("dm4.accesscontrol.user_role", userRole.uri)
            .put("dm4.accesscontrol.permission", permissions.asTopics())
        );
    }

    // ---

    private Permissions createPermissions(boolean write) {
        return new Permissions().add(Operation.WRITE, write);
    }

    @SuppressWarnings("unused")
    private Permissions createPermissions(boolean write, boolean create) {
        return createPermissions(write).add(Operation.CREATE, create);
    }

    // ===

    private boolean isOwnTopic(Topic topic) {
        return isOwnUri(topic.getTypeUri());
    }

    private boolean isOwnAssociation(Association assoc) {
        return isOwnRole(assoc.getRole1()) || isOwnRole(assoc.getRole2());            
    }

    private boolean isOwnRole(Role role) {
        if (!(role instanceof TopicRole)) {
            return false;
        }
        Topic topic = ((TopicRole) role).getTopic();
        return isOwnTopic(topic);
    }

    private boolean isOwnUri(String uri) {
        return uri.startsWith("dm4.accesscontrol.");
    }



    // === Logging ===

    private String info(DeepaMehtaObject object) {
        if (object instanceof TopicType) {
            return "topic type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof Topic) {
            return "topic " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\", uri=\"" + object.getUri() +
                "\")";
        } else if (object instanceof Association) {
            return "association " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\")";
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }

    private String userInfo(Topic username) {
        if (username == null) {
            return "user <anonymous>";
        }
        return "user \"" + username.getSimpleValue() + "\" (id=" + username.getId() + ", typeUri=\"" +
            username.getTypeUri() + "\")";
    }

    private String info(HttpSession session) {
        return "session" + (session != null ? " " + session.getId() +
            " (username=" + username(session) + ")" : ": null");
    }
}
