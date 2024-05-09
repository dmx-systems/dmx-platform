package systems.dmx.accountmanagement;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.service.accesscontrol.SharingMode;
import systems.dmx.core.service.event.PostDeleteTopic;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.service.event.PreDeleteTopic;
import systems.dmx.core.service.event.PreUpdateTopic;
import systems.dmx.workspaces.WorkspacesService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.*;
import java.util.logging.Logger;

import static systems.dmx.accesscontrol.Constants.USERNAME;
import static systems.dmx.accountmanagement.Constants.*;
import static systems.dmx.workspaces.Constants.WORKSPACE;
import static systems.dmx.workspaces.Constants.WORKSPACE_ASSIGNMENT;


@Path("/account-management")
@Consumes("application/json")
@Produces("application/json")
public class AccountManagementPlugin extends PluginActivator implements AccountManagementService, PreDeleteTopic,
        PreUpdateTopic, PostDeleteTopic, PostUpdateTopic {

    private static final Logger logger = Logger.getLogger(AccountManagementPlugin.class.getName());

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String ACCOUNT_MANAGEMENT_METHOD = System.getProperty("dmx.accountmanagement.method", DmxAccountManagementMethod.NAME);

    private static final boolean NEW_ACCOUNTS_ARE_ENABLED = Boolean.parseBoolean(
            System.getProperty("dmx.security.new_accounts_are_enabled", "true")
    );
    private static final String SITE_SALT = System.getProperty("dmx.security.site_salt", "");
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService ws;

    @Inject
    private AccessControlService accessControlService;

    private final Map<String, AccountManagementMethod> accountManagementMethods = new HashMap<>();

    static {
        logger.info("Security config:" +
                "\n  dmx.security.new_accounts_are_enabled = " + NEW_ACCOUNTS_ARE_ENABLED);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void init() {
        // registers built-in account management method
        registerAccountManagementMethod(new DmxAccountManagementMethod(dmx, mf, SITE_SALT));
    }

    // ****************************
    // *** AccountManagementService ***
    // ****************************

    // === User Accounts ===

    @Override
    public void registerAccountManagementMethod(AccountManagementMethod method) {
        accountManagementMethods.put(method.name(), method);
    }

    @Override
    public void unregisterAccountManagementMethod(AccountManagementMethod method) {
        accountManagementMethods.remove(method.name());
    }

    @Override
    public List<String> getAccountManagementMethods() {
        return new ArrayList<>(accountManagementMethods.keySet());
    }

    @Override
    public String getConfiguredAccountManagementMethod() { return ACCOUNT_MANAGEMENT_METHOD; }

    @POST
    @Path("/user-account")
    @Transactional
    @Override
    public Topic createUserAccount(Credentials cred) {
        try {
            accessControlService.checkAdmin();
            return _createUserAccount(cred);
        } catch (Exception e) {
            throw new RuntimeException("Creating user account \"" + cred.username + "\" failed", e);
        }
    }

    @Override
    public Topic _createUserAccount(final Credentials cred) {
        String username = cred.username;
        // If set, the methodName controls in which system a user is created, otherwise use the configured method
        String methodName = cred.methodName != null ? cred.methodName : ACCOUNT_MANAGEMENT_METHOD;

        logger.info(String.format("Creating user account '%s' with method '%s'", username, methodName));

        // 1) create Username topic and private workspace
        final Topic usernameTopic = createUsernameAndPrivateWorkspace(username);

        // 2) Create actual account
        AccountManagementMethod method = getAccountManagementMethod(methodName);
        method.createAccount(cred);

        // Links account management method to username
        usernameTopic.setProperty(ACCOUNT_MANAGEMENT_METHOD_NAME, method.name(), true);

        return usernameTopic;
    }

    @Override
    public void changePassword(Credentials currentCred, Credentials newCred) {
        Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(newCred.username);
        getAccountManagementMethodForUsername(usernameTopic).changePassword(currentCred, newCred);
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
                long workspaceId = accessControlService.getPrivateWorkspace().getId();
                String compDefUri = WORKSPACE + "#" + WORKSPACE_ASSIGNMENT;
                passwordTopic.getChildTopics().setRef(compDefUri, workspaceId);
                passwordTopic.getRelatingAssoc().getChildTopics().setRef(compDefUri, workspaceId);
            }
        }
    }

    @Override
    public void preDeleteTopic(Topic topic) {
        if (topic.getTypeUri().equals(USERNAME)) {
            String username = topic.getSimpleValue().toString();

            // Removes user data from potentially external systems
            getAccountManagementMethodForUsername(topic).onUsernameDeleted(username);
        }
    }

    @Override
    public void postDeleteTopic(TopicModel topic) {
        if (topic.getTypeUri().equals(USERNAME)) {
            String username = topic.getSimpleValue().toString();
            Collection<Topic> workspaces = accessControlService.getWorkspacesByOwner(username);
            String currentUser = accessControlService.getUsername();
            logger.info("### Transferring ownership of " + workspaces.size() + " workspaces from \"" + username +
                    "\" -> \"" + currentUser + "\"");
            for (Topic workspace : workspaces) {
                accessControlService.setWorkspaceOwner(workspace, currentUser);
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
    }

    // ------------------------------------------------------------------------------------------------- Private Methods
    private Topic createUsernameAndPrivateWorkspace(final String username) {
        try {
            logger.info("Creating username topic \"" + username + "\"");
            PrivilegedAccess pa = dmx.getPrivilegedAccess();
            //
            // 1) check username uniqueness
            // Note: we can't do this check in the preCreateTopic() listener. If such an username topic exists already
            // the DMX value integrator will reuse this one instead of trying to create a new one. The preCreateTopic()
            // listener will not trigger.
            Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(username);
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
            accessControlService.setWorkspaceOwner(
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

    private AccountManagementMethod getAccountManagementMethod(String methodName) {
        return accountManagementMethods.get(
                accountManagementMethods.containsKey(methodName)
                        ? methodName
                        : DmxAccountManagementMethod.NAME
        );
    }

    private AccountManagementMethod getAccountManagementMethodForUsername(Topic usernameTopic) {
        String methodName =
                (usernameTopic.hasProperty(ACCOUNT_MANAGEMENT_METHOD_NAME))
                        ? usernameTopic.getProperty(ACCOUNT_MANAGEMENT_METHOD_NAME).toString()
                        : DmxAccountManagementMethod.NAME;

        return getAccountManagementMethod(methodName);
    }

}
