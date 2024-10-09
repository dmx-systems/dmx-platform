package systems.dmx.accesscontrol;

import com.sun.jersey.spi.container.ContainerRequest;
import org.eclipse.jetty.http.HttpStatus;
import systems.dmx.accesscontrol.event.PostLoginUser;
import systems.dmx.accesscontrol.event.PostLogoutUser;
import systems.dmx.accesscontrol.identityproviderredirect.*;
import systems.dmx.config.*;
import systems.dmx.core.*;
import systems.dmx.core.model.*;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.*;
import systems.dmx.core.service.accesscontrol.AccessControlException;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.Operation;
import systems.dmx.core.service.accesscontrol.Permissions;
import systems.dmx.core.service.event.*;
import systems.dmx.core.util.DMXUtils;
import systems.dmx.core.util.IdList;
import systems.dmx.core.util.JavaUtils;
import systems.dmx.files.FilesService;
import systems.dmx.files.event.CheckDiskQuota;
import systems.dmx.workspaces.WorkspacesService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static systems.dmx.accesscontrol.Constants.*;
import static systems.dmx.core.Constants.DEFAULT;
import static systems.dmx.files.Constants.FILE;
import static systems.dmx.workspaces.Constants.WORKSPACE;
import static systems.dmx.workspaces.Constants.WORKSPACE_ASSIGNMENT;


@Path("/access-control")
@Consumes("application/json")
@Produces("application/json")
public class AccessControlPlugin extends PluginActivator implements AccessControlService, ConfigCustomizer,
                                                                                          CheckTopicReadAccess,
                                                                                          CheckTopicWriteAccess,
                                                                                          CheckAssocReadAccess,
                                                                                          CheckAssocWriteAccess,
                                                                                          PreCreateAssoc,
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

    private final Map<String, AuthorizationMethod> authorizationMethods = new HashMap<>();

    private static final Logger logger = Logger.getLogger(AccessControlPlugin.class.getName());

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



    @Override
    @Deprecated
    public Topic createUserAccount(Credentials cred) {
        // TODO: Remove in next major DMX iteration
        return DeprecatedUserAccountMethods.call(
                getBundleContext(),
                (service) -> service.createUserAccount(cred));
    }

    @Override
    @Deprecated
    public Topic _createUserAccount(Credentials cred) throws Exception {
        // TODO: Remove in next major DMX iteration
        try {
            return DeprecatedUserAccountMethods.call(
                    getBundleContext(),
                    (service) -> service._createUserAccount(cred));
        } catch (DeprecatedUserAccountMethods.CallableException ce) {
            throw ce.causeFromCallable;
        }
    }

    @Override
    @Deprecated
    public Topic createUsername(String username) {
        // TODO: Remove in next major DMX iteration
        return DeprecatedUserAccountMethods.call(
                getBundleContext(),
                (service) -> service.createUsername(username));
    }

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
    public LogoutResponse logout() {
        // TODO: If an idp login was present, then return the redirect URI
        // TODO: Introduce separate new API that only does anything for IDP
        HttpSession session = request.getSession(false);
        String adapterName = (String) session.getAttribute("idpAdapterName");
        // If user was authenticated with idp it also needs to be logged out this way.
        if (adapterName != null) {
            List<IdentityProviderRedirectAdapter> matchingAdapters =
                    authorizationMethods.values().stream().map(AuthorizationMethod::getIdentityProviderRedirectAdapter)
                            .filter(Objects::nonNull)
                            .filter((it) -> it.getName().equals(adapterName))
                            .collect(Collectors.toList());
            if (matchingAdapters.size() == 1) {
                URI logoutUri = matchingAdapters.get(0).createLogoutUri(session);
                // Might only happen at a later point
                _logout(request);
                return new LogoutResponse(logoutUri);
            }
        }

        _logout(request);
        //
        // For a "private" DMX installation: emulate a HTTP logout by forcing the webbrowser to bring up its
        // login dialog and to forget the former Authorization information. The user is supposed to press "Cancel".
        // The login dialog can't be used to login again.
        if (!IS_PUBLIC_INSTALLATION) {
            throw401Unauthorized(true);     // showBrowserLoginDialog=true
        }

        return new LogoutResponse(null);
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

    // === Identity Provider Redirect ===

    @GET
    @Path("/identity-provider-redirect/configuration")
    public List<IdentityProviderRedirectConfiguration>
    getIdentityProviderRedirectConfiguration() {
        return authorizationMethods
                .values()
                .stream()
                .map(AuthorizationMethod::getIdentityProviderRedirectAdapter)
                .filter(Objects::nonNull)
                .map(IdentityProviderRedirectConfiguration::new)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/identity-provider-redirect/uri")
    public Response getIdentityProviderUri(@QueryParam("name") String name) {
        // Finds all adapters with the provided name
        List<IdentityProviderRedirectAdapter> matchingAdapters =
            authorizationMethods
                .values()
                .stream()
                .map(AuthorizationMethod::getIdentityProviderRedirectAdapter)
                .filter((it) -> (it != null && it.getName().equals(name)))
                .collect(Collectors.toList());

        switch (matchingAdapters.size()) {
            case 0:
                return Response.status(Status.NOT_FOUND).build();
            case 1:
                IdentityProviderRedirectAdapter adapter = matchingAdapters.get(0);
                // when there is just one adapter, create a URI and provide a
                // single use authenticator to the adapter
                URI redirectUri = adapter
                        .createIdentityProviderRedirectUri(
                                newSingleUseAuthenticator(adapter.getName()));
                return Response
                        .status(302)
                        .location(redirectUri)
                        .build();
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
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
    public void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel) {
        //
        setModifier(topic);
    }

    @Override
    public void postUpdateAssoc(Assoc assoc, ChangeReport report, AssocModel updateModel) {
        setModifier(assoc);
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
            // FIXME: throw AccessControlException?
            throw new RuntimeException("Disk quota of " + userInfo(username) + " exceeded, diskQuota=" + diskQuota +
                " bytes, occupiedSpace=" + occupiedSpace + " bytes, fileSize=" + fileSize + " bytes");
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    SingleUseAuthenticator newSingleUseAuthenticator(String name) {
        return new SingleUseAuthenticator() {

            private AtomicBoolean used = new AtomicBoolean(false);

            @Override
            public void authenticate(IdentityAssertion identityAssertion) throws SingleUseAuthenticationFailedException {
                try {
                    if (used.get()) {
                        throw new SingleUseAuthenticationFailedException("Already attempted a login", null);
                    }
                    _login(identityAssertion.getUsername(), request);
                    request.getSession(false).setAttribute("idpAdapterName", name);
                } finally {
                    used.set(true);
                }
            }
        };
    }

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
        return dmx.getPrivilegedAccess().inRequestScope(request);
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
