package de.deepamehta.plugins.webservice.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



/**
 * Maps all Throwables but WebApplicationExceptions to a 500 (Internal Server Error) response.
 * A WebApplicationException's response is returned directly.
 * <p>
 * We don't want Jersey to re-throw anything to the HTTP container as this would result in logging
 * the exception twice and possibly to interspersed illegible stack traces (see #484).
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Throwable> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private UriInfo uriInfo;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        } else {
            logger.log(Level.SEVERE, "Processing HTTP request " + uriInfo.getRequestUri() + " failed. " +
                "Generating a 500 response (Internal Server Error). The original exception/error is:", e);
            return Response.serverError().build();
        }
    }
}
