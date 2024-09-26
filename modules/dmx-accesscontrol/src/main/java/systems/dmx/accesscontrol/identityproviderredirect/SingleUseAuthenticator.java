package systems.dmx.accesscontrol.identityproviderredirect;

/**
 * Callback interface that allows a single login to be performed after a
 * successful authentication with an identity provider took place.
 */
public interface SingleUseAuthenticator {
    void authenticate(IdentityAssertion identityAssertion) throws SingleUseAuthenticationFailedException;
}
