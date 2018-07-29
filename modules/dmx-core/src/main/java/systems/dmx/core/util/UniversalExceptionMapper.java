package systems.dmx.core.util;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.service.accesscontrol.AccessControlException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Maps exceptions to suitable HTTP responses.
 * <p>
 * Supports both, JAX-RS responses and Servlet API responses.
 * <p>
 * 2 additional aspects are handled:
 *   - Logging the exception.
 *   - Enriching the response with an error entity.
 */
public class UniversalExceptionMapper {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: status 405 is not defined by JAX-RS
    private static StatusType METHOD_NOT_ALLOWED = new StatusType() {
        @Override public int getStatusCode() {return 405;}
        @Override public String getReasonPhrase() {return "Method Not Allowed";}
        @Override public Family getFamily() {return Family.CLIENT_ERROR;}
    };

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Throwable e;
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UniversalExceptionMapper(Throwable e, HttpServletRequest request) {
        this.e = e;
        this.request = request;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Response toResponse() {
        Response response;
        if (e instanceof WebApplicationException) {
            response = ((WebApplicationException) e).getResponse();
            StatusType status = fromStatusCode(response.getStatus());
            Family family = status.getFamily();
            // Don't log redirects like 304 Not Modified
            if (family == Family.CLIENT_ERROR || family == Family.SERVER_ERROR) {
                Throwable cause = e.getCause();
                Throwable originalException = cause != null ? cause : e;
                logException(status, originalException);
                // Only set entity if not already provided by application
                if (response.getEntity() == null) {
                    response = errorResponse(Response.fromResponse(response), originalException);
                }
            }
        } else {
            // build generic response
            StatusType status = hasNestedAccessControlException(e) ? Status.UNAUTHORIZED : Status.INTERNAL_SERVER_ERROR;
            logException(status, e);
            response = errorResponse(Response.status(status), e);
        }
        return response;
    }

    public void initResponse(HttpServletResponse response) throws IOException {
        transferResponse(toResponse(), response);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void logException(StatusType status, Throwable e) {
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

    private StatusType fromStatusCode(int statusCode) {
        StatusType status;
        if (statusCode == 405) {
            status = METHOD_NOT_ALLOWED;
        } else {
            status = Status.fromStatusCode(statusCode);
            if (status == null) {
                throw new RuntimeException(statusCode + " is an unexpected status code");
            }
        }
        return status;
    }

    // ---

    /**
     * Transfers status code, headers, and entity of a JAX-RS Response to a HttpServletResponse and sends the response.
     */
    private void transferResponse(Response response, HttpServletResponse servletResponse) throws IOException {
        // status code
        servletResponse.setStatus(response.getStatus());
        // headers
        MultivaluedMap<String, Object> metadata = response.getMetadata();
        for (String header : metadata.keySet()) {
            for (Object value : metadata.get(header)) {
                servletResponse.addHeader(header, value.toString());
            }
        }
        // entity
        servletResponse.getWriter().write(response.getEntity().toString());     // throws IOException
        // servletResponse.sendError(response.getStatus(), (String) response.getEntity()); // throws IOException
    }

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
            String message = e.getMessage();    // may be null
            JSONObject json = new JSONObject()
                .put("exception", e.getClass().getName())
                .put("message", message != null ? message : "");
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

        @Override
        public String toString() {
            return json.toString();
        }
    }
}
