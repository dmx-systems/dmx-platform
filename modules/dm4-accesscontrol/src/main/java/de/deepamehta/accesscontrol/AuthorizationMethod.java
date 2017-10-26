package de.deepamehta.accesscontrol;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.accesscontrol.Credentials;



public interface AuthorizationMethod {

    /**
     * Checks if the given credentials are valid.
     *
     * @return  the corresponding Username topic if the credentials are valid, or <code>null</code> otherwise.
     */
    Topic checkCredentials(Credentials cred);
}
