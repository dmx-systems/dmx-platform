package de.deepamehta.core.impl;

import de.deepamehta.core.service.RequestContext;

import javax.servlet.http.HttpServletRequest;



class RequestContextImpl implements RequestContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private HttpServletRequest request;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RequestContextImpl(HttpServletRequest request) {
        this.request = request;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }
}
