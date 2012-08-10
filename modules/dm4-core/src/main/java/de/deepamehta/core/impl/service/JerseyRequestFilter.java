package de.deepamehta.core.impl.service;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import java.util.logging.Logger;



class JerseyRequestFilter implements ContainerRequestFilter {

    private Logger logger = Logger.getLogger(getClass().getName());

    public ContainerRequest filter(ContainerRequest request) {
        logger.info("########## " + request.getRequestUri());
        return request;
    }
}
