package de.deepamehta.webpublishing.impl;

import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.DeepaMehtaService;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;



class JerseyRequestFilter implements ContainerRequestFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyRequestFilter(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        Cookies.set(request);
        dms.fireEvent(WebPublishingEvents.SERVICE_REQUEST_FILTER, request);
        return request;
    }
}
