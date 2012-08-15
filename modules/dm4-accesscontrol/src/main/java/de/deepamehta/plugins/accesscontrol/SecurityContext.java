package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.core.Topic;
import javax.servlet.http.HttpServletRequest;


interface SecurityContext {

    Topic login(String username, String password, HttpServletRequest request);
}
