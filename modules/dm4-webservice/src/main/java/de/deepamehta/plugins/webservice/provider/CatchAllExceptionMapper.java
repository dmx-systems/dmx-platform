package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// ### import java.io.PrintWriter;
// ### import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * We don't want Jersey to re-throw anything to the HTTP container as this would result in logging
 * the exception twice and possibly in interspersed illegible stack traces (see #484).
 * <p>
 * This mapper maps <i>all</i> Throwables to a suitable response.
 * <p>
 * 2 additional aspects are handled:
 *   - Logging the exception.
 *   - Enriching the response with an exception info entity.
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
        Response response;
        if (e instanceof WebApplicationException) {
            response = ((WebApplicationException) e).getResponse();
            Status status = Status.fromStatusCode(response.getStatus());
            Family family = status.getFamily();
            // Don't log redirects like 304 Not Modified
            if (family == Family.CLIENT_ERROR || family == Family.SERVER_ERROR) {
                Throwable cause = e.getCause();
                Throwable originalException = cause != null ? cause : e;
                logError(status, originalException);
                // Only set entity if not already provided by application
                if (response.getEntity() == null) {
                    response = errorResponse(Response.fromResponse(response), originalException);
                }
            }
        } else {
            // build generic response
            Status status = hasNestedAccessControlException(e) ? Status.UNAUTHORIZED : Status.INTERNAL_SERVER_ERROR;
            logError(status, e);
            response = errorResponse(Response.status(status), e);
        }
        return response;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void logError(Status status, Throwable e) {
        logger.log(Level.SEVERE, "Request \"" + JavaUtils.requestInfo(request) + "\" failed. Responding with " +
            JavaUtils.responseInfo(status) + ". The original exception/error is:", e);
    }

    private Response errorResponse(ResponseBuilder builder, Throwable e) {
        return builder.type(MediaType.APPLICATION_JSON).entity(new ExceptionInfo(e)).build();
    }

    private boolean hasNestedAccessControlException(Throwable e) {
        while (e != null) {
            if (e instanceof AccessControlException) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    /* ### private String toJSON(Throwable e) {
        try {
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            return new JSONObject().put("exception", out).toString();
        } catch (JSONException je) {
            throw new RuntimeException("Generating exception info failed", je);
        }
    } */

    // --------------------------------------------------------------------------------------------------- Private Class

    private static class ExceptionInfo implements JSONEnabled {

        private JSONObject json;

        private ExceptionInfo(Throwable e) {
            try {
                json = createJSONObject(e);
            } catch (JSONException je) {
                throw new RuntimeException("Generating exception info failed", je);
            }
        }

        private JSONObject createJSONObject(Throwable e) throws JSONException {
            JSONObject json = new JSONObject()
                .put("exception", e.getClass().getName())
                .put("message", e.getMessage());
            //
            Throwable cause = e.getCause();
            if (cause != null) {
                json.put("cause", createJSONObject(cause));
            }
            //
            return json;
        }

        @Override
        public JSONObject toJSON() {
            return json;
        }
    }
}
