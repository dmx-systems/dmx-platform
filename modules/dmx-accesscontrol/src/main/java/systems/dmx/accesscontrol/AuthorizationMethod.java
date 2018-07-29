package systems.dmx.accesscontrol;

import systems.dmx.core.Topic;
import systems.dmx.core.service.accesscontrol.Credentials;



public interface AuthorizationMethod {

    /**
     * Checks if the given credentials are valid.
     *
     * @return  the corresponding Username topic if the credentials are valid, or <code>null</code> otherwise.
     */
    Topic checkCredentials(Credentials cred);
}
