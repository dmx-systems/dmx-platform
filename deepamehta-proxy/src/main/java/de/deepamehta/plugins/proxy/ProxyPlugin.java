package de.deepamehta.plugins.proxy;

import de.deepamehta.plugins.proxy.model.DirectoryListing;
import de.deepamehta.plugins.proxy.model.Resource;
import de.deepamehta.plugins.proxy.model.ResourceInfo;
import de.deepamehta.plugins.proxy.service.ProxyService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.CommandParams;
import de.deepamehta.core.service.CommandResult;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.util.JavaUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import javax.servlet.http.HttpServletRequest;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



@Path("/")
public class ProxyPlugin extends Plugin implements ProxyService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************
    // *** Plugin Service ***
    // **********************



    @GET
    @Path("/{uri}")
    public Resource getResource(@PathParam("uri") URL uri, @QueryParam("type") String mediaType,
                                                           @QueryParam("size") long size) {
        logger.info("Retrieving resource " + uri + " (mediaType=\"" + mediaType + "\", size=" + size + ")");
        if (isRequestAllowed(request)) {
            if (uri.getProtocol().equals("file")) {
                File file = new File(uri.getPath());
                if (file.isDirectory()) {
                    return new Resource(new DirectoryListing(file));
                }
            }
            return new Resource(uri, mediaType, size);
        } else {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    @GET
    @Path("/{uri}/info")
    @Produces("application/json")
    public ResourceInfo getResourceInfo(@PathParam("uri") URL uri) {
        logger.info("Requesting resource info for " + uri);
        if (isRequestAllowed(request)) {
            return new ResourceInfo(uri);
        } else {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean isRequestAllowed(HttpServletRequest request) {
        String localAddr = request.getLocalAddr();
        String remoteAddr = request.getRemoteAddr();
        boolean allowed = localAddr.equals(remoteAddr);
        logger.info(request.getRequestURL() + "\nlocal address: " + localAddr + ", remote address: " + remoteAddr +
            " => " + (allowed ? "ALLOWED" : "FORBIDDEN"));
        return allowed;
    }
}
