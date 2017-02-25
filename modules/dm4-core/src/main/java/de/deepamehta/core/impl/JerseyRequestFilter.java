package de.deepamehta.core.impl;

import de.deepamehta.core.service.Cookies;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;



class JerseyRequestFilter implements ContainerRequestFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TransactionFactory tf;
    private EventManager em;

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyRequestFilter(TransactionFactory tf, EventManager em) {
        this.tf = tf;
        this.em = em;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Cookies.set(request);
        tf.createTx(request);
        em.fireEvent(CoreEvent.SERVICE_REQUEST_FILTER, request);
        return request;
        // Note: we don't catch here as a WebApplicationException (e.g. thrown by
        // CachingPlugin's serviceRequestFilter()) must reach the exception mapper
    }
}
