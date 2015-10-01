package de.deepamehta.webpublishing.listeners;

import de.deepamehta.core.service.EventListener;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;



public interface ServiceRequestFilterListener extends EventListener {

    void serviceRequestFilter(ContainerRequest containerRequest);
}
