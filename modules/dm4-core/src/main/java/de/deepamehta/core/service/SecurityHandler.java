package de.deepamehta.core.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public interface SecurityHandler {
    boolean handleSecurity(HttpServletRequest request, HttpServletResponse response);
}
