package de.deepamehta.plugins.accesscontrol;

import javax.servlet.http.HttpServletRequest;



interface SecurityContext {

    void checkRequest(HttpServletRequest request) throws AccessControlException;
}
