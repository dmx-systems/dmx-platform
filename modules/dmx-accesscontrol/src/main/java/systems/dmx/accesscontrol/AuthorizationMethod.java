package systems.dmx.accesscontrol;

import systems.dmx.core.Topic;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.accesscontrol.identityproviderredirect.IdentityProviderRedirectAdapter;


public interface AuthorizationMethod {

    /**
     * Checks if the given credentials are valid.
     *
     * @return the corresponding Username topic if the credentials are valid, or <code>null</code> otherwise.
     */
    Topic checkCredentials(Credentials cred);

    /**
     * Return the {@link IdentityProviderRedirectAdapter} instance of this
     * authorization method, when available or <code>null</code>, when not.
     *
     * @return the instance or <code>null</code>
     */
    default IdentityProviderRedirectAdapter getIdentityProviderRedirectAdapter() {
        return null;
    }
}
