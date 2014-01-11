package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerResponse;



public interface ServiceResponseFilterListener extends EventListener {

    void serviceResponseFilter(ContainerResponse response);
}
