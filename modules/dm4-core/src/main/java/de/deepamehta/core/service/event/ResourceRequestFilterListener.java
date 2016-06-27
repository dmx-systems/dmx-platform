package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public interface ResourceRequestFilterListener extends EventListener {

    void resourceRequestFilter(HttpServletRequest request, HttpServletResponse response);
}
