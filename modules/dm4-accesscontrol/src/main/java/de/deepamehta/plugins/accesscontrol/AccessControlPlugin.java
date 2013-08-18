package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.Permissions;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendAssociationTypeListener;
import de.deepamehta.core.service.event.PreSendTopicTypeListener;
import de.deepamehta.core.service.event.ResourceRequestFilterListener;
import de.deepamehta.core.service.event.ServiceRequestFilterListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, AllPluginsActiveListener,
                                                                                       PostCreateTopicListener,
                                                                                       PostCreateAssociationListener,
                                                                                       PostUpdateTopicListener,
                                                                                       IntroduceTopicTypeListener,
                                                                                       IntroduceAssociationTypeListener,
                                                                                       ServiceRequestFilterListener,
                                                                                       ResourceRequestFilterListener,
                                                                                       PreSendTopicTypeListener,
                                                                                       PreSendAssociationTypeListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Security settings
    private static final boolean READ_REQUIRES_LOGIN  = Boolean.getBoolean("dm4.security.read_requires_login");
    private static final boolean WRITE_REQUIRES_LOGIN = Boolean.getBoolean("dm4.security.write_requires_login");
    private static final String SUBNET_FILTER         = System.getProperty("dm4.security.subnet_filter");

    private static final String AUTHENTICATION_REALM = "DeepaMehta";

    // Default user account
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "";

    // Default ACLs
    private static final AccessControlList DEFAULT_INSTANCE_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );
    private static final AccessControlList DEFAULT_TYPE_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER),
        new ACLEntry(Operation.CREATE, UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );
    //
    private static final AccessControlList DEFAULT_USER_ACCOUNT_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER)
    );

    // Property names
    private static String URI_CREATOR = "dm4.accesscontrol.creator";
    private static String URI_OWNER = "dm4.accesscontrol.owner";
    private static String URI_ACL = "dm4.accesscontrol.acl";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WorkspacesService wsService;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************************************
    // *** AccessControlService Implementation ***
    // *******************************************



    // === Session ===

    @POST
    @Path("/login")
    @Override
    public void login() {
        // Note: the actual login is performed by the request filter. See requestFilter().
    }

    @POST
    @Path("/logout")
    @Override
    public void logout() {
        HttpSession session = request.getSession(false);        // create=false
        logger.info("##### Logging out from " + info(session));
        session.invalidate();
        //
        // For a "private" DeepaMehta installation: emulate a HTTP logout by forcing the webbrowser to bring up its
        // login dialog and to forget the former Authorization information. The user is supposed to press "Cancel".
        // The login dialog can't be used to login again.
        if (READ_REQUIRES_LOGIN) {
            throw401Unauthorized();
        }
    }



    // === User ===

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
        return dms.getTopic("dm4.accesscontrol.username", new SimpleValue(username), false);
    }



    // === Permissions ===

    @GET
    @Path("/topic/{id}")
    @Override
    public Permissions getTopicPermissions(@PathParam("id") long topicId) {
        return getPermissions(dms.getTopic(topicId, false));
    }

    @GET
    @Path("/association/{id}")
    @Override
    public Permissions getAssociationPermissions(@PathParam("id") long assocId) {
        return getPermissions(dms.getAssociation(assocId, false));
    }



    // === Creator ===

    @Override
    public String getCreator(DeepaMehtaObject object) {
        return object.hasProperty(URI_CREATOR) ? (String) object.getProperty(URI_CREATOR) : null;
    }

    @Override
    public void setCreator(DeepaMehtaObject object, String username) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            object.setProperty(URI_CREATOR, username, true);    // addToIndex=true
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting the creator of object " + object.getId() + " failed", e);
        } finally {
            tx.finish();
        }
    }



    // === Owner ===

    @Override
    public String getOwner(DeepaMehtaObject object) {
        return object.hasProperty(URI_OWNER) ? (String) object.getProperty(URI_OWNER) : null;
    }

    @Override
    public void setOwner(DeepaMehtaObject object, String username) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            object.setProperty(URI_OWNER, username, true);      // addToIndex=true
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting the owner of object " + object.getId() + " failed", e);
        } finally {
            tx.finish();
        }
    }



    // === Access Control List ===

    @Override
    public AccessControlList getACL(DeepaMehtaObject object) {
        try {
            if (object.hasProperty(URI_ACL)) {
                return new AccessControlList(new JSONObject((String) object.getProperty(URI_ACL)));
            } else {
                return new AccessControlList();
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching the ACL of object " + object.getId() + " failed", e);
        }
    }

    @Override
    public void setACL(DeepaMehtaObject object, AccessControlList acl) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            object.setProperty(URI_ACL, acl.toJSON().toString(), false);    // addToIndex=false
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting the ACL of object " + object.getId() + " failed", e);
        } finally {
            tx.finish();
        }
    }



    // === Workspaces ===

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



    // === Retrieval ===

    @GET
    @Path("/creator/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByCreator(@PathParam("username") String username) {
        return dms.getTopicsByProperty(URI_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByOwner(@PathParam("username") String username) {
        return dms.getTopicsByProperty(URI_OWNER, username);
    }

    @GET
    @Path("/creator/{username}/assocs")
    @Override
    public Collection<Association> getAssociationsByCreator(@PathParam("username") String username) {
        return dms.getAssociationsByProperty(URI_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/assocs")
    @Override
    public Collection<Association> getAssociationsByOwner(@PathParam("username") String username) {
        return dms.getAssociationsByProperty(URI_OWNER, username);
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void postInstall() {
        logger.info("Creating \"admin\" user account");
        Topic adminAccount = createUserAccount(new Credentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));
        // Note 1: the admin account needs to be setup for access control itself.
        // At post-install time our listeners are not yet registered. So we must setup manually here.
        // Note 2: at post-install time there is no user session. So we call setupAccessControl() directly
        // instead of (the higher-level) setupUserAccountAccessControl().
        setupAccessControl(adminAccount, DEFAULT_USER_ACCOUNT_ACL, DEFAULT_USERNAME);
        // ### TODO: setup access control for the admin account's Username and Password topics.
        // However, they are not strictly required for the moment.
    }

    @Override
    public void init() {
        logger.info("Security settings:" +
            "\n    dm4.security.read_requires_login=" + READ_REQUIRES_LOGIN +
            "\n    dm4.security.write_requires_login=" + WRITE_REQUIRES_LOGIN +
            "\n    dm4.security.subnet_filter=\""+ SUBNET_FILTER + "\"");
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
            setupAccessControlForDefaultTopicmap(defaultTopicmap);
        }
    }

    // ---

    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        if (isUserAccount(topic)) {
            setupUserAccountAccessControl(topic);
        } else {
            setupDefaultAccessControl(topic);
        }
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
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel, ClientState clientState,
                                                                                       Directives directives) {
        if (topic.getTypeUri().equals("dm4.accesscontrol.user_account")) {
            Topic usernameTopic = topic.getCompositeValue().getTopic("dm4.accesscontrol.username");
            Topic passwordTopic = topic.getCompositeValue().getTopic("dm4.accesscontrol.password");
            String newUsername = usernameTopic.getSimpleValue().toString();
            TopicModel oldUsernameTopic = oldModel.getCompositeValueModel().getTopic("dm4.accesscontrol.username",
                null);
            String oldUsername = oldUsernameTopic != null ? oldUsernameTopic.getSimpleValue().toString() : "";
            if (!newUsername.equals(oldUsername)) {
                //
                if (!oldUsername.equals("")) {
                    throw new RuntimeException("Changing a Username is not supported (tried \"" + oldUsername +
                        "\" -> \"" + newUsername + "\")");
                }
                //
                logger.info("### Username has changed from \"" + oldUsername + "\" -> \"" + newUsername +
                    "\". Setting \"" + newUsername + "\" as the new owner of 3 topics:\n" +
                    "    - User Account topic (ID " + topic.getId() + ")\n" + 
                    "    - Username topic (ID " + usernameTopic.getId() + ")\n" + 
                    "    - Password topic (ID " + passwordTopic.getId() + ")");
                setOwner(topic, newUsername);
                setOwner(usernameTopic, newUsername);
                setOwner(passwordTopic, newUsername);
            }
        }
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
    public void serviceRequestFilter(ContainerRequest containerRequest) {
        // Note: we pass the injected HttpServletRequest
        requestFilter(request);
    }

    @Override
    public void resourceRequestFilter(HttpServletRequest servletRequest) {
        // Note: for the resource filter no HttpServletRequest is injected
        requestFilter(servletRequest);
    }

    // ---

    // ### TODO: make the types cachable (like topics/associations). That is, don't deliver the permissions along
    // with the types (don't use the preSend{}Type hooks). Instead let the client request the permissions separately.

    @Override
    public void preSendTopicType(TopicType topicType, ClientState clientState) {
        // Note: the permissions for "Meta Meta Type" must be set manually.
        // This type doesn't exist in DB. Fetching its ACL entries would fail.
        if (topicType.getUri().equals("dm4.core.meta_meta_type")) {
            enrichWithPermissions(topicType, createPermissions(false, false));  // write=false, create=false
            return;
        }
        //
        enrichWithPermissions(topicType, getPermissions(topicType));
    }

    @Override
    public void preSendAssociationType(AssociationType assocType, ClientState clientState) {
        enrichWithPermissions(assocType, getPermissions(assocType));
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount(Credentials cred) {
        return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new CompositeValueModel()
            .put("dm4.accesscontrol.username", cred.username)
            .put("dm4.accesscontrol.password", cred.password)), null);  // clientState=null
    }

    private boolean isUserAccount(Topic topic) {
        String typeUri = topic.getTypeUri();
        return typeUri.equals("dm4.accesscontrol.user_account")
            || typeUri.equals("dm4.accesscontrol.username")
            || typeUri.equals("dm4.accesscontrol.password");
    }

    /**
     * Fetches the default user ("admin").
     *
     * @throws  RuntimeException    If the default user doesn't exist.
     *
     * @return  The default user (a Topic of type "Username" / <code>dm4.accesscontrol.username</code>).
     */
    private Topic fetchDefaultUser() {
        return getUsernameOrThrow(DEFAULT_USERNAME);
    }

    private Topic getUsernameOrThrow(String username) {
        Topic usernameTopic = getUsername(username);
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
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
            Set<RelatedTopic> workspaces = wsService.getAssignedWorkspaces(topic);
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

    private void setupAccessControlForDefaultTopicmap(Topic defaultTopicmap) {
        String operation = "### Setup access control for the default topicmap (\"untitled\")";
        try {
            // Note: we only check for creator assignment.
            // If an object has a creator assignment it is expected to have an ACL entry as well.
            if (getCreator(defaultTopicmap) != null) {
                logger.info(operation + " ABORTED -- already setup");
                return;
            }
            //
            logger.info(operation);
            setupAccessControl(defaultTopicmap, DEFAULT_INSTANCE_ACL, DEFAULT_USERNAME);
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
        return dms.getTopic("uri", new SimpleValue("dm4.topicmaps.default_topicmap"), false);
    }



    // === Request Filter ===

    private void requestFilter(HttpServletRequest request) {
        logger.fine("##### " + request.getMethod() + " " + request.getRequestURL() +
            "\n      ##### \"Authorization\"=\"" + request.getHeader("Authorization") + "\"" +
            "\n      ##### " + info(request.getSession(false)));    // create=false
        //
        checkRequestOrigin(request);    // throws WebApplicationException
        checkAuthorization(request);    // throws WebApplicationException
    }

    // ---

    private void checkRequestOrigin(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean allowed = JavaUtils.isInRange(remoteAddr, SUBNET_FILTER);
        //
        logger.fine("Remote address=\"" + remoteAddr + "\", dm4.security.subnet_filter=\"" + SUBNET_FILTER +
            "\" => " + (allowed ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!allowed) {
            throw403Forbidden();    // throws WebApplicationException
        }
    }

    private void checkAuthorization(HttpServletRequest request) {
        boolean authorized;
        if (request.getSession(false) != null) {    // create=false
            authorized = true;
        } else {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                // Note: if login fails we are NOT authorized, even if no login is required
                authorized = tryLogin(new Credentials(authHeader), request);
            } else {
                authorized = !isLoginRequired(request);
            }
        }
        //
        if (!authorized) {
            throw401Unauthorized(); // throws WebApplicationException
        }
    }

    // ---

    private boolean isLoginRequired(HttpServletRequest request) {
        return request.getMethod().equals("GET") ? READ_REQUIRES_LOGIN : WRITE_REQUIRES_LOGIN;
    }

    /**
     * Checks weather the credentials are valid and if so creates a session.
     *
     * @return  true if the credentials are valid.
     */
    private boolean tryLogin(Credentials cred, HttpServletRequest request) {
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
        Topic userAccount = username.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
            "dm4.accesscontrol.user_account", true, false);     // fetchComposite=true, fetchRelatingComposite=false
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

    // ---

    private void throw401Unauthorized() {
        // Note: a non-private DM installation (read_requires_login=false) utilizes DM's login dialog and must suppress
        // the browser's login dialog. To suppress the browser's login dialog a contrived authentication scheme "xBasic"
        // is used (see http://loudvchar.blogspot.ca/2010/11/avoiding-browser-popup-for-401.html)
        String authScheme = READ_REQUIRES_LOGIN ? "Basic" : "xBasic";
        throw new WebApplicationException(Response.status(Status.UNAUTHORIZED)
            .header("WWW-Authenticate", authScheme + " realm=" + AUTHENTICATION_REALM)
            .header("Content-Type", "text/html")    // for text/plain (default) Safari provides no Web Console
            .entity("You're not authorized. Sorry.")
            .build());
    }

    private void throw403Forbidden() {
        throw new WebApplicationException(Response.status(Status.FORBIDDEN)
            .header("Content-Type", "text/html")    // for text/plain (default) Safari provides no Web Console
            .entity("Access is forbidden. Sorry.")
            .build());
    }



    // === Create ACL Entries ===

    /**
     * Sets the logged in user as the creator and the owner of the specified object
     * and creates a default access control entry for it.
     *
     * If no user is logged in, nothing is performed.
     */
    private void setupDefaultAccessControl(DeepaMehtaObject object) {
        try {
            String username = getUsername();
            // Note: when no user is logged in we do NOT fallback to the default user for the access control setup.
            // This would not help in gaining data consistency because the topics/associations created so far
            // (BEFORE the Access Control plugin is activated) would still have no access control setup.
            // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
            // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
            // the other hand we don't have such a mechanism (and don't want one either).
            if (username == null) {
                logger.fine("Setting up access control for " + info(object) + " ABORTED -- no user is logged in");
                return;
            }
            //
            setupAccessControl(object, DEFAULT_INSTANCE_ACL, username);
        } catch (Exception e) {
            throw new RuntimeException("Setting up access control for " + info(object) + " failed (" + object + ")", e);
        }
    }

    private void setupDefaultAccessControl(Type type) {
        try {
            String username = getUsername();
            //
            if (username == null) {
                username = DEFAULT_USERNAME;
                setupViewConfigAccessControl(type.getViewConfig());
            }
            //
            setupAccessControl(type, DEFAULT_TYPE_ACL, username);
        } catch (Exception e) {
            throw new RuntimeException("Setting up access control for " + info(type) + " failed (" + type + ")", e);
        }
    }

    // ---

    private void setupUserAccountAccessControl(Topic topic) {
        setupAccessControl(topic, DEFAULT_USER_ACCOUNT_ACL, getUsername());
    }

    private void setupViewConfigAccessControl(ViewConfiguration viewConfig) {
        for (Topic configTopic : viewConfig.getConfigTopics()) {
            setupAccessControl(configTopic, DEFAULT_INSTANCE_ACL, DEFAULT_USERNAME);
        }
    }

    // ---

    private void setupAccessControl(DeepaMehtaObject object, AccessControlList acl, String username) {
        setCreator(object, username);
        setOwner(object, username);
        setACL(object, acl);
    }



    // === Determine Permissions ===

    private Permissions getPermissions(DeepaMehtaObject object) {
        return createPermissions(hasPermission(getUsername(), Operation.WRITE, object));
    }

    private Permissions getPermissions(Type type) {
        String username = getUsername();
        return createPermissions(hasPermission(username, Operation.WRITE, type),
                                 hasPermission(username, Operation.CREATE, type));
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
            UserRole[] userRoles = getACL(object).getUserRoles(operation);
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
        Topic usernameTopic = getUsernameOrThrow(username);
        Set<RelatedTopic> workspaces = wsService.getAssignedWorkspaces(object);
        logger.fine(info(object) + " is assigned to " + workspaces.size() + " workspaces");
        for (RelatedTopic workspace : workspaces) {
            if (wsService.isAssignedToWorkspace(usernameTopic, workspace.getId())) {
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
        String owner = getOwner(object);
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
        String creator = getCreator(object);
        logger.fine("The creator is " + userInfo(creator));
        return creator != null && creator.equals(username);
    }

    // ---

    private void enrichWithPermissions(Type type, Permissions permissions) {
        // Note: we must extend/override possibly existing permissions.
        // Consider a type update: directive UPDATE_TOPIC_TYPE is followed by UPDATE_TOPIC, both on the same object.
        CompositeValueModel typePermissions = permissions(type);
        typePermissions.put(Operation.WRITE.uri, permissions.get(Operation.WRITE.uri));
        typePermissions.put(Operation.CREATE.uri, permissions.get(Operation.CREATE.uri));
    }

    private CompositeValueModel permissions(DeepaMehtaObject object) {
        // Note 1: "dm4.accesscontrol.permissions" is a contrived URI. There is no such type definition.
        // Permissions are for transfer only, recalculated for each request, not stored in DB.
        // Note 2: The permissions topic exists only in the object's model (see note below).
        // There is no corresponding topic in the attached composite value. So we must query the model here.
        // (object.getCompositeValue().getTopic(...) would not work)
        TopicModel permissionsTopic = object.getCompositeValue().getModel()
            .getTopic("dm4.accesscontrol.permissions", null);
        CompositeValueModel permissions;
        if (permissionsTopic != null) {
            permissions = permissionsTopic.getCompositeValueModel();
        } else {
            permissions = new CompositeValueModel();
            // Note: we put the permissions topic directly in the model here (instead of the attached composite value).
            // The "permissions" topic is for transfer only. It must not be stored in the DB (as it would when putting
            // it in the attached composite value).
            object.getCompositeValue().getModel().put("dm4.accesscontrol.permissions", permissions);
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
        } else if (object instanceof AssociationType) {
            return "association type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
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

    private String info(HttpServletRequest request) {
        StringBuilder info = new StringBuilder();
        info.append("    " + request.getMethod() + " " + request.getRequestURI() + "\n");
        Enumeration<String> e1 = request.getHeaderNames();
        while (e1.hasMoreElements()) {
            String name = e1.nextElement();
            info.append("\n    " + name + ":");
            Enumeration<String> e2 = request.getHeaders(name);
            while (e2.hasMoreElements()) {
                String header = e2.nextElement();
                info.append(" " + header);
            }
        }
        return info.toString();
    }
}
