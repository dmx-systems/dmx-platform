package systems.dmx.accountmanagement;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accesscontrol.AuthorizationMethod;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.workspaces.WorkspacesService;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static systems.dmx.accesscontrol.Constants.USERNAME;
import static systems.dmx.accountmanagement.Constants.ACCOUNT_MANAGER_NAME;

public class AccountManagementPluginTest {

    private final AccountManagementPlugin subject = new AccountManagementPlugin();

    private final PrivilegedAccess privilegedAccess = mock();
    private final CoreService dmx = mock();

    private final ModelFactory mf = mock();

    private final WorkspacesService ws = mock();
    private final AccessControlService accessControlService = mock();

    private void set(Object o, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = PluginActivator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(o, value);
    }

    @BeforeEach
    public void before() throws Exception {
        AccountManagementPlugin.logger.setLevel(Level.OFF);

        when(mf.newTopicModel(anyString(), any(SimpleValue.class))).thenReturn(mock());

        when(dmx.createTopic(any())).thenReturn(mock());

        // by default no user present
        when(privilegedAccess.getUsernameTopic(anyString())).thenReturn(null);

        // by default run the Callable and return its result
        when(privilegedAccess.runInWorkspaceContext(anyLong(), any())).thenAnswer((InvocationOnMock invocation) ->
                invocation.<Callable<Topic>>getArgument(1).call());
        doNothing().when(privilegedAccess).assignToWorkspace(any(), anyLong());
        when(dmx.getPrivilegedAccess()).thenReturn(privilegedAccess);

        doNothing().when(accessControlService).setWorkspaceOwner(any(), any());

        set(subject, "dmx", dmx);
        set(subject, "mf", mf);
        subject.ws = ws;
        subject.accessControlService = accessControlService;
    }

    @Test
    @DisplayName("after creation should not contain any account manager")
    void should_not_contain_any_account_manager() {
        // then:
        assertThat(subject.getAccountManagerNames()).isEqualTo(Collections.emptyList());
    }

    @Test
    @DisplayName("getConfiguredAccountManager() should be 'DMX' by default")
    void getConfiguredAccountManager_should_be_dmx() {
        // when:
        String result = subject.getConfiguredAccountManagerName();

        // then:
        assertThat(result).isEqualTo("DMX");
    }

    @Test
    @DisplayName("init() should add 'DMX' account manager")
    void init_should_add_dmx_account_manager() {
        // when:
        subject.serviceArrived(accessControlService);

        // then:
        assertThat(subject.getAccountManagerNames()).isEqualTo(Collections.singletonList("DMX"));
    }

    @Test
    @DisplayName("registerAccountManager() should add account manager")
    void init_should_add_account_manager() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);

        // when:
        subject.registerAccountManager(accountManager);

        // then:
        assertThat(subject.getAccountManagerNames()).isEqualTo(Collections.singletonList(accountManagerName));
    }

    @Test
    @DisplayName("registerAccountManager() should add authorization method")
    void init_should_add_authorization_method() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);

        // when:
        subject.registerAccountManager(accountManager);

        // then:
        verify(accessControlService).registerAuthorizationMethod(eq(accountManagerName), any());
    }

    @Test
    @DisplayName("registerAccountManager() should remove account manager")
    void init_should_remove_account_manager() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);

        subject.registerAccountManager(accountManager);

        // when:
        subject.unregisterAccountManager(accountManager);

        // then:
        assertThat(subject.getAccountManagerNames()).isEqualTo(Collections.emptyList());
    }

    @Test
    @DisplayName("unregisterAccountManager() should remove authorization method")
    void init_should_remove_authorization_method() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);

        subject.registerAccountManager(accountManager);

        // when:
        subject.unregisterAccountManager(accountManager);

        // then:
        verify(accessControlService).unregisterAuthorizationMethod(accountManagerName);
    }

    @Test
    @DisplayName("_createUserAccount() should create account with the method present in the credentials")
    void _createUserAccount_should_create_account_with_method_from_credentials() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        doNothing().when(accountManager).createAccount(any());

        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        subject._createUserAccount(credentials);

        // then:
        verify(accountManager).createAccount(credentials);
    }

    @Test
    @DisplayName("_createUserAccount() should create account with the configured manager when method missing in the credentials")
    void _createUserAccount_should_create_account_with_configured_manager_when_not_in_credentials() {
        // given:
        String accountManagerName = "DMX";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        doNothing().when(accountManager).createAccount(any());

        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );

        // when:
        subject._createUserAccount(credentials);

        // then:
        verify(accountManager).createAccount(credentials);
    }

    @Test
    @DisplayName("createUserAccount() should check whether current user is admin")
    void createUserAccount_should_check_current_user_is_admin() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).createAccount(any());
        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );

        // when:
        subject.createUserAccount(credentials);

        // then:
        verify(accessControlService).checkAdmin();
    }

    @Test
    @DisplayName("createUserAccount() should create account when the admin check succeeds")
    void createUserAccount_should_create_account_when_admin_check_succeeds() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).createAccount(any());
        subject.registerAccountManager(accountManager);

        // Not doing anything means, the user is an admin
        doNothing().when(accessControlService).checkAdmin();

        Credentials credentials = new Credentials(
                "username",
                "password"
        );

        // when:
        subject.createUserAccount(credentials);

        // then:
        verify(accountManager).createAccount(credentials);
    }

    @Test
    @DisplayName("createUserAccount() should not create account when the admin check fails")
    void createUserAccount_should_not_create_account_when_admin_check_fails() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).createAccount(any());
        subject.registerAccountManager(accountManager);

        // Throwing exception means, the user is not an admin
        Throwable throwable = new IllegalStateException("Not admin");
        doThrow(throwable).when(accessControlService).checkAdmin();

        // when:
        ThrowableAssert.ThrowingCallable callable = () -> subject.createUserAccount(mock());

        // then:
        assertThatThrownBy(callable)
                .isInstanceOf(RuntimeException.class)
                .hasCause(throwable);
        verify(accountManager, times(0)).createAccount(any());
    }

    @Test
    @DisplayName("changePassword() should lookup user from username from new credentials")
    void changePassword_should_lookup_username() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).changePassword(any(), any());
        subject.registerAccountManager(accountManager);

        // Lookup succeeds
        when(privilegedAccess.getUsernameTopic(anyString())).thenReturn(mock());

        String userName = "theUser";
        Credentials newCredentials = new Credentials(userName, "newpassword");

        // when:
        subject.changePassword(mock(), newCredentials);

        // then:
        verify(privilegedAccess).getUsernameTopic(userName);
    }

    @Test
    @DisplayName("changePassword() should change password with configured manager when not set via property on username topic")
    void changePassword_should_change_password_with_configured_manager_when_not_set_via_property_on_username_topic() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).changePassword(any(), any());
        subject.registerAccountManager(accountManager);

        AccountManager otherAccountManager = mock();
        String otherAccountManagerName = "OtherAccountManager";
        when(otherAccountManager.name()).thenReturn(otherAccountManagerName);
        doNothing().when(otherAccountManager).changePassword(any(), any());
        subject.registerAccountManager(otherAccountManager);

        Topic userNameTopic = mock();
        when(userNameTopic.hasProperty(any())).thenReturn(false);

        // Lookup succeeds
        when(privilegedAccess.getUsernameTopic(anyString())).thenReturn(userNameTopic);

        String userName = "theUser";
        Credentials currentCredentials = mock();
        Credentials newCredentials = new Credentials(userName, "newpassword");

        // when:
        subject.changePassword(currentCredentials, newCredentials);

        // then:
        verify(accountManager).changePassword(currentCredentials, newCredentials);
        verify(otherAccountManager, times(0)).changePassword(currentCredentials, newCredentials);
    }

    @Test
    @DisplayName("changePassword() should change password with manager that is set via property on username topic")
    void changePassword_should_change_password_with_manager_set_via_property_on_username_topic() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("AccountManager");
        doNothing().when(accountManager).changePassword(any(), any());
        subject.registerAccountManager(accountManager);

        AccountManager otherAccountManager = mock();
        String otherAccountManagerName = "OtherAccountManager";
        when(otherAccountManager.name()).thenReturn(otherAccountManagerName);
        doNothing().when(otherAccountManager).changePassword(any(), any());
        subject.registerAccountManager(otherAccountManager);

        // Lookup succeeds
        Topic userNameTopic = mock();
        when(privilegedAccess.getUsernameTopic(anyString())).thenReturn(userNameTopic);
        when(userNameTopic.hasProperty(any())).thenReturn(true);
        when(userNameTopic.getProperty(ACCOUNT_MANAGER_NAME)).thenReturn(otherAccountManagerName);

        String userName = "theUser";
        Credentials currentCredentials = mock();
        Credentials newCredentials = new Credentials(userName, "newpassword");

        // when:
        subject.changePassword(currentCredentials, newCredentials);

        // then:
        verify(accountManager, times(0)).changePassword(currentCredentials, newCredentials);
        verify(otherAccountManager).changePassword(currentCredentials, newCredentials);
    }

    @Test
    @DisplayName("preDeleteTopic() with username topic should invoke onUsernameDeleted with configured manager when not set via property on username topic")
    void preDeleteTopicd_should_invoke_onUsernameDeleted_with_configured_manager() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("DMX");
        doNothing().when(accountManager).changePassword(any(), any());
        subject.registerAccountManager(accountManager);

        AccountManager otherAccountManager = mock();
        String otherAccountManagerName = "OtherAccountManager";
        when(otherAccountManager.name()).thenReturn(otherAccountManagerName);
        doNothing().when(otherAccountManager).changePassword(any(), any());
        subject.registerAccountManager(otherAccountManager);

        String userName = "theUser";
        SimpleValue simpleValue = mock();
        when(simpleValue.toString()).thenReturn(userName);

        Topic userNameTopic = mock();
        when(userNameTopic.getSimpleValue()).thenReturn(simpleValue);
        when(userNameTopic.getTypeUri()).thenReturn(USERNAME);
        when(userNameTopic.hasProperty(any())).thenReturn(false);

        // when:
        subject.preDeleteTopic(userNameTopic);

        // then:
        verify(accountManager).onUsernameDeleted(userName);
        verify(otherAccountManager, times(0)).onUsernameDeleted(anyString());
    }

    @Test
    @DisplayName("preDeleteTopic() with username topic should invoke onUsernameDeleted with manager that is set via property on username topic")
    void preDeleteTopicd_should_invoke_onUsernameDeleted_with_manager_from_property() {
        // given:
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn("AccountManager");
        doNothing().when(accountManager).changePassword(any(), any());
        subject.registerAccountManager(accountManager);

        AccountManager otherAccountManager = mock();
        String otherAccountManagerName = "OtherAccountManager";
        when(otherAccountManager.name()).thenReturn(otherAccountManagerName);
        doNothing().when(otherAccountManager).changePassword(any(), any());
        subject.registerAccountManager(otherAccountManager);

        String userName = "theUser";
        SimpleValue simpleValue = mock();
        when(simpleValue.toString()).thenReturn(userName);

        Topic userNameTopic = mock();
        when(userNameTopic.getSimpleValue()).thenReturn(simpleValue);
        when(userNameTopic.getTypeUri()).thenReturn(USERNAME);
        when(userNameTopic.hasProperty(any())).thenReturn(true);
        when(userNameTopic.getProperty(ACCOUNT_MANAGER_NAME)).thenReturn(otherAccountManagerName);

        // when:
        subject.preDeleteTopic(userNameTopic);

        // then:
        verify(accountManager, times(0)).onUsernameDeleted(anyString());
        verify(otherAccountManager).onUsernameDeleted(userName);
    }

    @Test
    @DisplayName("postDeleteTopic() for username topic should set ownership of all workspace of the deleted username to the logged in user")
    void postDeleteTopic_should_set_ownership() {
        // given:
        String loggedInUser = "theLoggedInUser";
        when(accessControlService.getUsername()).thenReturn(loggedInUser);

        String userName = "theDeletedUser";
        SimpleValue simpleValue = mock();
        when(simpleValue.toString()).thenReturn(userName);

        TopicModel userNameTopic = mock();
        when(userNameTopic.getSimpleValue()).thenReturn(simpleValue);
        when(userNameTopic.getTypeUri()).thenReturn(USERNAME);

        Collection<Topic> workspaces = Arrays.asList(mock(), mock(), mock());
        when(accessControlService.getWorkspacesByOwner(any())).thenReturn(workspaces);

        // when:
        subject.postDeleteTopic(userNameTopic);

        // then:
        verify(accessControlService).getWorkspacesByOwner(userName);
        verify(accessControlService).getUsername();

        for(Topic workspace : workspaces) {
            verify(accessControlService).setWorkspaceOwner(workspace, loggedInUser);
        }
    }

    @Test
    @DisplayName("postDeleteTopic() should do nothing for non-username topic")
    void postDeleteTopic_should_do_nothing() {
        // given:
        TopicModel topicModelopic = mock();
        when(topicModelopic.getTypeUri()).thenReturn("bla.foo.Baz");

        // when:
        subject.postDeleteTopic(topicModelopic);

        // then:
        verify(accessControlService, times(0)).getWorkspacesByOwner(any());
        verify(accessControlService, times(0)).getUsername();
        verify(accessControlService, times(0)).setWorkspaceOwner(any(), any());
    }

    // TODO: asAuthorizationMethod stuff
    @Test
    @DisplayName("AuthorizationMethod::checkLogin() should call account manager check credentials")
    void AuthorizationMethod_checkLogin_should_call_account_manager_check_credentials() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        when(accountManager.checkCredentials(any())).thenReturn(CheckCredentialsResult.failed());
        doNothing().when(accountManager).createAccount(any());

        final AuthorizationMethod[] authorizationMethod = new AuthorizationMethod[1];
        doAnswer((invocation) -> {
            authorizationMethod[0] = invocation.getArgument(1); // AuthorizationMethod instance
            return null;
        }).when(accessControlService).registerAuthorizationMethod(any(), any());

        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        authorizationMethod[0].checkCredentials(credentials);

        // then:
        verify(accountManager).checkCredentials(credentials);
    }

    @Test
    @DisplayName("AuthorizationMethod::checkLogin() should return null when credential check failed")
    void AuthorizationMethod_checkLogin_should_return_null() {
        // given:
        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        when(accountManager.checkCredentials(any())).thenReturn(CheckCredentialsResult.failed());
        doNothing().when(accountManager).createAccount(any());

        final AuthorizationMethod[] authorizationMethod = new AuthorizationMethod[1];
        doAnswer((invocation) -> {
            authorizationMethod[0] = invocation.getArgument(1); // AuthorizationMethod instance
            return null;
        }).when(accessControlService).registerAuthorizationMethod(any(), any());

        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        Topic result = authorizationMethod[0].checkCredentials(credentials);

        // then:
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("AuthorizationMethod::checkLogin() should return given topic when credential check suceeded")
    void AuthorizationMethod_checkLogin_should_return_username_topic() {
        // given:
        Topic usernameTopic = mock();

        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        when(accountManager.checkCredentials(any())).thenReturn(CheckCredentialsResult.success(usernameTopic));
        doNothing().when(accountManager).createAccount(any());

        final AuthorizationMethod[] authorizationMethod = new AuthorizationMethod[1];
        doAnswer((invocation) -> {
            authorizationMethod[0] = invocation.getArgument(1); // AuthorizationMethod instance
            return null;
        }).when(accessControlService).registerAuthorizationMethod(any(), any());

        subject.registerAccountManager(accountManager);

        Credentials credentials = new Credentials(
                "username",
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        Topic result = authorizationMethod[0].checkCredentials(credentials);

        // then:
        assertThat(result).isEqualTo(usernameTopic);
    }

    @Test
    @DisplayName("AuthorizationMethod::checkLogin() should do a username lookup and return its result when non-null")
    void AuthorizationMethod_checkLogin_should_do_username_lookup_and_return() {
        // given:
        Topic usernameTopic = mock();
        when(accessControlService.getUsernameTopic(any())).thenReturn(usernameTopic);

        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        when(accountManager.checkCredentials(any())).thenReturn(CheckCredentialsResult.lookupOrCreationRequired());
        doNothing().when(accountManager).createAccount(any());

        final AuthorizationMethod[] authorizationMethod = new AuthorizationMethod[1];
        doAnswer((invocation) -> {
            authorizationMethod[0] = invocation.getArgument(1); // AuthorizationMethod instance
            return null;
        }).when(accessControlService).registerAuthorizationMethod(any(), any());

        subject.registerAccountManager(accountManager);

        String userName = "username";
        Credentials credentials = new Credentials(
                userName,
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        Topic result = authorizationMethod[0].checkCredentials(credentials);

        // then:
        verify(accessControlService).getUsernameTopic(userName);
        assertThat(result).isEqualTo(usernameTopic);
    }

    @Test
    @DisplayName("AuthorizationMethod::checkLogin() should do a username and workspace creation when username lookup is null")
    void AuthorizationMethod_checkLogin_should_do_username_creation() throws Exception {
        // given:
        Topic usernameTopic = mock();
        // lookup return null
        when(accessControlService.getUsernameTopic(any())).thenReturn(null);

        String accountManagerName = "MockManager";
        AccountManager accountManager = mock();
        when(accountManager.name()).thenReturn(accountManagerName);
        when(accountManager.checkCredentials(any())).thenReturn(CheckCredentialsResult.lookupOrCreationRequired());
        doNothing().when(accountManager).createAccount(any());

        // shortcut for user creation
        doReturn(usernameTopic).when(privilegedAccess).runInWorkspaceContext(anyLong(), any());

        DMXTransaction transaction = mock();
        when(dmx.beginTx()).thenReturn(transaction);

        final AuthorizationMethod[] authorizationMethod = new AuthorizationMethod[1];
        doAnswer((invocation) -> {
            authorizationMethod[0] = invocation.getArgument(1); // AuthorizationMethod instance
            return null;
        }).when(accessControlService).registerAuthorizationMethod(any(), any());

        subject.registerAccountManager(accountManager);

        String userName = "username";
        Credentials credentials = new Credentials(
                userName,
                "password"
        );
        credentials.methodName = accountManagerName;

        // when:
        Topic result = authorizationMethod[0].checkCredentials(credentials);

        // then:
        verify(privilegedAccess).runInWorkspaceContext(eq(-1L), any());
        assertThat(result).isEqualTo(usernameTopic);
    }

}
