package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.core.Topic;
import javax.servlet.http.HttpServletRequest;


interface SecurityContext {

    boolean isLoginRequired(HttpServletRequest request);

    Topic login(String username, String password, HttpServletRequest request);
}
