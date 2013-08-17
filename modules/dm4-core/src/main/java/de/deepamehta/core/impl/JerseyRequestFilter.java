package de.deepamehta.core.impl;

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
        dms.fireEvent(CoreEvent.SERVICE_REQUEST_FILTER, request);
        return request;
    }
}
