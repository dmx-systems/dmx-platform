package de.deepamehta.core.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



// ### TODO: to be dropped?
public interface SecurityHandler {
    boolean handleSecurity(HttpServletRequest request, HttpServletResponse response);
}
