package systems.dmx.accountmanagement;

import systems.dmx.core.service.accesscontrol.Credentials;



public interface AccountManager {

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

    /**
     * Check whether the given credentials are valid in the underlying system.
     * <p>
     * Non-DMX implementations are supposed to tell the platform via the return value, that a username topic
     * lookup is necessary. See {@link CheckCredentialsResult} for details on how to do that.
     *
     * @param credentials
     * @return
     */
    CheckCredentialsResult checkCredentials(Credentials credentials);

}
