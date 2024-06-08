package systems.dmx.accountmanagement;

import systems.dmx.accesscontrol.AuthorizationMethod;
import systems.dmx.core.service.accesscontrol.Credentials;



public interface AccountManagementMethod extends AuthorizationMethod {

    String name();

    /**
     * Unconditionally creates an account.
     */
    void createAccount(Credentials cred);

    /**
     * Unconditionally changes an account's password
     */
    void changePassword(Credentials currentCred, Credentials newCred);

    /**
     * Called by the DMX platform when a username topic has been deleted. This indicates
     * the no more logins will happen with that username.
     *
     * The implementation may choose to remove what is has stored about that
     * user, too.
     *
     * @param username
     */
    void onUsernameDeleted(String username);

}
