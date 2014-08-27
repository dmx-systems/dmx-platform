package de.deepamehta.core.impl;

import de.deepamehta.core.service.Cookies;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;



class JerseyRequestFilter implements ContainerRequestFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyRequestFilter(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Cookies.set(request);
        dms.fireEvent(CoreEvent.SERVICE_REQUEST_FILTER, request);
        return request;
    }
}
