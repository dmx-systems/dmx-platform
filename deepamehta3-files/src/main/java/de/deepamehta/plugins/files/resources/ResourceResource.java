package de.deepamehta.plugins.files.resources;

import de.deepamehta.plugins.files.model.DirectoryListing;
import de.deepamehta.plugins.files.model.ResourceInfo;

import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;



@Path("/")
public class ResourceResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private @Context HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @GET
    @Path("/{uri}")
    public Response getResource(@PathParam("uri") URL uri, @QueryParam("type") String type,
                                                           @QueryParam("size") long size) throws Exception {
        logger.info("Requesting resource " + uri + " (type=\"" + type + "\", size=" + size + ")");
        if (isRequestAllowed(request)) {
            if (uri.getProtocol().equals("file")) {
                File file = new File(uri.getPath());
                if (file.isDirectory()) {
                    return Response.ok(new DirectoryListing(file), "application/json").build();
                }
            }
            return resource(uri, type, size);
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/{uri}/info")
    @Produces("application/json")
    public Response getResourceInfo(@PathParam("uri") URL uri) throws Exception {
        logger.info("Requesting resource info for " + uri);
        if (isRequestAllowed(request)) {
            return Response.ok(new ResourceInfo(uri)).build();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Response resource(URL uri, String type, long size) throws Exception {
        InputStream in = uri.openStream();
        ResponseBuilder builder = Response.ok(in);
        if (type != null) {
            builder.type(type);
        }
        if (size != 0) {
            builder.header("Content-Length", size);
        }
        return builder.build();
    }

    // ---

    private boolean isRequestAllowed(HttpServletRequest request) {
        String localAddr = request.getLocalAddr();
        String remoteAddr = request.getRemoteAddr();
        boolean allowed = localAddr.equals(remoteAddr);
        logger.info("local address: " + localAddr + ", remote address: " + remoteAddr +
            " => " + (allowed ? "ALLOWED" : "FORBIDDEN"));
        return allowed;
    }
}
