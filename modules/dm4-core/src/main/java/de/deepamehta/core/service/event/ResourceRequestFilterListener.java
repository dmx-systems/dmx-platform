package de.deepamehta.core.service.event;

import de.deepamehta.core.service.Listener;

import javax.servlet.http.HttpServletRequest;



public interface ResourceRequestFilterListener extends Listener {

    void resourceRequestFilter(HttpServletRequest servletRequest);
}
