package systems.dmx.caching;

import systems.dmx.timestamps.TimestampsService;

import systems.dmx.core.DMXObject;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.event.ServiceRequestFilter;
import systems.dmx.core.service.event.ServiceResponseFilter;
import systems.dmx.core.util.JavaUtils;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Path("/cache")
public class CachingPlugin extends PluginActivator implements ServiceRequestFilter, ServiceResponseFilter {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String CACHABLE_PATH = "core/(topic|association)/(\\d+)";

    private static String HEADER_CACHE_CONTROL = "Cache-Control";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private TimestampsService timestampsService;

    @Context
    HttpServletRequest req;

    private Pattern cachablePath = Pattern.compile(CACHABLE_PATH);

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // Listeners

    @Override
    public void serviceRequestFilter(ContainerRequest request) {
        long objectId = requestObjectId(request);
        if (objectId != -1) {
            if (timestampsService == null) {
                throw new RuntimeException("Time service is not available");
            }
            //
            long time = timestampsService.getModificationTime(objectId);
            Response.ResponseBuilder builder = request.evaluatePreconditions(new Date(time));
            if (builder != null) {
                Response response = builder.build();
                Response.Status status = Response.Status.fromStatusCode(response.getStatus());
                logger.fine("### Preconditions of request \"" + JavaUtils.requestInfo(req) +
                    "\" are not met -- Responding with " + JavaUtils.responseInfo(status));
                throw new WebApplicationException(response);
            }
        }
    }

    @Override
    public void serviceResponseFilter(ContainerResponse response) {
        DMXObject object = responseObject(response);
        if (object != null) {
            setCacheControlHeader(response, "no-store");
            // ### FIXME: support browser cache by using "max-age=0" instead (#35)
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private long requestObjectId(ContainerRequest request) {
        // Example URL: "http://localhost:8080/core/topic/2695?children=true"
        //   request.getBaseUri()="http://localhost:8080/"
        //   request.getPath()="core/topic/2695"
        //   request.getAbsolutePath()="http://localhost:8080/core/topic/2695"
        //   request.getRequestUri()="http://localhost:8080/core/topic/2695?children=true"
        Matcher m = cachablePath.matcher(request.getPath());
        if (m.matches()) {
            long objectId = Long.parseLong(m.group(2));
            //
            String objectType = m.group(1);     // group 1 is "topic" or "association"
            if (!objectType.equals("topic") && !objectType.equals("association")) {
                throw new RuntimeException("Unexpected object type: \"" + objectType + "\"");
            }
            //
            return objectId;
        } else {
            return -1;
        }
    }

    // ---

    // ### FIXME: copy in TimestampsPlugin
    private DMXObject responseObject(ContainerResponse response) {
        Object entity = response.getEntity();
        return entity instanceof DMXObject ? (DMXObject) entity : null;
    }

    private void setCacheControlHeader(ContainerResponse response, String value) {
        setHeader(response, HEADER_CACHE_CONTROL, value);
    }

    // ### FIXME: copy in TimestampsPlugin
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
