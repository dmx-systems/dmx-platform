package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.model.DirectoryListing;
import de.deepamehta.plugins.files.model.Resource;
import de.deepamehta.plugins.files.model.ResourceInfo;
import de.deepamehta.plugins.files.service.FilesService;

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
public class FilesPlugin extends Plugin implements FilesService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public CommandResult executeCommandHook(String command, CommandParams params, ClientContext clientContext) {
        if (command.equals("deepamehta-files.open-file")) {
            long fileTopicId = params.getInt("topic_id");    // topic_id deserializes as Integer (not Long)
            openFile(fileTopicId);
            return new CommandResult("message", "OK");
        } else if (command.equals("deepamehta-files.create-file-topic")) {
            String path = params.getString("path");
            try {
                return new CommandResult(createFileTopic(path).toJSON());
            } catch (Throwable e) {
                throw new RuntimeException("Error while creating file topic for \"" + path + "\"", e);
            }
        } else if (command.equals("deepamehta-files.create-folder-topic")) {
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
            Topic fileTopic = dms.getTopic(fileTopicId, false, null);    // fetchComposite=false, clientContext=null
            path = fileTopic.getChildTopicValue("dm4.files.path").toString();
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
        Topic fileTopic = getFileTopic(path);
        if (fileTopic != null) {
            return fileTopic;
        }
        //
        File file = new File(path);
        String fileName = file.getName();
        String fileType = JavaUtils.getFileType(fileName);
        long fileSize = file.length();
        //
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.file_name", fileName);
        comp.put("dm4.files.path", path);
        if (fileType != null) {
            comp.put("dm4.files.media_type", fileType);
        }
        comp.put("dm4.files.size", fileSize);
        //
        String content = renderFileContent(file, fileType, fileSize);
        if (content != null) {
            comp.put("dm4.files.content", content);
        }
        //
        return dms.createTopic(new TopicModel("dm4.files.file", comp), null);           // clientContext=null
    }

    @Override
    public Topic createFolderTopic(String path) {
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false);     // fetchComposite=false
        if (topic != null) {
            return topic;
        }
        //
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.folder_name", new File(path).getName());
        comp.put("dm4.files.path", path);
        //
        return dms.createTopic(new TopicModel("dm4.files.folder", comp), null);         // clientContext=null
    }

    // ---

    @GET
    @Path("/{uri:.+}")
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
    @Path("/{uri:.+}/info")
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

    private Topic getFileTopic(String path) {
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false);     // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.file",
                true, false);
        }
        return null;
    }

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
        return "/proxy/file:" + JavaUtils.encodeURIComponent(path) + "?type=" + type + "&size=" + size;
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
