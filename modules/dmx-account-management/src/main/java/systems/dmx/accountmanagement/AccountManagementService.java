package systems.dmx.accountmanagement;

import systems.dmx.core.Topic;
import systems.dmx.core.service.accesscontrol.Credentials;

import java.util.List;


public interface AccountManagementService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Private workspaces
    static final String DEFAULT_PRIVATE_WORKSPACE_NAME = "Private Workspace";

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === User Accounts ===

    void registerAccountManagementMethod(AccountManagementMethod method);

    void unregisterAccountManagementMethod(AccountManagementMethod method);

    List<String> getAccountManagementMethods();

    String getConfiguredAccountManagementMethod();

    /**
     * Creates an user account.
     * Only DMX admins are allowed to create user accounts.
     *
     * @throws  RuntimeException    if the requesting user is not a DMX admin.
     *
     * @return  The "Username" topic of the created user account.
     */
    Topic createUserAccount(Credentials cred);

    /**
     * Creates an user account.
     * This is a privileged method: no permissions are checked.
     * <p>
     * Security: this method is not callable from outside as it has no REST interface. So the DMX platform is still
     * secure. On the other hand, a 3rd-party plugin which provides a RESTful interface to this method is required
     * to apply an additional authorization measure, e.g. a short-living access token sent via email.
     *
     * @return  The "Username" topic of the created user account.
     */
    Topic _createUserAccount(Credentials cred);

    /**
     * Changes the password of an account to the newly given credentials.
     *
     * Depending on the system that ultimately performs the password change, the current credentials are necessary.
     * It can be <code>null</code> if not necessary.
     *
     * @param currentCred Current credentials of the account whose password is to be changed.
     * @param newCred Credentials containing the new password.
     */
    void changePassword(Credentials currentCred, Credentials newCred);
}
