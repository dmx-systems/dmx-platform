package de.deepamehta.ldap;

import de.deepamehta.accesscontrol.AuthorizationMethod;
import de.deepamehta.accesscontrol.AccessControlService;

import de.deepamehta.core.service.accesscontrol.Credentials;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.Control;


import de.deepamehta.core.service.CoreService;

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
        System.out.println("------ LDAP.checkCredentials ------");
        System.out.println(cred.username);
        System.out.println(cred.plaintextPassword);
        if (checkLdapCredentials(cred.username, cred.plaintextPassword)) {
            System.out.println("LDAP login: OK");
            Topic usernameTopic = acs.getUsernameTopic(cred.username);
            System.out.println("Got Topic");
            if (usernameTopic != null) {
                System.out.println("return existing Topic");
                return usernameTopic;
            } else {
                System.out.println("create Topic");
                return acs.createUserAccount(cred);
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean checkLdapCredentials (String username, String password) {
        try {
            final String server = "ldap://127.0.0.1:10389";
            final String adminUsername = "uid=admin,ou=system";
            final String adminPassword = "secret";
            final String searchBase = "ou=users,o=mojo";
            LdapContext ctx = connect(server, adminUsername, adminPassword);
            String cn = lookupUserCn(ctx, searchBase, username);
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
