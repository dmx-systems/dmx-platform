package de.deepamehta.ldap;

import de.deepamehta.accesscontrol.AuthorizationMethod;
import de.deepamehta.accesscontrol.AccessControlService;

import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;


public class LDAP extends PluginActivator implements AuthorizationMethod {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acs;



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        acs.registerAuthorizationMethod("ldap", this);
    }


    // ******************************************
    // *** AuthorizationMethod implementation ***
    // ******************************************



    public Topic checkCredentials(Credentials cred) {
        return null;
    }
}
