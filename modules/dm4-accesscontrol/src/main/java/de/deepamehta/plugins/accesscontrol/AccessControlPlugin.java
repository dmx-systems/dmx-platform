package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.event.PostLoginUserListener;
import de.deepamehta.plugins.accesscontrol.event.PostLogoutUserListener;
import de.deepamehta.plugins.config.ConfigCustomizer;
import de.deepamehta.plugins.config.ConfigDefinition;
import de.deepamehta.plugins.config.ConfigModificationRole;
import de.deepamehta.plugins.config.ConfigService;
import de.deepamehta.plugins.config.ConfigTarget;
import de.deepamehta.plugins.files.FilesService;
import de.deepamehta.plugins.files.event.CheckDiskQuotaListener;
import de.deepamehta.plugins.workspaces.WorkspacesService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.service.accesscontrol.Permissions;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateAssociationListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreCreateTopicListener;
import de.deepamehta.core.service.event.PreGetAssociationListener;
import de.deepamehta.core.service.event.PreGetTopicListener;
import de.deepamehta.core.service.event.PreUpdateTopicListener;
import de.deepamehta.core.service.event.ResourceRequestFilterListener;
import de.deepamehta.core.service.event.ServiceRequestFilterListener;
import de.deepamehta.core.util.JavaUtils;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;

import javax.servlet.http.HttpServletRequest;
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
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, ConfigCustomizer,
                                                                                          PreCreateTopicListener,
                                                                                          PreUpdateTopicListener,
                                                                                          PreGetTopicListener,
                                                                                          PreGetAssociationListener,
                                                                                          PostCreateTopicListener,
                                                                                          PostCreateAssociationListener,
                                                                                          PostUpdateTopicListener,
                                                                                          PostUpdateAssociationListener,
                                                                                          ServiceRequestFilterListener,
                                                                                          ResourceRequestFilterListener,
                                                                                          CheckDiskQuotaListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Security settings
    private static final boolean READ_REQUIRES_LOGIN  = Boolean.parseBoolean(
        System.getProperty("dm4.security.read_requires_login", "false"));
    private static final boolean WRITE_REQUIRES_LOGIN = Boolean.parseBoolean(
        System.getProperty("dm4.security.write_requires_login", "true"));
    private static final String SUBNET_FILTER = System.getProperty("dm4.security.subnet_filter", "127.0.0.1/32");
    private static final boolean NEW_ACCOUNTS_ARE_ENABLED = Boolean.parseBoolean(
        System.getProperty("dm4.security.new_accounts_are_enabled", "true"));
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    private static final String AUTHENTICATION_REALM = "DeepaMehta";

    // Type URIs
    private static final String LOGIN_ENABLED_TYPE = "dm4.accesscontrol.login_enabled";
    private static final String MEMBERSHIP_TYPE = "dm4.accesscontrol.membership";

    // Property URIs
    private static String PROP_CREATOR  = "dm4.accesscontrol.creator";
    private static String PROP_OWNER    = "dm4.accesscontrol.owner";
    private static String PROP_MODIFIER = "dm4.accesscontrol.modifier";

    // Events
    private static DeepaMehtaEvent POST_LOGIN_USER = new DeepaMehtaEvent(PostLoginUserListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostLoginUserListener) listener).postLoginUser(
                (String) params[0]
            );
        }
    };
    private static DeepaMehtaEvent POST_LOGOUT_USER = new DeepaMehtaEvent(PostLogoutUserListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostLogoutUserListener) listener).postLogoutUser(
                (String) params[0]
            );
        }
    };

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService wsService;

    @Inject
    private FilesService filesService;

    @Inject
    private ConfigService configService;

    @Context
    private HttpServletRequest request;

    private static Logger logger = Logger.getLogger(AccessControlPlugin.class.getName());

    static {
        logger.info("Security settings:" +
            "\n  dm4.security.read_requires_login=" + READ_REQUIRES_LOGIN +
            "\n  dm4.security.write_requires_login=" + WRITE_REQUIRES_LOGIN +
            "\n  dm4.security.subnet_filter=\"" + SUBNET_FILTER + "\"" +
            "\n  dm4.security.new_accounts_are_enabled=" + NEW_ACCOUNTS_ARE_ENABLED);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *******************************************
    // *** AccessControlService Implementation ***
    // *******************************************



    // === User Session ===

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
        _logout(request);
        //
        // For a "private" DeepaMehta installation: emulate a HTTP logout by forcing the webbrowser to bring up its
        // login dialog and to forget the former Authorization information. The user is supposed to press "Cancel".
        // The login dialog can't be used to login again.
        if (READ_REQUIRES_LOGIN) {
            throw401Unauthorized();
        }
    }

    // ---

    @GET
    @Path("/user")
    @Produces("text/plain")
    @Override
    public String getUsername() {
        return dms.getAccessControl().getUsername(request);
    }

    @GET
    @Path("/username")
    @Override
    public Topic getUsernameTopic() {
        return dms.getAccessControl().getUsernameTopic(request);
    }

    // ---

    @GET
    @Path("/user/workspace")
    @Override
    public Topic getPrivateWorkspace() {
        String username = getUsername();
        if (username == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return dms.getAccessControl().getPrivateWorkspace(username);
    }



    // === User Accounts ===

    @POST
    @Path("/user_account")
    @Transactional
    @Override
    public Topic createUserAccount(final Credentials cred) {
        try {
            final String username = cred.username;
            logger.info("Creating user account \"" + username + "\"");
            //
            // 1) create user account
            AccessControl ac = dms.getAccessControl();
            // We suppress standard workspace assignment here as a User Account topic (and its child topics) require
            // special assignments. See steps 3) and 4) below.
            Topic userAccount = ac.runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    return dms.createTopic(new TopicModel("dm4.accesscontrol.user_account", new ChildTopicsModel()
                        .put("dm4.accesscontrol.username", username)
                        .put("dm4.accesscontrol.password", cred.password)));
                }
            });
            ChildTopics childTopics = userAccount.getChildTopics();
            Topic usernameTopic = childTopics.getTopic("dm4.accesscontrol.username");
            Topic passwordTopic = childTopics.getTopic("dm4.accesscontrol.password");
            //
            // 2) create private workspace
            Topic privateWorkspace = wsService.createWorkspace(DEFAULT_PRIVATE_WORKSPACE_NAME, null,
                SharingMode.PRIVATE);
            setWorkspaceOwner(privateWorkspace, username);
            // Note: we don't set a particular creator/modifier here as we don't want suggest that the new user's
            // private workspace has been created by the new user itself. Instead we set the *current* user as the
            // creator/modifier (via postCreateTopic() listener). In case of the "admin" user account the creator/
            // modifier remain undefined as it is actually created by the system itself.
            //
            // 3) assign user account and password to private workspace
            // Note: the current user has no READ access to the private workspace just created.
            // So we must use the privileged assignToWorkspace calls here (instead of using the Workspaces service).
            long privateWorkspaceId = privateWorkspace.getId();
            ac.assignToWorkspace(userAccount, privateWorkspaceId);
            ac.assignToWorkspace(passwordTopic, privateWorkspaceId);
            //
            // 4) assign username to "System" workspace
            // Note: user <anonymous> has no READ access to the System workspace. So we must use privileged calls here.
            // This is to support the "DM4 Sign-up" 3rd-party plugin.
            long systemWorkspaceId = ac.getSystemWorkspaceId();
            ac.assignToWorkspace(usernameTopic, systemWorkspaceId);
            //
            return usernameTopic;
        } catch (Exception e) {
            throw new RuntimeException("Creating user account \"" + cred.username + "\" failed", e);
        }
    }

    @GET
    @Path("/username/{username}")
    @Override
    public Topic getUsernameTopic(@PathParam("username") String username) {
        return dms.getAccessControl().getUsernameTopic(username);
    }



    // === Workspaces / Memberships ===

    @GET
    @Path("/workspace/{workspace_id}/owner")
    @Produces("text/plain")
    @Override
    public String getWorkspaceOwner(@PathParam("workspace_id") long workspaceId) {
        // ### TODO: delegate to Core's AccessControl.getOwner()?
        return dms.hasProperty(workspaceId, PROP_OWNER) ? (String) dms.getProperty(workspaceId, PROP_OWNER) : null;
    }

    @Override
    public void setWorkspaceOwner(Topic workspace, String username) {
        try {
            workspace.setProperty(PROP_OWNER, username, true);  // addToIndex=true
        } catch (Exception e) {
            throw new RuntimeException("Setting the workspace owner of " + info(workspace) + " failed (username=" +
                username + ")", e);
        }
    }

    // ---

    @POST
    @Path("/user/{username}/workspace/{workspace_id}")
    @Transactional
    @Override
    public void createMembership(@PathParam("username") String username, @PathParam("workspace_id") long workspaceId) {
        try {
            dms.createAssociation(new AssociationModel(MEMBERSHIP_TYPE,
                new TopicRoleModel(getUsernameTopicOrThrow(username).getId(), "dm4.core.default"),
                new TopicRoleModel(workspaceId, "dm4.core.default")
            ));
        } catch (Exception e) {
            throw new RuntimeException("Creating membership for user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public boolean isMember(String username, long workspaceId) {
        return dms.getAccessControl().isMember(username, workspaceId);
    }



    // === Topicmaps ===

    @GET
    @Path("/workspace/{workspace_id}/topicmaps")
    @Override
    public ResultList<RelatedTopic> getTopicmaps(@PathParam("workspace_id") long workspaceId) {
        ResultList<RelatedTopic> topicmaps = wsService.getAssignedTopics(workspaceId, "dm4.topicmaps.topicmap");
        // filter topicmaps according to Private flag
        Iterator<RelatedTopic> i = topicmaps.iterator();
        while (i.hasNext()) {
            Topic topicmap = i.next();
            if (isTopicmapPrivate(topicmap) && !isCreator(topicmap.getId())) {
                i.remove();
            }
        }
        return topicmaps;
    }



    // === Permissions ===

    @GET
    @Path("/topic/{id}")
    @Override
    public Permissions getTopicPermissions(@PathParam("id") long topicId) {
        return getPermissions(topicId);
    }

    @GET
    @Path("/association/{id}")
    @Override
    public Permissions getAssociationPermissions(@PathParam("id") long assocId) {
        return getPermissions(assocId);
    }



    // === Object Info ===

    @GET
    @Path("/object/{id}/creator")
    @Produces("text/plain")
    @Override
    public String getCreator(@PathParam("id") long objectId) {
        return dms.hasProperty(objectId, PROP_CREATOR) ? (String) dms.getProperty(objectId, PROP_CREATOR) : null;
    }

    @GET
    @Path("/object/{id}/modifier")
    @Produces("text/plain")
    @Override
    public String getModifier(@PathParam("id") long objectId) {
        return dms.hasProperty(objectId, PROP_MODIFIER) ? (String) dms.getProperty(objectId, PROP_MODIFIER) : null;
    }



    // === Retrieval ===

    @GET
    @Path("/creator/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByCreator(@PathParam("username") String username) {
        return dms.getTopicsByProperty(PROP_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByOwner(@PathParam("username") String username) {
        return dms.getTopicsByProperty(PROP_OWNER, username);
    }

    @GET
    @Path("/creator/{username}/assocs")
    @Override
    public Collection<Association> getAssociationsByCreator(@PathParam("username") String username) {
        return dms.getAssociationsByProperty(PROP_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/assocs")
    @Override
    public Collection<Association> getAssociationsByOwner(@PathParam("username") String username) {
        return dms.getAssociationsByProperty(PROP_OWNER, username);
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void preInstall() {
        configService.registerConfigDefinition(new ConfigDefinition(
            ConfigTarget.TYPE_INSTANCES, "dm4.accesscontrol.username",
            new TopicModel(LOGIN_ENABLED_TYPE, new SimpleValue(NEW_ACCOUNTS_ARE_ENABLED)),
            ConfigModificationRole.ADMIN, this
        ));
    }

    @Override
    public void shutdown() {
        // Note 1: unregistering is crucial e.g. for redeploying the Access Control plugin. The next register call
        // (at preInstall() time) would fail as the Config service already holds such a registration.
        // Note 2: we must check if the Config service is still available. If the Config plugin is redeployed the
        // Access Control plugin is stopped/started as well but at shutdown() time the Config service is already gone.
        if (configService != null) {
            configService.unregisterConfigDefinition(LOGIN_ENABLED_TYPE);
        } else {
            logger.warning("Config service is already gone");
        }
    }



    // ****************************************
    // *** ConfigCustomizer Implementations ***
    // ****************************************



    @Override
    public TopicModel getConfigValue(Topic topic) {
        if (!topic.getTypeUri().equals("dm4.accesscontrol.username")) {
            throw new RuntimeException("Unexpected configurable topic: " + topic);
        }
        // the "admin" account must be enabled regardless of the "dm4.security.new_accounts_are_enabled" setting
        if (topic.getSimpleValue().toString().equals(ADMIN_USERNAME)) {
            return new TopicModel(LOGIN_ENABLED_TYPE, new SimpleValue(true));
        }
        // don't customize
        return null;
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preGetTopic(long topicId) {
        checkReadPermission(topicId);
    }

    @Override
    public void preGetAssociation(long assocId) {
        checkReadPermission(assocId);
        //
        long[] playerIds = dms.getPlayerIds(assocId);
        checkReadPermission(playerIds[0]);
        checkReadPermission(playerIds[1]);
    }

    // ---

    @Override
    public void preCreateTopic(TopicModel model) {
        if (model.getTypeUri().equals("dm4.accesscontrol.username")) {
            String username = model.getSimpleValue().toString();
            Topic usernameTopic = getUsernameTopic(username);
            if (usernameTopic != null) {
                throw new RuntimeException("Username \"" + username + "\" exists already");
            }
        }
    }

    @Override
    public void postCreateTopic(Topic topic) {
        String typeUri = topic.getTypeUri();
        if (typeUri.equals("dm4.workspaces.workspace")) {
            setWorkspaceOwner(topic);
        } else if (typeUri.equals("dm4.webclient.search")) {
            // ### TODO: refactoring. The Access Control module must not know about the Webclient.
            // Let the Webclient do the workspace assignment instead.
            assignSearchTopic(topic);
        }
        //
        setCreatorAndModifier(topic);
    }

    @Override
    public void postCreateAssociation(Association assoc) {
        setCreatorAndModifier(assoc);
    }

    // ---

    @Override
    public void preUpdateTopic(Topic topic, TopicModel newModel) {
        if (topic.getTypeUri().equals("dm4.accesscontrol.username")) {
            SimpleValue newUsername = newModel.getSimpleValue();
            String oldUsername = topic.getSimpleValue().toString();
            if (newUsername != null && !newUsername.toString().equals(oldUsername)) {
                throw new RuntimeException("A Username can't be changed (tried \"" + oldUsername + "\" -> \"" +
                    newUsername + "\")");
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel) {
        setModifier(topic);
    }

    @Override
    public void postUpdateAssociation(Association assoc, AssociationModel oldModel) {
        if (isMembership(assoc.getModel())) {
            if (isMembership(oldModel)) {
                // ### TODO?
            } else {
                wsService.assignToWorkspace(assoc, assoc.getTopicByType("dm4.workspaces.workspace").getId());
            }
        } else if (isMembership(oldModel)) {
            // ### TODO?
        }
        //
        setModifier(assoc);
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

    @Override
    public void checkDiskQuota(String username, long fileSize, long diskQuota) {
        if (diskQuota < 0) {
            logger.info("### Checking disk quota of " + userInfo(username) + " ABORTED -- disk quota is disabled");
            return;
        }
        //
        long occupiedSpace = getOccupiedSpace(username);
        boolean quotaOK = occupiedSpace + fileSize <= diskQuota;
        //
        logger.info("### File size: " + fileSize + " bytes, " + userInfo(username) + " occupies " + occupiedSpace +
            " bytes, disk quota: " + diskQuota + " bytes => QUOTA " + (quotaOK ? "OK" : "EXCEEDED"));
        //
        if (!quotaOK) {
            throw new RuntimeException("Disk quota of " + userInfo(username) + " exceeded. Disk quota: " +
                diskQuota + " bytes. Currently occupied: " + occupiedSpace + " bytes.");
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic getUsernameTopicOrThrow(String username) {
        Topic usernameTopic = getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("User \"" + username + "\" does not exist");
        }
        return usernameTopic;
    }

    private boolean isMembership(AssociationModel assoc) {
        return assoc.getTypeUri().equals(MEMBERSHIP_TYPE);
    }

    private void assignSearchTopic(Topic searchTopic) {
        try {
            Topic workspace;
            if (getUsername() != null) {
                workspace = getPrivateWorkspace();
            } else {
                workspace = wsService.getWorkspace(WorkspacesService.DEEPAMEHTA_WORKSPACE_URI);
            }
            wsService.assignToWorkspace(searchTopic, workspace.getId());
        } catch (Exception e) {
            throw new RuntimeException("Assigning search topic to workspace failed", e);
        }
    }

    // --- Topicmaps ---

    private boolean isTopicmapPrivate(Topic topicmap) {
        // Note: migrated topicmaps might not have a Private child topic ### TODO?
        Boolean isPrivate = topicmap.getChildTopics().getBooleanOrNull("dm4.topicmaps.private");
        return isPrivate != null ? isPrivate : false;
    }

    private boolean isCreator(long objectId) {
        String username = getUsername();
        return username != null ? username.equals(getCreator(objectId)) : false;
    }

    // --- Disk quota ---

    private long getOccupiedSpace(String username) {
        long occupiedSpace = 0;
        for (Topic fileTopic : dms.getTopics("dm4.files.file")) {
            long fileTopicId = fileTopic.getId();
            if (getCreator(fileTopicId).equals(username)) {
                occupiedSpace += filesService.getFile(fileTopicId).length();
            }
        }
        return occupiedSpace;
    }



    // === Request Filter ===

    private void requestFilter(HttpServletRequest request) {
        logger.fine("##### " + request.getMethod() + " " + request.getRequestURL() +
            "\n      ##### \"Authorization\"=\"" + request.getHeader("Authorization") + "\"" +
            "\n      ##### " + info(request.getSession(false)));    // create=false
        //
        checkRequestOrigin(request);    // throws WebApplicationException 403 Forbidden
        checkAuthorization(request);    // throws WebApplicationException 401 Unauthorized
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
            throw403Forbidden();        // throws WebApplicationException
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
            throw401Unauthorized();     // throws WebApplicationException
        }
    }

    // ---

    private boolean isLoginRequired(HttpServletRequest request) {
        return request.getMethod().equals("GET") ? READ_REQUIRES_LOGIN : WRITE_REQUIRES_LOGIN;
    }

    /**
     * Checks weather the credentials are valid and if the user account is enabled, and if both checks are positive
     * logs the user in.
     *
     * @return  true if the user has logged in.
     */
    private boolean tryLogin(Credentials cred, HttpServletRequest request) {
        String username = cred.username;
        Topic usernameTopic = checkCredentials(cred);
        if (usernameTopic != null && getLoginEnabled(usernameTopic)) {
            logger.info("##### Logging in as \"" + username + "\" => SUCCESSFUL!");
            _login(username, request);
            return true;
        } else {
            logger.info("##### Logging in as \"" + username + "\" => FAILED!");
            return false;
        }
    }

    private Topic checkCredentials(Credentials cred) {
        return dms.getAccessControl().checkCredentials(cred);
    }

    private boolean getLoginEnabled(Topic usernameTopic) {
        Topic loginEnabled = dms.getAccessControl().getConfigTopic(LOGIN_ENABLED_TYPE, usernameTopic.getId());
        return loginEnabled.getSimpleValue().booleanValue();
    }

    // ---

    private void _login(String username, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("username", username);
        logger.info("##### Creating new " + info(session));
        //
        dms.fireEvent(POST_LOGIN_USER, username);
    }

    private void _logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);    // create=false
        String username = username(session);                // save username before invalidating
        logger.info("##### Logging out from " + info(session));
        //
        session.invalidate();
        //
        dms.fireEvent(POST_LOGOUT_USER, username);
    }

    // ---

    private String username(HttpSession session) {
        return dms.getAccessControl().username(session);
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



    // === Setup Access Control ===

    /**
     * Sets the logged in user as the creator/modifier of the given object.
     * <p>
     * If no user is logged in, nothing is performed.
     */
    private void setCreatorAndModifier(DeepaMehtaObject object) {
        try {
            String username = getUsername();
            // Note: when no user is logged in we do NOT fallback to the default user for the access control setup.
            // This would not help in gaining data consistency because the topics/associations created so far
            // (BEFORE the Access Control plugin is activated) would still have no access control setup.
            // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
            // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
            // the other hand we don't have such a mechanism (and don't want one either).
            if (username == null) {
                logger.fine("Setting the creator/modifier of " + info(object) + " ABORTED -- no user is logged in");
                return;
            }
            //
            setCreatorAndModifier(object, username);
        } catch (Exception e) {
            throw new RuntimeException("Setting the creator/modifier of " + info(object) + " failed", e);
        }
    }

    /**
     * @param   username    must not be null.
     */
    private void setCreatorAndModifier(DeepaMehtaObject object, String username) {
        setCreator(object, username);
        setModifier(object, username);
    }

    // ---

    /**
     * Sets the creator of a topic or an association.
     */
    private void setCreator(DeepaMehtaObject object, String username) {
        try {
            object.setProperty(PROP_CREATOR, username, true);   // addToIndex=true
        } catch (Exception e) {
            throw new RuntimeException("Setting the creator of " + info(object) + " failed (username=" + username + ")",
                e);
        }
    }

    // ---

    private void setModifier(DeepaMehtaObject object) {
        String username = getUsername();
        // Note: when a plugin topic is updated there is no user logged in yet.
        if (username == null) {
            return;
        }
        //
        setModifier(object, username);
    }

    private void setModifier(DeepaMehtaObject object, String username) {
        object.setProperty(PROP_MODIFIER, username, false);     // addToIndex=false
    }

    // ---

    private void setWorkspaceOwner(Topic workspace) {
        String username = getUsername();
        // Note: username is null if the Access Control plugin is activated already
        // when a 3rd-party plugin creates a workspace at install-time.
        if (username == null) {
            return;
        }
        //
        setWorkspaceOwner(workspace, username);
    }



    // === Calculate Permissions ===

    /**
     * @param   objectId    a topic ID, or an association ID
     */
    private void checkReadPermission(long objectId) {
        if (!inRequestScope()) {
            logger.fine("### Object " + objectId + " is accessed by \"System\" -- READ permission is granted");
            return;
        }
        //
        String username = getUsername();
        if (!hasPermission(username, Operation.READ, objectId)) {
            throw new AccessControlException(userInfo(username) + " has no READ permission for object " + objectId);
        }
    }

    /**
     * @param   objectId    a topic ID, or an association ID.
     */
    private Permissions getPermissions(long objectId) {
        return new Permissions().add(Operation.WRITE, hasPermission(getUsername(), Operation.WRITE, objectId));
    }

    /**
     * Checks if a user is permitted to perform an operation on an object (topic or association).
     *
     * @param   username    the logged in user, or <code>null</code> if no user is logged in.
     * @param   objectId    a topic ID, or an association ID.
     *
     * @return  <code>true</code> if permission is granted, <code>false</code> otherwise.
     */
    private boolean hasPermission(String username, Operation operation, long objectId) {
        return dms.getAccessControl().hasPermission(username, operation, objectId);
    }

    private boolean inRequestScope() {
        try {
            request.getMethod();
            return true;
        } catch (IllegalStateException e) {
            // Note: this happens if a request method is called outside request scope.
            // This is the case while system startup.
            return false;
        } catch (NullPointerException e) {
            // While system startup request might be null.
            // Jersey might not have injected the proxy object yet.
            return false;
        }
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
