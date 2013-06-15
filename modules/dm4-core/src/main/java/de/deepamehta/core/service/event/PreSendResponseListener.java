package de.deepamehta.core.service.event;

import de.deepamehta.core.service.Listener;

// ### TODO: remove Jersey dependency. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerResponse;



public interface PreSendResponseListener extends Listener {

    void preSendResponse(ContainerResponse response);
}
