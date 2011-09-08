package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.service.FilesService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.util.JavaUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import java.awt.Desktop;
import java.io.File;
import java.util.logging.Logger;



@Path("/")
@Produces("application/json")
public class FilesPlugin extends Plugin implements FilesService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************
    // *** Plugin Service ***
    // **********************



    @POST
    @Path("/file/{path}")
    @Override
    public Topic createFileTopic(@PathParam("path") String path) {
        try {
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
            return dms.createTopic(new TopicModel("dm4.files.file", comp), null);       // clientContext=null
        } catch (Throwable e) {
            throw new RuntimeException("Creating file topic for path \"" + path + "\" failed", e);
        }
    }

    @POST
    @Path("/folder/{path}")
    @Override
    public Topic createFolderTopic(@PathParam("path") String path) {
        try {
            Topic folderTopic = getFolderTopic(path);
            if (folderTopic != null) {
                return folderTopic;
            }
            //
            CompositeValue comp = new CompositeValue();
            comp.put("dm4.files.folder_name", new File(path).getName());
            comp.put("dm4.files.path", path);
            //
            return dms.createTopic(new TopicModel("dm4.files.folder", comp), null);     // clientContext=null
        } catch (Throwable e) {
            throw new RuntimeException("Creating folder topic for path \"" + path + "\" failed", e);
        }
    }

    // ---

    @POST
    @Path("/{id}/file/{path}")
    @Override
    public Topic createChildFileTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFileTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }

    @POST
    @Path("/{id}/folder/{path}")
    @Override
    public Topic createChildFolderTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFolderTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }

    // ---

    @GET
    @Path("/{id}")
    @Override
    public void openFile(@PathParam("id") long fileTopicId) {
        String path = null;
        try {
            Topic fileTopic = dms.getTopic(fileTopicId, false, null);    // fetchComposite=false, clientContext=null
            path = fileTopic.getChildTopicValue("dm4.files.path").toString();
            logger.info("### Opening file \"" + path + "\"");
            Desktop.getDesktop().open(new File(path));
        } catch (Throwable e) {
            throw new RuntimeException("Opening file \"" + path + "\" failed", e);
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

    private Topic getFolderTopic(String path) {
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false);     // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.folder",
                true, false);
        }
        return null;
    }

    // ---

    private void associateChildTopic(long folderTopicId, long childTopicId) {
        if (!childAssociationExists(folderTopicId, childTopicId)) {
            dms.createAssociation(new AssociationModel("dm4.core.aggregation",
                new TopicRoleModel(folderTopicId, "dm4.core.whole"),
                new TopicRoleModel(childTopicId,  "dm4.core.part")), null);    // clientContext=null
        }
    }

    private boolean childAssociationExists(long folderTopicId, long childTopicId) {
        return dms.getAssociations(folderTopicId, childTopicId, "dm4.core.aggregation").size() > 0;
    }

    // ---

    // ### FIXME: to be dropped
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
}
