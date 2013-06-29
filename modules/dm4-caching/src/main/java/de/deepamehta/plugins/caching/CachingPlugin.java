package de.deepamehta.plugins.caching;

import de.deepamehta.plugins.time.service.TimeService;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.service.event.PreProcessRequestListener;
import de.deepamehta.core.service.event.PreSendResponseListener;

// ### TODO: remove Jersey dependency. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class CachingPlugin extends PluginActivator implements PreProcessRequestListener, PreSendResponseListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String CACHABLE_PATH = "core/(topic|association)/(\\d+)";

    private static String HEADER_CACHE_CONTROL = "Cache-Control";

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
                    logger.info("### Precondition for object " + objectId + " not met");
                    throw new WebApplicationException(response.build());
                }
            }
        }
    }

    @Override
    public void preSendResponse(ContainerResponse response) {
        DeepaMehtaObject object = responseObject(response);
        if (object != null) {
            setCacheControlHeader(response, "max-age=0");
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

    // ---

    // ### FIXME: copy in TimePlugin
    private DeepaMehtaObject responseObject(ContainerResponse response) {
        Object entity = response.getEntity();
        return entity instanceof DeepaMehtaObject ? (DeepaMehtaObject) entity : null;
    }

    private void setCacheControlHeader(ContainerResponse response, String directives) {
        setHeader(response, HEADER_CACHE_CONTROL, directives);
    }

    // ### FIXME: copy in TimePlugin
    private void setHeader(ContainerResponse response, String header, String value) {
        MultivaluedMap headers = response.getHttpHeaders();
        //
        if (headers.containsKey(header)) {
            throw new RuntimeException("Response already has a \"" + header + "\" header");
        }
        //
        headers.putSingle(header, value);
    }
}
