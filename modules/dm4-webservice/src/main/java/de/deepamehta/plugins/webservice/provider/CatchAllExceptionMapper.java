package de.deepamehta.plugins.webservice.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



/**
 * Maps all Throwables but WebApplicationExceptions to a 500 (Internal Server Error) response.
 * A WebApplicationException's response is returned directly.
 * <p>
 * We don't want Jersey to re-throw anything against the servlet container as this would result
 * in an interspersed illegible stack trace (see #484).
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Throwable> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        } else {
            logger.log(Level.SEVERE, "A DeepaMehta resource method or event listener threw an exception resp. " +
                "an error occurred. Mapping exception/error to response: 500 (Internal Server Error).", e);
            return Response.serverError().build();
        }
    }
}
