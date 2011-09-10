package de.deepamehta.plugins.proxy;

import de.deepamehta.plugins.proxy.model.DirectoryListing;
import de.deepamehta.plugins.proxy.model.Resource;
import de.deepamehta.plugins.proxy.model.ResourceInfo;
import de.deepamehta.plugins.proxy.service.ProxyService;

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

import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;



@Path("/")
public class ProxyPlugin extends Plugin implements ProxyService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.proxy.files.path");
    private static final String REMOTE_ACCESS_FILTER = System.getProperty("dm4.proxy.net.filter");

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
    @Override
    public Resource getResource(@PathParam("uri") URL uri, @QueryParam("type") String mediaType,
                                                           @QueryParam("size") long size) {
        logger.info("Requesting resource \"" + uri + "\" (mediaType=\"" + mediaType + "\", size=" + size + ")");
        //
        checkRemoteAccess(request);
        //
        if (uri.getProtocol().equals("file")) {
            uri = fileRepositoryURL(uri.getPath());
            File file = new File(uri.getPath());
            if (file.isDirectory()) {
                return new Resource(new DirectoryListing(file));
            }
        }
        return new Resource(uri, mediaType, size);
    }

    @GET
    @Path("/{uri}/info")
    @Produces("application/json")
    @Override
    public ResourceInfo getResourceInfo(@PathParam("uri") URL uri) {
        logger.info("Requesting info for resource \"" + uri + "\"");
        //
        checkRemoteAccess(request);
        //
        return new ResourceInfo(uri);
    }

    // ---

    @Override
    public File locateFile(String relativePath) {
        File file = new File(FILE_REPOSITORY_PATH, relativePath);
        //
        checkFileAccess(file);
        //
        return file;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void checkRemoteAccess(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isInRange = JavaUtils.isInRange(remoteAddr, REMOTE_ACCESS_FILTER);
        //
        logger.info("Checking remote access to \"" + request.getRequestURL() + "\"\n      remote address=\"" +
            remoteAddr + "\", range=\"" + REMOTE_ACCESS_FILTER + "\" => " + (isInRange ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!isInRange) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    private void checkFileAccess(File file) {
        try {
            // Note: a directory path returned by getCanonicalPath() never contains a "/" at the end.
            // Thats why "dm4.proxy.files.path" is expected to have no "/" at the end as well.
            String path = file.getCanonicalPath();
            boolean pointsToRepository = path.startsWith(FILE_REPOSITORY_PATH);
            //
            logger.info("Checking file repository access to \"" + file.getPath() + "\"\n      dm4.proxy.files.path=" +
                "\"" + FILE_REPOSITORY_PATH + "\", canonical request path=\"" + path + "\" => " +
                (pointsToRepository ? "ALLOWED" : "FORBIDDEN"));
            //
            if (!pointsToRepository) {
                throw new WebApplicationException(Status.FORBIDDEN);
            }
        } catch (IOException e) {
            throw new RuntimeException("Checking file repository access failed (file=\"" + file + "\")", e);
        }
    }

    // ---

    private URL fileRepositoryURL(String relativePath) {
        try {
            URL url = new URL("file:" + locateFile(relativePath));
            logger.info("Mapping request to file repository URL \"" + url + "\"");
            return url;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Constructing the file repository URL failed", e);
        }
    }
}
