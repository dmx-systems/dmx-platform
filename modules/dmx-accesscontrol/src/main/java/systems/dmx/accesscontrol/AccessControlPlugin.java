package systems.dmx.accesscontrol;

import static systems.dmx.accesscontrol.Constants.*;
import systems.dmx.accesscontrol.event.PostLoginUser;
import systems.dmx.accesscontrol.event.PostLogoutUser;
import systems.dmx.config.ConfigCustomizer;
import systems.dmx.config.ConfigDefinition;
import systems.dmx.config.ConfigModificationRole;
import systems.dmx.config.ConfigService;
import systems.dmx.config.ConfigTarget;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocPlayerModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.accesscontrol.AccessControlException;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.Operation;
import systems.dmx.core.service.accesscontrol.Permissions;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.service.accesscontrol.SharingMode;
import systems.dmx.core.service.event.CheckAssocReadAccess;
import systems.dmx.core.service.event.CheckAssocWriteAccess;
import systems.dmx.core.service.event.CheckTopicReadAccess;
import systems.dmx.core.service.event.CheckTopicWriteAccess;
import systems.dmx.core.service.event.PostCreateAssoc;
import systems.dmx.core.service.event.PostCreateTopic;
import systems.dmx.core.service.event.PostUpdateAssoc;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.service.event.PreUpdateTopic;
import systems.dmx.core.service.event.ServiceRequestFilter;
import systems.dmx.core.service.event.StaticResourceFilter;
import systems.dmx.core.util.JavaUtils;
import systems.dmx.files.FilesService;
import systems.dmx.files.event.CheckDiskQuota;
import static systems.dmx.workspaces.Constants.*;
import systems.dmx.workspaces.WorkspacesService;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



@Path("/accesscontrol")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, ConfigCustomizer,
                                                                                          CheckTopicReadAccess,
                                                                                          CheckTopicWriteAccess,
                                                                                          CheckAssocReadAccess,
                                                                                          CheckAssocWriteAccess,
                                                                                          PreUpdateTopic,
                                                                                          PostCreateTopic,
                                                                                          PostCreateAssoc,
                                                                                          PostUpdateTopic,
                                                                                          PostUpdateAssoc,
                                                                                          ServiceRequestFilter,
                                                                                          StaticResourceFilter,
                                                                                          CheckDiskQuota {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Security settings
    private static final String ANONYMOUS_READ_ALLOWED = System.getProperty("dmx.security.anonymous_read_allowed",
        "ALL");
    private static final String ANONYMOUS_WRITE_ALLOWED = System.getProperty("dmx.security.anonymous_write_allowed",
        "NONE");
    private static final AnonymousAccessFilter accessFilter = new AnonymousAccessFilter(ANONYMOUS_READ_ALLOWED,
        ANONYMOUS_WRITE_ALLOWED);
    private static final String SUBNET_FILTER = System.getProperty("dmx.security.subnet_filter", "127.0.0.1/32");
    private static final boolean NEW_ACCOUNTS_ARE_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.security.new_accounts_are_enabled", "true"));
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.
    private static final boolean IS_PUBLIC_INSTALLATION = ANONYMOUS_READ_ALLOWED.equals("ALL");

    private static final String AUTHENTICATION_REALM = "DMX";

    // ### TODO: copy in Credentials.java
    private static final String ENCODED_PASSWORD_PREFIX = "-SHA256-";

    // Property URIs
    private static final String PROP_CREATOR  = "dmx.accesscontrol.creator";
    private static final String PROP_OWNER    = "dmx.accesscontrol.owner";
    private static final String PROP_MODIFIER = "dmx.accesscontrol.modifier";

    // Events
    private static DMXEvent POST_LOGIN_USER = new DMXEvent(PostLoginUser.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostLoginUser) listener).postLoginUser(
                (String) params[0]
            );
        }
    };
    private static DMXEvent POST_LOGOUT_USER = new DMXEvent(PostLogoutUser.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostLogoutUser) listener).postLogoutUser(
                (String) params[0]
            );
        }
    };

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private WorkspacesService wsService;
    @Inject private FilesService filesService;
    @Inject private ConfigService configService;

    @Context
    private HttpServletRequest request;

    private Map<String, AuthorizationMethod> authorizationMethods = new HashMap();

    private static Logger logger = Logger.getLogger(AccessControlPlugin.class.getName());

    static {
        logger.info("Security settings:" +
            "\n  dmx.security.anonymous_read_allowed = " + accessFilter.dumpReadSetting() +
            "\n  dmx.security.anonymous_write_allowed = " + accessFilter.dumpWriteSetting() +
            "\n  dmx.security.subnet_filter = " + SUBNET_FILTER +
            "\n  dmx.security.new_accounts_are_enabled = " + NEW_ACCOUNTS_ARE_ENABLED);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************
    // *** AccessControlService ***
    // ****************************



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
        // For a "private" DMX installation: emulate a HTTP logout by forcing the webbrowser to bring up its
        // login dialog and to forget the former Authorization information. The user is supposed to press "Cancel".
        // The login dialog can't be used to login again.
        if (!IS_PUBLIC_INSTALLATION) {
            throw401Unauthorized(true);     // showBrowserLoginDialog=true
        }
    }

    // ---

    @GET
    @Path("/user")
    @Produces("text/plain")
    @Override
    public String getUsername() {
        return dmx.getPrivilegedAccess().getUsername(request);
    }

    @GET
    @Path("/username")
    @Override
    public Topic getUsernameTopic() {
        return dmx.getPrivilegedAccess().getUsernameTopic(request);
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
        return dmx.getPrivilegedAccess().getPrivateWorkspace(username);
    }



    // === User Accounts ===

    @POST
    @Path("/user_account")
    @Transactional
    @Override
    public Topic createUserAccount(final Credentials cred) {
        try {
            String username = cred.username;
            PrivilegedAccess pa = dmx.getPrivilegedAccess();
            logger.info("Creating user account \"" + username + "\"");
            //
            // 1) create Username topic and private workspace
            final Topic usernameTopic = createUsername(username);
            //
            // 2) create User Account
            // We suppress standard workspace assignment here as a User Account topic (and its child topics) require
            // special assignments. See step 3) below.
            Topic userAccount = pa.runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    return dmx.createTopic(mf.newTopicModel(USER_ACCOUNT, mf.newChildTopicsModel()
                        .setRef(USERNAME, usernameTopic.getId())
                        .set(PASSWORD, cred.password)));
                }
            });
            // 3) assign user account and password to private workspace
            // Note: the current user has no READ access to the private workspace just created.
            // So we must use the privileged assignToWorkspace calls here (instead of using the Workspaces service).
            Topic passwordTopic = userAccount.getChildTopics().getTopic(PASSWORD);
            long privateWorkspaceId = pa.getPrivateWorkspace(username).getId();
            pa.assignToWorkspace(userAccount, privateWorkspaceId);
            pa.assignToWorkspace(passwordTopic, privateWorkspaceId);
            //
            return usernameTopic;
        } catch (Exception e) {
            throw new RuntimeException("Creating user account \"" + cred.username + "\" failed", e);
        }
    }

    @Override
    public Topic createUsername(final String username) {
        try {
            logger.info("Creating username topic \"" + username + "\"");
            PrivilegedAccess pa = dmx.getPrivilegedAccess();
            //
            // 1) check username uniqueness
            // Note: we can't do this check in the preCreateTopic() listener. If such an username topic exists already
            // the DM5 value integrator will reuse this one instead of trying to create a new one. The preCreateTopic()
            // listener will not trigger.
            Topic usernameTopic = getUsernameTopic(username);
            if (usernameTopic != null) {
                throw new RuntimeException("Username \"" + username + "\" exists already");
            }
            // 2) create username topic
            // We suppress standard workspace assignment here as a username topic require special assignment.
            // See step 3) below.
            usernameTopic = pa.runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    return dmx.createTopic(mf.newTopicModel(USERNAME, new SimpleValue(username)));
                }
            });
            // 3) create private workspace
            setWorkspaceOwner(
                wsService.createWorkspace(DEFAULT_PRIVATE_WORKSPACE_NAME, null, SharingMode.PRIVATE),
                username
            );
            // Note: we don't set a particular creator/modifier here as we don't want suggest that the new user's
            // private workspace has been created by the new user itself. Instead we set the *current* user as the
            // creator/modifier (via postCreateTopic() listener). In case of the "admin" user account the creator/
            // modifier remain undefined as it is actually created by the system itself.
            //
            // 4) assign username topic to "System" workspace
            // Note: user <anonymous> has no READ access to the System workspace. So we must use privileged calls here.
            // This is to support the "DM4 Sign-up" 3rd-party plugin.
            pa.assignToWorkspace(usernameTopic, pa.getSystemWorkspaceId());
            //
            return usernameTopic;
        } catch (Exception e) {
            throw new RuntimeException("Creating username topic \"" + username + "\" failed", e);
        }
    }

    @GET
    @Path("/username/{username}")
    @Override
    public Topic getUsernameTopic(@PathParam("username") String username) {
        return dmx.getPrivilegedAccess().getUsernameTopic(username);
    }



    // === Workspaces / Memberships ===

    @GET
    @Path("/workspace/{workspace_id}/owner")
    @Produces("text/plain")
    @Override
    public String getWorkspaceOwner(@PathParam("workspace_id") long workspaceId) {
        // ### TODO: delegate to Core's PrivilegedAccess.getOwner()?
        return dmx.hasProperty(workspaceId, PROP_OWNER) ? (String) dmx.getProperty(workspaceId, PROP_OWNER) : null;
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
            Assoc assoc = dmx.createAssoc(mf.newAssocModel(MEMBERSHIP,
                mf.newTopicPlayerModel(getUsernameTopicOrThrow(username).getId(), DEFAULT),
                mf.newTopicPlayerModel(workspaceId, DEFAULT)
            ));
            assignMembershipToWorkspace(assoc);
        } catch (Exception e) {
            throw new RuntimeException("Creating membership for user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @Override
    public boolean isMember(String username, long workspaceId) {
        return dmx.getPrivilegedAccess().isMember(username, workspaceId);
    }

    // ---

    @GET
    @Path("/workspace/admin/id")
    @Override
    public long getAdminWorkspaceId() {
        return dmx.getPrivilegedAccess().getAdministrationWorkspaceId();
    }


    // === Permissions ===

    @GET
    @Path("/object/{id}")
    @Override
    public Permissions getPermissions(@PathParam("id") long objectId) {
        return new Permissions().add(Operation.WRITE, hasPermission(getUsername(), Operation.WRITE, objectId));
    }



    // === Object Info ===

    @GET
    @Path("/object/{id}/creator")
    @Produces("text/plain")
    @Override
    public String getCreator(@PathParam("id") long objectId) {
        return dmx.getPrivilegedAccess().getCreator(objectId);
    }

    @GET
    @Path("/object/{id}/modifier")
    @Produces("text/plain")
    @Override
    public String getModifier(@PathParam("id") long objectId) {
        return dmx.hasProperty(objectId, PROP_MODIFIER) ? (String) dmx.getProperty(objectId, PROP_MODIFIER) : null;
    }



    // === Retrieval ===

    @GET
    @Path("/creator/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByCreator(@PathParam("username") String username) {
        return dmx.getTopicsByProperty(PROP_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByOwner(@PathParam("username") String username) {
        return dmx.getTopicsByProperty(PROP_OWNER, username);
    }

    @GET
    @Path("/creator/{username}/assocs")
    @Override
    public Collection<Assoc> getAssocsByCreator(@PathParam("username") String username) {
        return dmx.getAssocsByProperty(PROP_CREATOR, username);
    }

    @GET
    @Path("/owner/{username}/assocs")
    @Override
    public Collection<Assoc> getAssocsByOwner(@PathParam("username") String username) {
        return dmx.getAssocsByProperty(PROP_OWNER, username);
    }



    // === Authorization Methods ===

    @GET
    @Path("/methods")
    @Override
    public Set<String> getAuthorizationMethods() {
        return authorizationMethods.keySet();
    }

    @Override
    public void registerAuthorizationMethod(String name, AuthorizationMethod am) {
        if (authorizationMethods.containsKey(name)) {
            throw new RuntimeException("Authorization method \"" + name + "\" already registered");
        }
        logger.info("Registering authorization method \"" + name + "\"");
        authorizationMethods.put(name, am);
    }

    @Override
    public void unregisterAuthorizationMethod(String name) {
        logger.info("Unregistering authorization method \"" + name + "\"");
        authorizationMethods.remove(name);
    }



    // *************
    // *** Hooks ***
    // *************



    @Override
    public void preInstall() {
        configService.registerConfigDefinition(new ConfigDefinition(
            ConfigTarget.TYPE_INSTANCES, USERNAME,
            mf.newTopicModel(LOGIN_ENABLED, new SimpleValue(NEW_ACCOUNTS_ARE_ENABLED)),
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
            configService.unregisterConfigDefinition(LOGIN_ENABLED);
        } else {
            logger.warning("Config service is already gone");
        }
    }



    // ************************
    // *** ConfigCustomizer ***
    // ************************



    @Override
    public TopicModel getConfigValue(Topic topic) {
        if (!topic.getTypeUri().equals(USERNAME)) {
            throw new RuntimeException("Unexpected configurable topic: " + topic);
        }
        // the "admin" account must be enabled regardless of the "dmx.security.new_accounts_are_enabled" setting
        if (topic.getSimpleValue().toString().equals(ADMIN_USERNAME)) {
            return mf.newTopicModel(LOGIN_ENABLED, new SimpleValue(true));
        }
        // don't customize
        return null;
    }



    // *****************
    // *** Listeners ***
    // *****************



    @Override
    public void checkTopicReadAccess(long topicId) {
        checkReadAccess(topicId);
    }

    @Override
    public void checkTopicWriteAccess(long topicId) {
        checkWriteAccess(topicId);
    }

    // ---

    @Override
    public void checkAssocReadAccess(long assocId) {
        checkReadAccess(assocId);
        //
        List<PlayerModel> players = dmx.getPlayerModels(assocId);
        checkReadAccess(players.get(0));
        checkReadAccess(players.get(1));
    }

    @Override
    public void checkAssocWriteAccess(long assocId) {
        checkWriteAccess(assocId);
    }

    // ---

    @Override
    public void postCreateTopic(Topic topic) {
        if (topic.getTypeUri().equals(WORKSPACE)) {
            setWorkspaceOwner(topic);
        }
        setCreatorAndModifier(topic);
    }

    @Override
    public void postCreateAssoc(Assoc assoc) {
        setCreatorAndModifier(assoc);
    }

    // ---

    @Override
    public void preUpdateTopic(Topic topic, TopicModel updateModel) {
        if (topic.getTypeUri().equals(USERNAME)) {
            SimpleValue newUsername = updateModel.getSimpleValue();
            String oldUsername = topic.getSimpleValue().toString();
            if (newUsername != null && !newUsername.toString().equals(oldUsername)) {
                throw new RuntimeException("A Username can't be changed (tried \"" + oldUsername + "\" -> \"" +
                    newUsername + "\")");
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals(USER_ACCOUNT)) {
            // encode password
            RelatedTopic passwordTopic = topic.getChildTopics().getTopic(PASSWORD);
            String password = passwordTopic.getSimpleValue().toString();
            passwordTopic.setSimpleValue(encodePassword(password));
            // reassign workspace
            long workspaceId = getPrivateWorkspace().getId();
            wsService.assignToWorkspace(passwordTopic, workspaceId);
            wsService.assignToWorkspace(passwordTopic.getRelatingAssoc(), workspaceId);
        }
        //
        setModifier(topic);
    }

    @Override
    public void postUpdateAssoc(Assoc assoc, AssocModel updateModel, AssocModel oldAssoc) {
        if (isMembership(assoc.getModel()) && !isMembership(oldAssoc)) {
            assignMembershipToWorkspace(assoc);
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
    public void staticResourceFilter(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        // Note: for the resource filter no HttpServletRequest is injected
        requestFilter(servletRequest);
    }

    // ---

    @Override
    public void checkDiskQuota(String username, long fileSize, long diskQuota) {
        if (diskQuota < 0) {
            logger.info("### Checking disk quota of " + userInfo(username) + " SKIPPED -- disk quota is disabled");
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

    // ### TODO: copy in Credentials.java
    private String encodePassword(String password) {
        return ENCODED_PASSWORD_PREFIX + JavaUtils.encodeSHA256(password);
    }

    private boolean isMembership(AssocModel assoc) {
        return assoc.getTypeUri().equals(MEMBERSHIP);
    }

    /**
     * Assigns a Membership to its workspace player.
     *
     * @throws  RuntimeException    if the given assoc has no workspace player
     */
    private void assignMembershipToWorkspace(Assoc assoc) {
        try {
            DMXObject workspace = assoc.getDMXObjectByType(WORKSPACE);
            if (workspace == null) {
                throw new RuntimeException("Assoc " + assoc.getId() + " has no workspace player");
            }
            wsService.assignToWorkspace(assoc, workspace.getId());
        } catch (Exception e) {
            throw new RuntimeException("Assigning membership " + assoc.getId() + " to a workspace failed", e);
        }
    }

    // --- Disk quota ---

    private long getOccupiedSpace(String username) {
        long occupiedSpace = 0;
        for (Topic fileTopic : dmx.getTopicsByType("dmx.files.file")) {
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
        // 1) apply subnet filter
        checkRequestOrigin(request);        // throws WebApplicationException 403 Forbidden
        // 2) create session (if not yet created)
        HttpSession session = request.getSession();
        // 3) check authorization (if not yet logged in)
        if (username(session) == null) {
            checkAuthorization(request);    // throws WebApplicationException 401 Unauthorized
        }
    }

    // ---

    private void checkRequestOrigin(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean allowed = JavaUtils.isInRange(remoteAddr, SUBNET_FILTER);
        //
        logger.fine("Remote address=\"" + remoteAddr + "\", dmx.security.subnet_filter=\"" + SUBNET_FILTER +
            "\" => " + (allowed ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!allowed) {
            throw403Forbidden();        // throws WebApplicationException
        }
    }

    private void checkAuthorization(HttpServletRequest request) {
        boolean authorized;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            Credentials cred = new Credentials(authHeader);
            AuthorizationMethod am = getAuthorizationMethod(cred);
            // Note: if login fails we are NOT authorized, even if no login is required
            authorized = tryLogin(cred, am, request);
        } else {
            authorized = accessFilter.isAnonymousAccessAllowed(request);
        }
        if (!authorized) {
            // Note: a non-public DM installation (anonymous_read_allowed != "ALL") utilizes the browser's login dialog.
            // (In contrast a public DM installation utilizes DM's login dialog and must suppress the browser's login
            // dialog.)
            throw401Unauthorized(!IS_PUBLIC_INSTALLATION);  // throws WebApplicationException
        }
    }

    private AuthorizationMethod getAuthorizationMethod(Credentials cred) {
        AuthorizationMethod am = null;
        if (!cred.methodName.equals("Basic")) {
            logger.info("authMethodName: \"" + cred.methodName + "\"");
            am = getAuthorizationMethod(cred.methodName);
        }
        return am;
    }

    private AuthorizationMethod getAuthorizationMethod(String name) {
        AuthorizationMethod am = authorizationMethods.get(name);
        if (am == null) {
            throw new RuntimeException("Authorization method \"" + name + "\" is unknown");
        }
        return am;
    }

    // ---

    /**
     * Checks whether the credentials are valid and if the user account is enabled, and if both checks are positive
     * logs the user in.
     *
     * @return  true if the user has logged in.
     */
    private boolean tryLogin(Credentials cred, AuthorizationMethod am, HttpServletRequest request) {
        String username = cred.username;
        Topic usernameTopic = checkCredentials(cred, am);
        if (usernameTopic != null && getLoginEnabled(usernameTopic)) {
            logger.info("##### Logging in as \"" + username + "\" => SUCCESSFUL!");
            _login(username, request);
            return true;
        } else {
            logger.info("##### Logging in as \"" + username + "\" => FAILED!");
            return false;
        }
    }

    private Topic checkCredentials(Credentials cred, AuthorizationMethod am) {
        if (am == null) {
            return dmx.getPrivilegedAccess().checkCredentials(cred);
        } else {
            return am.checkCredentials(cred);
        }
    }

    private boolean getLoginEnabled(Topic usernameTopic) {
        Topic loginEnabled = dmx.getPrivilegedAccess().getConfigTopic(LOGIN_ENABLED, usernameTopic.getId());
        return loginEnabled.getSimpleValue().booleanValue();
    }

    // ---

    private void _login(String username, HttpServletRequest request) {
        request.getSession(false).setAttribute("username", username);   // create=false
        dmx.fireEvent(POST_LOGIN_USER, username);
    }

    private void _logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);    // create=false
        String username = username(session);                // save username before removing
        logger.info("##### Logging out from " + info(session));
        // Note: the session is not invalidated. Just the "username" attribute is removed.
        session.removeAttribute("username");
        dmx.fireEvent(POST_LOGOUT_USER, username);
    }

    // ---

    private String username(HttpSession session) {
        return dmx.getPrivilegedAccess().username(session);
    }

    // ---

    private void throw401Unauthorized(boolean showBrowserLoginDialog) {
        // Note: to suppress the browser's login dialog a contrived authentication scheme "xBasic"
        // is used (see http://loudvchar.blogspot.ca/2010/11/avoiding-browser-popup-for-401.html)
        String authScheme = showBrowserLoginDialog ? "Basic" : "xBasic";
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
    private void setCreatorAndModifier(DMXObject object) {
        try {
            String username = getUsername();
            // Note: when no user is logged in we do NOT fallback to the default user for the access control setup.
            // This would not help in gaining data consistency because the topics/associations created so far
            // (BEFORE the Access Control plugin is activated) would still have no access control setup.
            // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
            // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
            // the other hand we don't have such a mechanism (and don't want one either).
            if (username == null) {
                logger.fine("Setting the creator/modifier of " + info(object) + " SKIPPED -- no user is logged in");
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
    private void setCreatorAndModifier(DMXObject object, String username) {
        setCreator(object, username);
        setModifier(object, username);
    }

    // ---

    /**
     * Sets the creator of a topic or an association.
     */
    private void setCreator(DMXObject object, String username) {
        try {
            object.setProperty(PROP_CREATOR, username, true);   // addToIndex=true
        } catch (Exception e) {
            throw new RuntimeException("Setting the creator of " + info(object) + " failed (username=" + username + ")",
                e);
        }
    }

    // ---

    private void setModifier(DMXObject object) {
        String username = getUsername();
        // Note: when a plugin topic is updated there is no user logged in yet.
        if (username == null) {
            return;
        }
        //
        setModifier(object, username);
    }

    private void setModifier(DMXObject object, String username) {
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

    private void checkReadAccess(PlayerModel player) {
        long id = player.getId();
        if (player instanceof AssocPlayerModel) {
            checkAssocReadAccess(id);     // recursion
        } else {
            checkReadAccess(id);
        }
    }

    /**
     * @param   objectId    a topic ID, or an association ID
     */
    private void checkReadAccess(long objectId) {
        checkAccess(Operation.READ, objectId);
    }

    /**
     * @param   objectId    a topic ID, or an association ID
     */
    private void checkWriteAccess(long objectId) {
        checkAccess(Operation.WRITE, objectId);
    }

    // ---

    /**
     * @param   objectId    a topic ID, or an association ID
     */
    private void checkAccess(Operation operation, long objectId) {
        if (!inRequestScope()) {
            logger.fine("### Object " + objectId + " is accessed by \"System\" -- " + operation +
                " permission is granted");
            return;
        }
        //
        String username = getUsername();
        if (!hasPermission(username, operation, objectId)) {
            throw new AccessControlException(userInfo(username) + " has no " + operation + " permission for object " +
                objectId);
        }
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
        return dmx.getPrivilegedAccess().hasPermission(username, operation, objectId);
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

    private String info(DMXObject object) {
        if (object instanceof TopicType) {
            return "topic type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof AssocType) {
            return "association type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof Topic) {
            return "topic " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\", uri=\"" + object.getUri() +
                "\")";
        } else if (object instanceof Assoc) {
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
