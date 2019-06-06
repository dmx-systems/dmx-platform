package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;



public interface ServiceRequestFilter extends EventListener {

    void serviceRequestFilter(ContainerRequest containerRequest);
}
