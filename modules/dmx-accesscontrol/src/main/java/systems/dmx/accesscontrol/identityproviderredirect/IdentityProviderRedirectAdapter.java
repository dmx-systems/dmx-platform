package systems.dmx.accesscontrol.identityproviderredirect;

import javax.servlet.http.HttpSession;
import java.net.URI;

public interface IdentityProviderRedirectAdapter {
    String getName();

    String getLabel();

    URI getImageUri();

    URI createIdentityProviderRedirectUri(SingleUseAuthenticator singleUseAuthenticator);

    URI createLogoutUri(HttpSession httpSession);
}
