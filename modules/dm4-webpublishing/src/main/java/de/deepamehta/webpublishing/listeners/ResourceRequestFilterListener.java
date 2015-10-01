package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.service.EventListener;

import javax.servlet.http.HttpServletRequest;



public interface ResourceRequestFilterListener extends EventListener {

    void resourceRequestFilter(HttpServletRequest servletRequest);
}
