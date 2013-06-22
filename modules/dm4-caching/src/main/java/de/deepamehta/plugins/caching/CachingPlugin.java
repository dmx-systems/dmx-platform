package de.deepamehta.plugins.caching;

import de.deepamehta.plugins.time.service.TimeService;

import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.PreProcessRequestListener;

// ### TODO: remove Jersey dependency. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class CachingPlugin extends PluginActivator implements PreProcessRequestListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String CACHABLE_PATH = "core/(topic|association)/(\\d+)";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TimeService timeService;
    private Pattern cachablePath;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        cachablePath = Pattern.compile(CACHABLE_PATH);
    }

    // ---

    @Override
    @ConsumesService("de.deepamehta.plugins.time.service.TimeService")
    public void serviceArrived(PluginService service) {
        timeService = (TimeService) service;
    }

    @Override
    public void serviceGone(PluginService service) {
        timeService = null;
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preProcessRequest(ContainerRequest request) {
        long objectId = objectId(request);
        if (objectId != -1) {
            long time = timeService.getTimeModified(objectId);
            if (time != -1) {
                Response.ResponseBuilder response = request.evaluatePreconditions(new Date(time));
                if (response != null) {
                    logger.fine("### Precondition for object " + objectId + " not met");
                    throw new WebApplicationException(response.build());
                }
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private long objectId(ContainerRequest request) {
        // Example URL: "http://localhost:8080/core/topic/2695?fetch_composite=false"
        //   request.getBaseUri()="http://localhost:8080/"
        //   request.getPath()="core/topic/2695"
        //   request.getAbsolutePath()="http://localhost:8080/core/topic/2695"
        //   request.getRequestUri()="http://localhost:8080/core/topic/2695?fetch_composite=false"
        Matcher m = cachablePath.matcher(request.getPath());
        return m.matches() ? Long.parseLong(m.group(2)) : -1;
        // Note: content of group 1 is "topic" or "association"
    }
}
