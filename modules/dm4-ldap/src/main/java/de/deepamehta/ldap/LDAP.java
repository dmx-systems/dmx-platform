package de.deepamehta.ldap;


import de.deepamehta.accesscontrol.AuthorizationMethod;
import de.deepamehta.accesscontrol.AccessControlService;

import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.Topic;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.Control;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Hashtable;


public class LDAP extends PluginActivator implements AuthorizationMethod {

    private Logger logger = Logger.getLogger(getClass().getName());
    private static final String LDAP_SERVER = System.getProperty("dm4.ldap.server", "127.0.0.1");
    private static final String LDAP_PORT = System.getProperty("dm4.ldap.port", "389");
    private static final String LDAP_MANAGER = System.getProperty("dm4.ldap.manager", "");
    private static final String LDAP_PASSWORD = System.getProperty("dm4.ldap.password", "");
    private static final String LDAP_USER_BASE = System.getProperty("dm4.ldap.user_base", "");
    private static final String LDAP_USER_ATTRIBUTE = System.getProperty("dm4.ldap.user_attribute", "");
    private static final String LDAP_FILTER = System.getProperty("dm4.ldap.filter", "");

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
        if (checkLdapCredentials(cred.username, cred.plaintextPassword)) {
            logger.info("LDAP login: OK");
            Topic usernameTopic = acs.getUsernameTopic(cred.username);
            if (usernameTopic != null) {
                return usernameTopic;
            } else {
                return createUserAccount(cred);
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createUserAccount (Credentials cred) {
        DeepaMehtaTransaction tx = dm4.beginTx();
        try {
            Topic usernameTopic = acs.createUserAccount(cred);
            tx.success();
            return usernameTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK! (" + this + ")");
            throw new RuntimeException("Creating user account failed", e);
        } finally {
            tx.finish();
        }
    }

    private boolean checkLdapCredentials (String username, String password) {
        try {
            final String server = "ldap://" + LDAP_SERVER + ":" + LDAP_PORT;
            LdapContext ctx = connect(server, LDAP_MANAGER, LDAP_PASSWORD);
            String cn = lookupUserCn(ctx, LDAP_USER_BASE, username);
            LdapContext ctx2 = connect(server, cn, password);
            return ctx2 != null;
        } catch (Exception e) {

        }
        return false;
    }

    private LdapContext connect(String server, String username, String password) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, server);
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        //ensures that objectSID attribute values
        //will be returned as a byte[] instead of a String
        env.put("java.naming.ldap.attributes.binary", "objectSID");
        
        // the following is helpful in debugging errors
        env.put("com.sun.jndi.ldap.trace.ber", System.err);
        Control[] arr = new Control[0];
        LdapContext ctx = new InitialLdapContext(env, arr);
        return ctx;
    }

    private static String lookupUserCn (LdapContext ctx, String ldapSearchBase, String uid) throws NamingException {
        String searchFilter = "(&(objectClass=inetOrgPerson)(uid=" + uid + "))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);
        SearchResult searchResult = null;
        if(results.hasMoreElements()) {
            searchResult = (SearchResult) results.nextElement();
            //make sure there is not another item available, there should be only 1 match
            if(results.hasMoreElements()) {
                System.err.println("Matched multiple users for the accountName: " + uid);
                return null; // throw some exception?
            }
            return searchResult.getNameInNamespace();
        } else {
            return null;
        }
    }
}
