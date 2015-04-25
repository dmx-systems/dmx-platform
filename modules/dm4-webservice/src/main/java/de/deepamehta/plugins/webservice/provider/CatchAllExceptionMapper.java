package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// ### import java.io.PrintWriter;
// ### import java.io.StringWriter;
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
        Status status = hasNestedAccessControlException(e) ? Status.UNAUTHORIZED : Status.INTERNAL_SERVER_ERROR;
        //
        logger.log(Level.SEVERE, errorMessage(status), e);
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new ExceptionInfo(e)).build();
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
        return "Request \"" + JavaUtils.requestInfo(request) + "\" failed. Responding with " +
            JavaUtils.responseInfo(status) + ". The original exception/error is:";
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
                JSONArray causes = new JSONArray();
                json = createJSONObject(e);
                json.put("causes", causes);
                while ((e = e.getCause()) != null) {
                    causes.put(createJSONObject(e));
                }
            } catch (JSONException je) {
                throw new RuntimeException("Generating exception info failed", je);
            }
        }

        private JSONObject createJSONObject(Throwable e) throws JSONException {
            return new JSONObject()
                .put("exception", e.getClass().getName())
                .put("message", e.getMessage());
        }

        @Override
        public JSONObject toJSON() {
            return json;
        }
    }
}
