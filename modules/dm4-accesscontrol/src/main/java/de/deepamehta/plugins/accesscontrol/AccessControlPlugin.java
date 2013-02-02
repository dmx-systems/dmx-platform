package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.accesscontrol.AccessControlList;
import de.deepamehta.core.service.accesscontrol.ACLEntry;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.service.accesscontrol.UserRole;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PreSendAssociationListener;
import de.deepamehta.core.service.event.PreSendAssociationTypeListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.service.event.PreSendTopicTypeListener;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import java.util.Set;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, AllPluginsActiveListener,
                                                                    SecurityContext,   PostCreateTopicListener,
                                                                                       PostCreateAssociationListener,
                                                                                       IntroduceTopicTypeListener,
                                                                                       IntroduceAssociationTypeListener,
                                                                                       PreSendTopicListener,
                                                                                       PreSendAssociationListener,
                                                                                       PreSendTopicTypeListener,
                                                                                       PreSendAssociationTypeListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // security settings
    private static final boolean READ_REQUIRES_LOGIN  = Boolean.getBoolean("dm4.security.read_requires_login");
    private static final boolean WRITE_REQUIRES_LOGIN = Boolean.getBoolean("dm4.security.write_requires_login");
    private static final String SUBNET_FILTER         = System.getProperty("dm4.security.subnet_filter");

    // default user
    private static final String DEFAULT_PASSWORD = "";

    // default ACLs
    private static final AccessControlList DEFAULT_OBJECT_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );
    private static final AccessControlList DEFAULT_TYPE_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER),
        new ACLEntry(Operation.CREATE, UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );

    // ---------------------------------------------------------------------------------------------- Instance Variables

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
    public void login() {
        // Note: the actual login is triggered by the RequestFilter. See checkRequest() below.
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
    @Produces("text/plain")
    @Override
    public String getUsername() {
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
        Topic topic = dms.getTopic(topicId, false, null);
        return createPermissions(hasPermission(getUsername(), Operation.WRITE, topic));
    }

    // ---

    @Override
    public String getCreator(long objectId) {
        return dms.getCreator(objectId);
    }

    @Override
    public void setCreator(long objectId, String username) {
        dms.setCreator(objectId, username);
    }

    // ---

    @Override
    public String getOwner(long objectId) {
        return dms.getOwner(objectId);
    }

    @Override
    public void setOwner(long objectId, String username) {
        dms.setOwner(objectId, username);
    }

    // ---

    @Override
    public void createACL(long objectId, AccessControlList acl) {
        dms.createACL(objectId, acl);
    }

    // ---

    @POST
    @Path("/user/{username}/workspace/{workspace_id}")
    @Override
    public void joinWorkspace(@PathParam("username") String username, @PathParam("workspace_id") long workspaceId) {
        joinWorkspace(getUsername(username), workspaceId);
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
                    authorized = login(new Credentials(authHeader), request);
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
     * Checks weather the credentials match an User Account.
     *
     * @return  <code>true</code> if the credentials match an User Account.
     */
    private boolean login(Credentials cred, HttpServletRequest request) {
        if (checkCredentials(cred)) {
            HttpSession session = createSession(cred.username, request);
            logger.info("##### Logging in as \"" + cred.username + "\" => SUCCESSFUL!" +
                "\n      ##### Creating new " + info(session));
            return true;
        } else {
            logger.info("##### Logging in as \"" + cred.username + "\" => FAILED!");
            return false;
        }
    }

    private boolean checkCredentials(Credentials cred) {
        Topic username = getUsername(cred.username);
        if (username == null) {
            return false;
        }
        return matches(username, cred.password);
    }

    private HttpSession createSession(String username, HttpServletRequest request) {
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

    private String username(HttpSession session) {
        String username = (String) session.getAttribute("username");
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



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void postInstall() {
        Topic userAccount = createUserAccount(new Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));
        logger.info("Creating \"admin\" user account => ID=" + userAccount.getId());
    }

    @Override
    public void init() {
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

    // ---

    @Override
    @ConsumesService("de.deepamehta.plugins.workspaces.service.WorkspacesService")
    public void serviceArrived(PluginService service) {
        wsService = (WorkspacesService) service;
    }

    @Override
    public void serviceGone(PluginService service) {
        wsService = null;
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    /**
     * Setup access control for the default user and the default topicmap.
     *   1) assign default user     to default workspace
     *   2) assign default topicmap to default workspace
     *   3) setup access control for default topicmap
     */
    @Override
    public void allPluginsActive() {
        // 1) assign default user to default workspace
        Topic defaultUser = fetchDefaultUser();
        assignToDefaultWorkspace(defaultUser, "default user (\"admin\")");
        //
        Topic defaultTopicmap = fetchDefaultTopicmap();
        if (defaultTopicmap != null) {
            // 2) assign default topicmap to default workspace
            assignToDefaultWorkspace(defaultTopicmap, "default topicmap (\"untitled\")");
            // 3) setup access control for default topicmap
            setupAccessControlForDefaultTopicmap(defaultTopicmap, defaultUser);
        }
    }

    // ---

    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        setupDefaultAccessControl(topic);
        //
        // when a workspace is created its creator joins automatically
        joinIfWorkspace(topic);
    }

    @Override
    public void postCreateAssociation(Association assoc, ClientState clientState, Directives directives) {
        setupDefaultAccessControl(assoc);
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType, ClientState clientState) {
        setupDefaultAccessControl(topicType);
    }

    @Override
    public void introduceAssociationType(AssociationType assocType, ClientState clientState) {
        setupDefaultAccessControl(assocType);
    }

    // ---

    @Override
    public void preSendTopic(Topic topic, ClientState clientState) {
        enrichWithPermissions(topic, clientState);
    }

    @Override
    public void preSendAssociation(Association assoc, ClientState clientState) {
        enrichWithPermissions(assoc, clientState);
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
        enrichWithPermissions(topicType, clientState);
    }

    @Override
    public void preSendAssociationType(AssociationType assocType, ClientState clientState) {
        enrichWithPermissions(assocType, clientState);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount(Credentials cred) {
        return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new ChildTopicsModel()
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

    private void joinIfWorkspace(Topic topic) {
        if (topic.getTypeUri().equals("dm4.workspaces.workspace")) {
            String username = getUsername();
            // Note: when the default workspace is created there is no user logged in yet.
            // The default user is assigned to the default workspace later on (see allPluginsActive()).
            if (username != null) {
                joinWorkspace(username, topic.getId());
            }
        }
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
            if (getCreator(defaultTopicmap.getId()) != null) {
                logger.info(operation + " ABORTED -- already setup");
                return;
            }
            //
            logger.info(operation);
            String username = defaultUser.getSimpleValue().toString();
            setupAccessControl(defaultTopicmap, DEFAULT_OBJECT_ACL, username);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    private Topic fetchDefaultTopicmap() {
        // Note: the Access Control plugin does not DEPEND on the Topicmaps plugin but is designed to work TOGETHER
        // with the Topicmaps plugin.
        // Currently the Access Control plugin needs to know some Topicmaps internals e.g. the URI of the default
        // topicmap. ### TODO: make "optional plugin dependencies" an explicit concept. Plugins must be able to ask
        // the core weather a certain plugin is installed (regardles weather it is activated already) and would wait
        // for its service only if installed.
        return dms.getTopic("uri", new SimpleValue("dm4.topicmaps.default_topicmap"), false, null);
    }



    // === ACL Entries ===

    /**
     * Sets the logged in user as the creator and the owner of the specified object
     * and creates a default access control entry for it.
     *
     * If no user is logged in, nothing is performed.
     */
    private void setupDefaultAccessControl(DeepaMehtaObject object) {
        String username = getUsername();
        //
        if (username == null) {
            logger.fine("Assigning a creator and default access control entry to " +
                info(object) + " ABORTED -- no user is logged in");
            return;
        }
        //
        setupAccessControl(object, DEFAULT_OBJECT_ACL, username);
    }

    private void setupDefaultAccessControl(Type type) {
        String username = getUsername();
        //
        if (username == null) {
            username = fetchDefaultUser().getSimpleValue().toString();
        }
        //
        setupAccessControl(type, DEFAULT_TYPE_ACL, username);
    }

    // ---

    private void setupAccessControl(DeepaMehtaObject object, AccessControlList acl, String username) {
        setCreator(object.getId(), username);
        setOwner(object.getId(), username);
        createACL(object.getId(), acl);
    }

    // ---

    /**
     * Checks if a user is allowed to perform an operation on an object (topic or association).
     * If so, <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean hasPermission(String username, Operation operation, DeepaMehtaObject object) {
        try {
            logger.fine("Determining permission for " + userInfo(username) + " to " + operation + " " + info(object));
            UserRole[] userRoles = dms.getACL(object.getId()).getUserRoles(operation);
            for (UserRole userRole : userRoles) {
                logger.fine("There is an ACL entry for user role " + userRole);
                if (userOccupiesRole(username, userRole, object)) {
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

    /**
     * Checks if a user occupies a role with regard to the specified object.
     * If so, <code>true</code> is returned.
     *
     * @param   username    the logged in user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>),
     *                      or <code>null</code> if no user is logged in.
     */
    private boolean userOccupiesRole(String username, UserRole userRole, DeepaMehtaObject object) {
        switch (userRole) {
        case EVERYONE:
            return true;
        case USER:
            return username != null;
        case MEMBER:
            return username != null && userIsMember(username, object);
        case OWNER:
            return username != null && userIsOwner(username, object);
        case CREATOR:
            return username != null && userIsCreator(username, object);
        default:
            throw new RuntimeException(userRole + " is an unsupported user role");
        }
    }

    // ---

    /**
     * Checks if a user is a member of any workspace the object is assigned to.
     * If so, <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>). ### FIXDOC
     * @param   object      the object in question.
     */
    private boolean userIsMember(String username, DeepaMehtaObject object) {
        Set<RelatedTopic> workspaces = wsService.getWorkspaces(object);
        logger.fine(info(object) + " is assigned to " + workspaces.size() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            if (wsService.isAssignedToWorkspace(getUsername(username), workspace.getId())) {
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
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>). ### FIXDOC
     */
    private boolean userIsOwner(String username, DeepaMehtaObject object) {
        String owner = getOwner(object.getId());
        logger.fine("The owner is " + userInfo(owner));
        return owner != null && owner.equals(username);
    }

    /**
     * Checks if a user is the creator of the object.
     * If so, <code>true</code> is returned.
     *
     * Prerequisite: a user is logged in (<code>username</code> is not <code>null</code>).
     *
     * @param   username    a Topic of type "Username" (<code>dm4.accesscontrol.username</code>). ### FIXDOC
     */
    private boolean userIsCreator(String username, DeepaMehtaObject object) {
        String creator = getCreator(object.getId());
        logger.fine("The creator is " + userInfo(creator));
        return creator != null && creator.equals(username);
    }

    // ---

    public void enrichWithPermissions(DeepaMehtaObject object, ClientState clientState) {
        logger.fine("### Enriching " + info(object) + " with its permissions (clientState=" + clientState + ")");
        enrichWithPermissions(object, hasPermission(getUsername(), Operation.WRITE, object));
    }

    public void enrichWithPermissions(Type type, ClientState clientState) {
        logger.fine("### Enriching type \"" + type.getUri() + "\" with its permissions");
        String username = getUsername();
        enrichWithPermissions(type, hasPermission(username, Operation.WRITE, type),
                                    hasPermission(username, Operation.CREATE, type));
    }
    
    // ---

    private void enrichWithPermissions(DeepaMehtaObject object, boolean write) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        ChildTopicsModel permissions = permissions(object);
        permissions.put(Operation.WRITE.uri, write);
    }

    private void enrichWithPermissions(Type type, boolean write, boolean create) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        ChildTopicsModel permissions = permissions(type);
        permissions.put(Operation.WRITE.uri, write);
        permissions.put(Operation.CREATE.uri, create);
    }

    // ---

    private ChildTopicsModel permissions(DeepaMehtaObject object) {
        // Note: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are transient data, not stored in DB, recalculated for each request.
        TopicModel permissionsTopic = object.getCompositeValue().getTopic("dm4.accesscontrol.permissions", null);
        ChildTopicsModel permissions;
        if (permissionsTopic != null) {
            permissions = permissionsTopic.getCompositeValue();
        } else {
            permissions = new ChildTopicsModel();
            object.getCompositeValue().put("dm4.accesscontrol.permissions", permissions);
        }
        return permissions;
    }

    // ---

    private Permissions createPermissions(boolean write) {
        return new Permissions().add(Operation.WRITE, write);
    }

    private Permissions createPermissions(boolean write, boolean create) {
        return createPermissions(write).add(Operation.CREATE, create);
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

    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }

    private String info(HttpSession session) {
        return "session" + (session != null ? " " + session.getId() +
            " (username=" + username(session) + ")" : ": null");
    }
}
