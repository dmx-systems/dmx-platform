package de.deepamehta.core.service.event;

import de.deepamehta.core.service.Listener;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerResponse;



public interface ServiceResponseFilterListener extends Listener {

    void serviceResponseFilter(ContainerResponse response);
}
