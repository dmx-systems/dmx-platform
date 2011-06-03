package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.model.DirectoryListing;
import de.deepamehta.plugins.files.model.Resource;
import de.deepamehta.plugins.files.model.ResourceInfo;
import de.deepamehta.plugins.files.service.FilesService;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.model.CommandResult;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.Relation;
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
public class FilesPlugin extends Plugin implements FilesService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private @Context HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



    @Override
    public CommandResult executeCommandHook(String command, CommandParams params, ClientContext clientContext) {
        if (command.equals("deepamehta3-files.open-file")) {
            long fileTopicId = params.getInt("topic_id");    // topic_id deserializes as Integer (not Long)
            openFile(fileTopicId);
            return new CommandResult("message", "OK");
        } else if (command.equals("deepamehta3-files.create-file-topic")) {
            String path = params.getString("path");
            try {
                return new CommandResult(createFileTopic(path).toJSON());
            } catch (Throwable e) {
                throw new RuntimeException("Error while creating file topic for \"" + path + "\"", e);
            }
        } else if (command.equals("deepamehta3-files.create-folder-topic")) {
            String path = (String) params.getString("path");
            try {
                return new CommandResult(createFolderTopic(path).toJSON());
            } catch (Throwable e) {
                throw new RuntimeException("Error while creating folder topic for \"" + path + "\"", e);
            }
        }
        return null;
    }



    // ***********************
    // *** Command Handler ***
    // ***********************



    public void openFile(long fileTopicId) {
        String path = null;
        try {
            path = dms.getTopicProperty(fileTopicId, "de/deepamehta/core/property/Path").toString();
            logger.info("### Opening file \"" + path + "\"");
            Desktop.getDesktop().open(new File(path));
        } catch (Throwable e) {
            throw new RuntimeException("Error while opening file \"" + path + "\"", e);
        }
    }



    // **********************
    // *** Plugin Service ***
    // **********************



    @Override
    public Topic createFileTopic(String path) {
        Topic topic = dms.getTopic("de/deepamehta/core/property/Path", new PropValue(path));
        if (topic != null) {
            return topic;
        }
        //
        File file = new File(path);
        String fileName = file.getName();
        String fileType = JavaUtils.getFileType(fileName);
        long fileSize = file.length();
        //
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/FileName", fileName);
        properties.put("de/deepamehta/core/property/Path", path);
        if (fileType != null) {
            properties.put("de/deepamehta/core/property/MediaType", fileType);
        }
        properties.put("de/deepamehta/core/property/Size", fileSize);
        //
        String content = renderFileContent(file, fileType, fileSize);
        if (content != null) {
            properties.put("de/deepamehta/core/property/Content", content);
        }
        //
        return dms.createTopic("de/deepamehta/core/topictype/File", properties, null);
    }

    @Override
    public Topic createFolderTopic(String path) {
        Topic topic = dms.getTopic("de/deepamehta/core/property/Path", new PropValue(path));
        if (topic != null) {
            return topic;
        }
        //
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/FolderName", new File(path).getName());
        properties.put("de/deepamehta/core/property/Path", path);
        //
        return dms.createTopic("de/deepamehta/core/topictype/Folder", properties, null);
    }

    // ---

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

    private String renderFileContent(File file, String fileType, long fileSize) {
        // Note: for unknown file types fileType is null
        if (fileType == null) {
            return null;
        }
        // TODO: let plugins render the file content
        String content = null;
        String path = file.getPath();
        if (fileType.equals("text/plain")) {
            content = "<pre>" + JavaUtils.readTextFile(file) + "</pre>";
        } else if (fileType.startsWith("image/")) {
            content = "<img src=\"" + localResourceURI(path, fileType, fileSize) + "\"></img>";
        } else if (fileType.equals("application/pdf")) {
            content = "<embed src=\"" + localResourceURI(path, fileType, fileSize) +
                "\" width=\"100%\" height=\"100%\"></embed>";
        } else if (fileType.startsWith("audio/")) {
            content = "<embed src=\"" + localResourceURI(path, fileType, fileSize) +
                "\" width=\"95%\" height=\"80\"></embed>";
            // var content = "<audio controls=\"\" src=\"" + localResourceURI(path, fileType, fileSize) +
            // "\"></audio>"
        } else if (fileType.startsWith("video/")) {
            content = "<embed src=\"" + localResourceURI(path, fileType, fileSize) + "\"></embed>";
            // var content = "<video controls=\"\" src=\"" + localResourceURI(path, fileType, fileSize) +
            // "\"></video>"
        }
        return content;
    }

    private String localResourceURI(String path, String type, long size) {
        return "/resource/file:" + JavaUtils.encodeURIComponent(path) + "?type=" + type + "&size=" + size;
    }

    // ---

    private boolean isRequestAllowed(HttpServletRequest request) {
        String localAddr = request.getLocalAddr();
        String remoteAddr = request.getRemoteAddr();
        boolean allowed = localAddr.equals(remoteAddr);
        logger.info(request.getRequestURL() + "\nlocal address: " + localAddr + ", remote address: " + remoteAddr +
            " => " + (allowed ? "ALLOWED" : "FORBIDDEN"));
        return allowed;
    }
}
