package de.deepamehta.plugins.files.resources;

import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @GET
    @Path("/{uri}")
    public Response getResource(@PathParam("uri") URL uri, @Context HttpServletRequest request,
                                                           @QueryParam("type") String type,
                                                           @QueryParam("size") long size) throws Exception {
        logger.info("Requesting resource " + uri + " (type=\"" + type + "\", size=" + size + ")");
        if (isRequestAllowed(request)) {
            if (uri.getProtocol().equals("file")) {
                File file = new File(uri.getPath());
                if (file.isDirectory()) {
                    return directoryResource(file);
                }
            }
            return resource(uri, type, size);
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/{uri}/info")
    public Response getResourceInfo(@PathParam("uri") URL uri, @Context HttpServletRequest request) throws Exception {
        logger.info("Requesting resource info for " + uri);
        if (isRequestAllowed(request)) {
            return resourceInfo(uri);
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

    private Response directoryResource(File directory) throws Exception {
        JSONObject dir = new JSONObject();
        JSONArray items = new JSONArray();
        dir.put("kind", "directory");
        dir.put("name", directory.getName());
        dir.put("path", directory.getPath());
        dir.put("items", items);
        //
        for (File file : directory.listFiles()) {
            JSONObject item = new JSONObject();
            item.put("name", file.getName());
            item.put("path", file.getPath());
            if (file.isDirectory()) {
                item.put("kind", "directory");
            } else {
                item.put("kind", "file");
                item.put("size", file.length());
                item.put("type", JavaUtils.getFileType(file.getName()));
            }
            items.put(item);
        }
        return Response.ok(dir, "application/json").build();
    }

    private Response resourceInfo(URL uri) throws Exception {
        JSONObject info = new JSONObject();
        if (uri.getProtocol().equals("file")) {
            File file = new File(uri.getPath());
            if (!file.exists()) {
                info.put("error", "not found");
            } else if (file.isDirectory()) {
                info.put("kind", "directory");
            } else {
                info.put("kind", "file");
            }
        } else {
            info.put("kind", "remote");
        }
        return Response.ok(info).build();
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
