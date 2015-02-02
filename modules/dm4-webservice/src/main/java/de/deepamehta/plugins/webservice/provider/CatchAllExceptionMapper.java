package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.util.JavaUtils;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;



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
    HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        }
        //
        Status status;
        if (hasNestedAccessControlException(e)) {
            status = Status.UNAUTHORIZED;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        logger.log(Level.SEVERE, errorMessage(status), e);
        return Response.status(status).build();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean hasNestedAccessControlException(Throwable e) {
        while (e != null) {
            if (e instanceof AccessControlException) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private String errorMessage(Status status) {
        return "Request \"" + JavaUtils.requestInfo(request) + "\" failed. Generating " +
            JavaUtils.responseInfo(status) + ". The original exception/error is:";
    }
}
