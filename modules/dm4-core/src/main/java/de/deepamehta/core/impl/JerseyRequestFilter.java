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
        try {
            Cookies.set(request);
            tf.create(request);
            em.fireEvent(CoreEvent.SERVICE_REQUEST_FILTER, request);
            return request;
        } catch (Exception e) {
            throw new RuntimeException("Request filtering failed", e);
        }
    }
}
