package de.deepamehta.core.impl;

import de.deepamehta.core.service.Cookies;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;



class JerseyRequestFilter implements ContainerRequestFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EventManager em;

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyRequestFilter(EventManager em) {
        this.em = em;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Cookies.set(request);
        em.fireEvent(CoreEvent.SERVICE_REQUEST_FILTER, request);
        return request;
    }
}
