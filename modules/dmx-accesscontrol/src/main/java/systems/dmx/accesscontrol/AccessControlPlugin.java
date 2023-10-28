package systems.dmx.accesscontrol;

import static systems.dmx.accesscontrol.Constants.*;
import systems.dmx.accesscontrol.event.PostLoginUser;
import systems.dmx.accesscontrol.event.PostLogoutUser;
import systems.dmx.config.ConfigCustomizer;
import systems.dmx.config.ConfigDef;
import systems.dmx.config.ConfigModRole;
import systems.dmx.config.ConfigService;
import systems.dmx.config.ConfigTarget;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocPlayerModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.ChangeReport;
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
import systems.dmx.core.service.event.PostDeleteTopic;
import systems.dmx.core.service.event.PostUpdateAssoc;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.service.event.PreCreateAssoc;
import systems.dmx.core.service.event.PreUpdateTopic;
import systems.dmx.core.service.event.ServiceRequestFilter;
import systems.dmx.core.service.event.StaticResourceFilter;
import systems.dmx.core.util.DMXUtils;
import systems.dmx.core.util.IdList;
import systems.dmx.core.util.JavaUtils;
import static systems.dmx.files.Constants.*;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



@Path("/access-control")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, ConfigCustomizer,
                                                                                          CheckTopicReadAccess,
                                                                                          CheckTopicWriteAccess,
                                                                                          CheckAssocReadAccess,
                                                                                          CheckAssocWriteAccess,
                                                                                          PreCreateAssoc,
                                                                                          PreUpdateTopic,
                                                                                          PostCreateTopic,
                                                                                          PostCreateAssoc,
                                                                                          PostDeleteTopic,
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
        System.getProperty("dmx.security.new_accounts_are_enabled", "true")
    );
    private static final String SITE_SALT = System.getProperty("dmx.security.site_salt", "");
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.
    private static final boolean IS_PUBLIC_INSTALLATION = ANONYMOUS_READ_ALLOWED.equals("ALL");

    private static final String AUTHENTICATION_REALM = "DMX";

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

    @Inject private WorkspacesService ws;
    @Inject private FilesService fs;
    @Inject private ConfigService cs;

    @Context private HttpServletRequest request;
    @Context private HttpServletResponse response;

    private Map<String, AuthorizationMethod> authorizationMethods = new HashMap();

    private static Logger logger = Logger.getLogger(AccessControlPlugin.class.getName());

    static {
        logger.info("Security config:" +
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

    @Override
    public void checkAdmin() {
        try {
            checkWriteAccess(getAdminWorkspaceId());
        } catch (Exception e) {
            throw new RuntimeException("User is not an administrator", e);
        }
    }



    // === User Accounts ===

    @POST
    @Path("/user-account")
    @Transactional
    @Override
    public Topic createUserAccount(Credentials cred) {
        try {
            checkAdmin();
            return _createUserAccount(cred);
        } catch (Exception e) {
            throw new RuntimeException("Creating user account \"" + cred.username + "\" failed", e);
        }
    }

    @Override
    public Topic _createUserAccount(final Credentials cred) throws Exception {
        String username = cred.username;
        PrivilegedAccess pa = dmx.getPrivilegedAccess();
        logger.info("Creating user account \"" + username + "\"");
        //
        // 1) create Username topic and private workspace
        final Topic usernameTopic = createUsername(username);
        //
        // 2) create User Account
        // Note: a User Account topic (and its child topics) requires special workspace assignments (see next step 3).
        // So we suppress standard workspace assignment. (We can't set the actual workspace here as privileged
        // "assignToWorkspace" calls are required.)
        String salt = JavaUtils.random256();
        Topic userAccount = pa.runInWorkspaceContext(-1, () ->
            dmx.createTopic(mf.newTopicModel(USER_ACCOUNT, mf.newChildTopicsModel()
                .setRef(USERNAME, usernameTopic.getId())
                .set(PASSWORD, JavaUtils.encodeSHA256(SITE_SALT + salt + cred.password))))
        );
        logger.info("### Salting password of user \"" + cred.username + "\"");
        RelatedTopic passwordTopic = userAccount.getChildTopics().getTopic(PASSWORD);
        passwordTopic.setProperty(SALT, salt, false);     // addToIndex=false
        // 3) assign user account and password to private workspace
        // Note: the current user has no READ access to the private workspace just created.
        // Privileged assignToWorkspace() calls are required (instead of using the Workspaces service).
        long privateWorkspaceId = pa.getPrivateWorkspace(username).getId();
        pa.assignToWorkspace(userAccount, privateWorkspaceId);
        pa.assignToWorkspace(passwordTopic, privateWorkspaceId);
        pa.assignToWorkspace(passwordTopic.getRelatingAssoc(), privateWorkspaceId);
        //
        return usernameTopic;
    }

    @Override
    public Topic createUsername(final String username) {
        try {
            logger.info("Creating username topic \"" + username + "\"");
            PrivilegedAccess pa = dmx.getPrivilegedAccess();
            //
            // 1) check username uniqueness
            // Note: we can't do this check in the preCreateTopic() listener. If such an username topic exists already
            // the DMX value integrator will reuse this one instead of trying to create a new one. The preCreateTopic()
            // listener will not trigger.
            Topic usernameTopic = getUsernameTopic(username);
            if (usernameTopic != null) {
                throw new RuntimeException("Username \"" + username + "\" exists already");
            }
            // 2) create Username topic
            // Note: a Username topic requires special workspace assignment (see step 4 below).
            // So we suppress standard workspace assignment. (We can't set the actual workspace here as privileged
            // "assignToWorkspace" calls are required.)
            usernameTopic = pa.runInWorkspaceContext(-1, () ->
                dmx.createTopic(mf.newTopicModel(USERNAME, new SimpleValue(username)))
            );
            // 3) create private workspace
            setWorkspaceOwner(
                ws.createWorkspace(DEFAULT_PRIVATE_WORKSPACE_NAME, null, SharingMode.PRIVATE), username
            );
            // Note: we don't set a particular creator/modifier here as we don't want suggest that the new user's
            // private workspace has been created by the new user itself. Instead we set the *current* user as the
            // creator/modifier (via postCreateTopic() listener). In case of the "admin" user account the creator/
            // modifier remain undefined as it is actually created by the system itself.
            //
            // 4) assign username topic to "System" workspace
            // Note: user <anonymous> has no READ access to the System workspace. So we must use privileged calls here.
            // This is to support the "DMX Sign-up" 3rd-party plugin.
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
    @Path("/workspace/{workspaceId}/owner")
    @Produces("text/plain")
    @Override
    public String getWorkspaceOwner(@PathParam("workspaceId") long workspaceId) {
        // ### TODO: delegate to Core's PrivilegedAccess.getOwner()?
        return dmx.hasProperty(workspaceId, OWNER) ? (String) dmx.getProperty(workspaceId, OWNER) : null;
    }

    @Override
    public void setWorkspaceOwner(Topic workspace, String username) {
        try {
            workspace.setProperty(OWNER, username, true);  // addToIndex=true
        } catch (Exception e) {
            throw new RuntimeException("Setting the workspace owner of " + info(workspace) + " failed (username=" +
                username + ")", e);
        }
    }

    @Override
    public void enrichWithOwnerInfo(Topic workspace) {
        workspace.getChildTopics().getModel().set(OWNER, getWorkspaceOwner(workspace.getId()));
    }

    // ---

    @GET
    @Path("/user/{username}/memberships")
    @Override
    public List<RelatedTopic> getMemberships(@PathParam("username") String username) {
        try {
            return getUsernameTopic(username).getRelatedTopics(MEMBERSHIP, DEFAULT, DEFAULT, WORKSPACE);
        } catch (Exception e) {
            throw new RuntimeException("Getting memberships of user \"" + username + "\" failed", e);
        }
    }

    @GET
    @Path("/workspace/{workspaceId}/memberships")
    @Override
    public List<RelatedTopic> getMemberships(@PathParam("workspaceId") long workspaceId) {
        try {
            return dmx.getTopic(workspaceId).getRelatedTopics(MEMBERSHIP, DEFAULT, DEFAULT, USERNAME);
        } catch (Exception e) {
            throw new RuntimeException("Getting memberships of workspace " + workspaceId + " failed", e);
        }
    }

    // TODO: make it RESTful
    @Override
    public boolean isMember(String username, long workspaceId) {
        return dmx.getPrivilegedAccess().isMember(username, workspaceId);
    }

    // TODO: make it RESTful
    @Override
    public Assoc getMembership(String username, long workspaceId) {
        Topic usernameTopic = getUsernameTopic(username);
        if (usernameTopic == null) {
            throw new RuntimeException("Unknown username: \"" + username + "\"");
        }
        return dmx.getAssocBetweenTopicAndTopic(MEMBERSHIP, usernameTopic.getId(), workspaceId, DEFAULT, DEFAULT);
    }

    @POST
    @Path("/user/{username}/workspace/{workspaceId}")
    @Transactional
    @Override
    public void createMembership(@PathParam("username") String username, @PathParam("workspaceId") long workspaceId) {
        try {
            checkWorkspaceArg(workspaceId);
            dmx.getPrivilegedAccess().createMembership(username, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Creating membership for user \"" + username + "\" and workspace " +
                workspaceId + " failed", e);
        }
    }

    @PUT
    @Path("/user/{username}")
    @Transactional
    @Override
    public List<RelatedTopic> bulkUpdateMemberships(@PathParam("username") String username,
                                                    @QueryParam("addWorkspaceIds") IdList addWorkspaceIds,
                                                    @QueryParam("removeWorkspaceIds") IdList removeWorkspaceIds) {
        try {
            List<Long> workspacesAdded = new ArrayList();
            List<Long> workspacesRemoved = new ArrayList();
            // To support applications which react on new memberships by creating further memberships programmatically,
            // we do the removal first here. Otherwise the just created memberships would be immediately deleted.
            if (removeWorkspaceIds != null) {
                for (long workspaceId : removeWorkspaceIds) {
                    if (deleteMembershipIfExists(username, workspaceId)) {
                        workspacesRemoved.add(workspaceId);
                    }
                }
            }
            if (addWorkspaceIds != null) {
                for (long workspaceId : addWorkspaceIds) {
                    if (!isMember(username, workspaceId)) {
                        createMembership(username, workspaceId);
                        workspacesAdded.add(workspaceId);
                    }
                }
            }
            logger.info("### User \"" + username + "\": workspaces added " + workspacesAdded + ", workspaces removed " +
                workspacesRemoved);
            return getMemberships(username);
        } catch (Exception e) {
            throw new RuntimeException("Bulk membership update for user \"" + username + "\" failed", e);
        }
    }

    @PUT
    @Path("/workspace/{workspaceId}")
    @Transactional
    @Override
    public List<RelatedTopic> bulkUpdateMemberships(@PathParam("workspaceId") long workspaceId,
                                                    @QueryParam("addUserIds") IdList addUserIds,
                                                    @QueryParam("removeUserIds") IdList removeUserIds) {
        try {
            List<String> usersAdded = new ArrayList();
            List<String> usersRemoved = new ArrayList();
            if (removeUserIds != null) {
                for (long userId : removeUserIds) {
                    String username = getUsername(userId);
                    if (deleteMembershipIfExists(username, workspaceId)) {
                        usersRemoved.add(username);
                    }
                }
            }
            if (addUserIds != null) {
                for (long userId : addUserIds) {
                    String username = getUsername(userId);
                    if (!isMember(username, workspaceId)) {
                        createMembership(username, workspaceId);
                        usersAdded.add(username);
                    }
                }
            }
            logger.info("### Workspace " + workspaceId + ": users added " + usersAdded + ", users removed " +
                usersRemoved);
            return getMemberships(workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Bulk membership update for workspace " + workspaceId + " failed", e);
        }
    }

    // ---

    @GET
    @Path("/workspace/admin/id")
    @Override
    public long getAdminWorkspaceId() {
        return dmx.getPrivilegedAccess().getAdminWorkspaceId();
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
        return dmx.hasProperty(objectId, MODIFIER) ? (String) dmx.getProperty(objectId, MODIFIER) : null;
    }

    @Override
    public void enrichWithUserInfo(DMXObject object) {
        long objectId = object.getId();
        object.getChildTopics().getModel()
            .set(CREATOR, getCreator(objectId))
            .set(MODIFIER, getModifier(objectId));
    }



    // === Retrieval ===

    @GET
    @Path("/user/{username}/workspaces")
    @Override
    public Collection<Topic> getWorkspacesByOwner(@PathParam("username") String username) {
        try {
            List<Topic> workspaces = dmx.getTopicsByProperty(OWNER, username);
            // Consistency check
            for (Topic workspace : workspaces) {
                if (!workspace.getTypeUri().equals(WORKSPACE)) {
                    throw new RuntimeException("Consistency check failed: topic " + workspace.getId() +
                        " is not a Workspace, but a \"" + workspace.getTypeUri() + "\"");
                }
            }
            //
            return workspaces;
        } catch (Exception e) {
            throw new RuntimeException("Getting workspaces owned by user \"" + username + "\" failed", e);
        }
    }

    @GET
    @Path("/creator/{username}/topics")
    @Override
    public Collection<Topic> getTopicsByCreator(@PathParam("username") String username) {
        return dmx.getTopicsByProperty(CREATOR, username);
    }

    @GET
    @Path("/creator/{username}/assocs")
    @Override
    public Collection<Assoc> getAssocsByCreator(@PathParam("username") String username) {
        return dmx.getAssocsByProperty(CREATOR, username);
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
        cs.registerConfigDef(new ConfigDef(
            ConfigTarget.TYPE_INSTANCES, USERNAME,
            mf.newTopicModel(LOGIN_ENABLED, new SimpleValue(NEW_ACCOUNTS_ARE_ENABLED)),
            ConfigModRole.ADMIN, this
        ));
    }

    @Override
    public void shutdown() {
        // Note 1: unregistering is crucial e.g. for redeploying the Access Control plugin. The next register call
        // (at preInstall() time) would fail as the Config service already holds such a registration.
        // Note 2: we must check if the Config service is still available. If the Config plugin is redeployed the
        // Access Control plugin is stopped/started as well but at shutdown() time the Config service is already gone.
        if (cs != null) {
            cs.unregisterConfigDef(LOGIN_ENABLED);
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
    public void preCreateAssoc(AssocModel assoc) {
        // Membership auto-typing
        PlayerModel[] p = DMXUtils.assocAutoTyping(assoc, USERNAME, WORKSPACE, MEMBERSHIP, DEFAULT, DEFAULT,
            players -> {
                try {
                    // creating a Membership requires WRITE permission for the involved workspace
                    PlayerModel wp = players[1];
                    checkWriteAccess(wp.getId());
                    return true;
                } catch (AccessControlException e) {
                    return false;
                }
            }
        );
        if (p != null) {
            // custom workspace assignment
            long workspaceId = p[1].getId();
            assoc.getChildTopics().setRef(WORKSPACE + "#" + WORKSPACE_ASSIGNMENT, workspaceId);
        }
    }

    @Override
    public void postCreateAssoc(Assoc assoc) {
        setCreatorAndModifier(assoc);
    }

    // ---

    @Override
    public void preUpdateTopic(Topic topic, TopicModel updateModel) {
        if (topic.getTypeUri().equals(USER_ACCOUNT)) {
            // Username
            TopicModel newUsernameTopic = updateModel.getChildTopics().getTopicOrNull(USERNAME);
            if (newUsernameTopic != null) {
                String newUsername = newUsernameTopic.getSimpleValue().toString();
                String username = topic.getChildTopics().getTopic(USERNAME).getSimpleValue().toString();
                if (!newUsername.equals(username)) {
                    throw new RuntimeException("A Username can't be changed (tried \"" + username + "\" -> \"" +
                        newUsername + "\")");
                }
            }
            // Password
            RelatedTopicModel passwordTopic = updateModel.getChildTopics().getTopicOrNull(PASSWORD);
            if (passwordTopic != null) {
                // empty check
                String password = passwordTopic.getSimpleValue().toString();
                if (password.equals("")) {
                    throw new RuntimeException("Password can't be empty");
                }
                // workspace assignment
                long workspaceId = getPrivateWorkspace().getId();
                String compDefUri = WORKSPACE + "#" + WORKSPACE_ASSIGNMENT;
                passwordTopic.getChildTopics().setRef(compDefUri, workspaceId);
                passwordTopic.getRelatingAssoc().getChildTopics().setRef(compDefUri, workspaceId);
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel) {
        if (topic.getTypeUri().equals(USER_ACCOUNT)) {
            // salt+hash password
            ChildTopics ct = topic.getChildTopics();
            RelatedTopic passwordTopic = ct.getTopic(PASSWORD);
            if (report.getChanges(PASSWORD) != null) {
                String username = ct.getTopic(USERNAME).getSimpleValue().toString();
                String password = passwordTopic.getSimpleValue().toString();
                Credentials cred = new Credentials(username, password);
                dmx.getPrivilegedAccess().storePasswordHash(cred, passwordTopic.getModel());
            }
        }
        //
        setModifier(topic);
    }

    @Override
    public void postUpdateAssoc(Assoc assoc, ChangeReport report, AssocModel updateModel) {
        setModifier(assoc);
    }

    @Override
    public void postDeleteTopic(TopicModel topic) {
        if (topic.getTypeUri().equals(USERNAME)) {
            String username = topic.getSimpleValue().toString();
            Collection<Topic> workspaces = getWorkspacesByOwner(username);
            String currentUser = getUsername();
            logger.info("### Transferring ownership of " + workspaces.size() + " workspaces from \"" + username +
                "\" -> \"" + currentUser + "\"");
            for (Topic workspace : workspaces) {
                setWorkspaceOwner(workspace, currentUser);
            }
        }
    }

    // ---

    @Override
    public void serviceRequestFilter(ContainerRequest containerRequest) {
        // Note: HttpServletRequest and HttpServletResponse are injected through JAX-RS.
        requestFilter(request, response);
    }

    @Override
    public void staticResourceFilter(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        // Note: the static resource filter is triggered through our custom OSGi HTTP Context. JAX-RS injection is not
        // in place.
        requestFilter(servletRequest, servletResponse);
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
            throw new RuntimeException("Disk quota of " + userInfo(username) + " exceeded, diskQuota=" + diskQuota +
                " bytes, occupiedSpace=" + occupiedSpace + " bytes, fileSize=" + fileSize + " bytes");
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Checks a "workspaceId" argument. 2 checks are performed:
     *   - the workspace ID refers actually to a workspace
     *   - the workspace is writable by the current user
     *
     * If any check fails an exception is thrown.
     *
     * @param   workspaceId     the workspace ID to check
     */
    private void checkWorkspaceArg(long workspaceId) {
        // ### Basically copied from WorkspacesPlugin.checkAssignmentArgs()
        Topic workspace = dmx.getTopic(workspaceId);
        String typeUri = workspace.getTypeUri();
        if (!typeUri.equals(WORKSPACE)) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a Workspace (but a \"" + typeUri +
                "\")");
        }
        //
        workspace.checkWriteAccess();       // throws AccessControlException
    }

    /**
     * @param   id      ID of an Username topic
     */
    private String getUsername(long id) {
        Topic username = dmx.getTopic(id);
        String typeUri = username.getTypeUri();
        if (!typeUri.equals(USERNAME)) {
            throw new IllegalArgumentException("Topic " + id + " is not a Username (but a \"" + typeUri + "\")");
        }
        return username.getSimpleValue().toString();
    }

    private boolean deleteMembershipIfExists(String username, long workspaceId) {
        Assoc membership = getMembership(username, workspaceId);
        if (membership != null) {
            membership.delete();
            return true;
        }
        return false;
    }

    // --- Disk Quota ---

    private long getOccupiedSpace(String username) {
        long occupiedSpace = 0;
        for (Topic fileTopic : dmx.getTopicsByType(FILE)) {
            long fileTopicId = fileTopic.getId();
            if (getCreator(fileTopicId).equals(username)) {
                try {
                    occupiedSpace += fs.getFile(fileTopicId).length();
                } catch (Exception e) {
                    // Note: file might be deleted meanwhile
                }
            }
        }
        return occupiedSpace;
    }



    // === Request Filter ===

    private void requestFilter(HttpServletRequest request, HttpServletResponse response) {
        logger.fine("##### " + request.getMethod() + " " + request.getRequestURL() +
            "\n      ##### \"Authorization\"=\"" + request.getHeader("Authorization") + "\"" +
            "\n      ##### " + info(request.getSession(false)));    // create=false
        // 1) apply subnet filter
        checkRequestOrigin(request);        // throws WebApplicationException 403 Forbidden
        // 2) create session (if needed)
        HttpSession session = getSession(request, response);
        // 3) check authorization (if not yet logged in)
        if (username(session) == null) {
            checkAuthorization(request);    // throws WebApplicationException 401 Unauthorized
        }
    }

    private HttpSession getSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession();
            String cookie = response.getHeader("Set-Cookie");
            response.setHeader("Set-Cookie", cookie + ";SameSite=Strict");
            logger.fine("### Creating " + info(session) + ", response=" + response);
        }
        return session;
    }

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
        logger.info("##### Logging out as \"" + username + "\"");
        logger.fine("##### Detaching username from " + info(session));
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
            object.setProperty(CREATOR, username, true);   // addToIndex=true
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
        object.setProperty(MODIFIER, username, false);     // addToIndex=false
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
     * Calculates the permission for a user to perform an operation on an object (topic or association).
     *
     * This is a low-level method. It does not grant permission to "System".
     * Consider the high-level checkReadAccess()/checkWriteAccess() methods instead.
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
