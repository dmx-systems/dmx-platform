package de.deepamehta.plugins.webservice.provider;

import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException e) {
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        } else {
            // All other runtime exceptions are wrapped in a WebApplicationException in order to let Jersey handle it.
            // Otherwise Jersey would re-throw the exception to the servlet container yielding to an interspersed
            // illegible stack trace. See #484.
            throw new WebApplicationException(e);
        }
    }
}
