package de.deepamehta.plugins.webservice.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Response toResponse(RuntimeException e) {
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        } else {
            // Note: we throw no non-WAE exception from here. Otherwise Jersey would re-throw the exception
            // to the servlet container yielding to an interspersed illegible stack trace. See #484.
            logger.log(Level.SEVERE, "A resource method or event listener threw a RuntimeException. " +
                "Mapped exception to response: 500 (Internal Server Error).", e);
            return Response.serverError().build();
        }
    }
}
