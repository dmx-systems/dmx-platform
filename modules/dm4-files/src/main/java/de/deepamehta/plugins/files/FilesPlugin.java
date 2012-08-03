package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.service.FilesService;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.listener.InitializePluginListener;
import de.deepamehta.core.service.listener.PluginServiceArrivedListener;
import de.deepamehta.core.service.listener.PluginServiceGoneListener;
import de.deepamehta.core.util.JavaUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import javax.servlet.http.HttpServletRequest;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;



@Path("/files")
@Produces("application/json")
public class FilesPlugin extends PluginActivator implements FilesService, InitializePluginListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.filerepo.path");
    private static final String REMOTE_ACCESS_FILTER = System.getProperty("dm4.filerepo.netfilter");
    private static final String FILE_REPOSITORY_URI = "/filerepo";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***********************************
    // *** FilesService Implementation ***
    // ***********************************



    @POST
    @Path("/file/{path:.+}")       // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createFileTopic(@PathParam("path") String path) {
        String info = "Creating file topic for path \"" + path + "\"";
        try {
            // ### FIXME: drag'n'drop files from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) check if already exists
            Topic fileTopic = getFileTopic(path);
            if (fileTopic != null) {
                logger.info(info + " ABORTED -- already exists");
                return fileTopic;
            }
            // 2) create topic
            logger.info(info);
            //
            File file = locateFile(path);
            String fileName = file.getName();
            String mediaType = JavaUtils.getFileType(fileName);
            long size = file.length();
            //
            return createFileTopic(fileName, path, mediaType, size);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(info + " failed", e));
        }
    }

    @POST
    @Path("/folder/{path:.+}")     // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createFolderTopic(@PathParam("path") String path) {
        String info = "Creating folder topic for path \"" + path + "\"";
        try {
            // ### FIXME: drag'n'drop folders from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) check if already exists
            Topic folderTopic = getFolderTopic(path);
            if (folderTopic != null) {
                logger.info(info + " ABORTED -- already exists");
                return folderTopic;
            }
            // 2) create topic
            logger.info(info);
            //
            String folderName = new File(path).getName();
            return createFolderTopic(folderName, path);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(info + " failed", e));
        }
    }

    // ---

    @POST
    @Path("/{id}/file/{path:.+}")  // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createChildFileTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFileTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }

    @POST
    @Path("/{id}/folder/{path:.+}")// Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createChildFolderTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFolderTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }



    // === File Repository ===

    @Override
    public void createFolder(String folderName, String storagePath) {
        try {
            logger.info("folderName=\"" + folderName + "\", storagePath=\"" + storagePath + "\"");
            // ### TODO
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Creating folder \"" + folderName +
                "\" failed (storagePath=\"" + storagePath + "\")", e));
        }
    }

    @POST
    @Path("/{storage_path:.+}")
    @Consumes("multipart/form-data")
    @Override
    public StoredFile storeFile(UploadedFile file, @PathParam("storage_path") String storagePath) {
        File repoFile = null;
        try {
            logger.info(file + ", storagePath=\"" + storagePath + "\"");
            Topic fileTopic = createFileTopic(file, storagePath);
            repoFile = repoFile(fileTopic);
            logger.info("Storing file \"" + repoFile + "\" in file repository");
            file.write(repoFile);
            return new StoredFile(fileName(fileTopic), fileTopic.getId());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Storing file \"" + repoFile +
                "\" in file repository failed", e));
        }
    }

    // ===

    @GET
    @Path("/{path:.+}")      // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public DirectoryListing getFolderContent(@PathParam("path") String path) {
        logger.info("path=\"" + path + "\")");
        //
        checkRemoteAccess(request);
        //
        File file = locateFile(path);
        if (file.isDirectory()) {
            return new DirectoryListing(file);
        }
        return null;    // ### FIXME
    }

    @GET
    @Path("/{path:.+}/info") // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public ResourceInfo getResourceInfo(@PathParam("path") String path) {
        logger.info("path=\"" + path + "\")");
        //
        checkRemoteAccess(request);
        //
        return new ResourceInfo(locateFile(path));
    }

    // ---

    @Override
    public File locateFile(String relativePath) {
        return checkFileAccess(new File(FILE_REPOSITORY_PATH, relativePath));
    }

    // ===

    @POST
    @Path("/{id}")
    @Override
    public void openFile(@PathParam("id") long fileTopicId) {
        File file = null;
        try {
            Topic fileTopic = dms.getTopic(fileTopicId, true, null);    // fetchComposite=true, clientState=null
            String path = fileTopic.getCompositeValue().getString("dm4.files.path");
            file = locateFile(path);
            logger.info("### Opening file \"" + file + "\"");
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Opening file \"" + file + "\" failed", e));
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void initializePlugin() {
        publishDirectory(FILE_REPOSITORY_PATH, FILE_REPOSITORY_URI);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic getFileTopic(String path) {
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false, null);   // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.file",
                true, false, null);
        }
        return null;
    }

    private Topic getFolderTopic(String path) {
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false, null);   // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.folder",
                true, false, null);
        }
        return null;
    }

    // ---

    private Topic createFileTopic(String fileName, String path, String mediaType, long size) {
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.file_name", fileName);
        comp.put("dm4.files.path", path);
        if (mediaType != null) {
            comp.put("dm4.files.media_type", mediaType);
        }
        comp.put("dm4.files.size", size);
        //
        return dms.createTopic(new TopicModel("dm4.files.file", comp), null);       // FIXME: clientState=null
    }

    private Topic createFolderTopic(String folderName, String path) {
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.folder_name", folderName);
        comp.put("dm4.files.path", path);
        //
        return dms.createTopic(new TopicModel("dm4.files.folder", comp), null);     // FIXME: clientState=null
    }

    // ---

    private void associateChildTopic(long folderTopicId, long childTopicId) {
        if (!childAssociationExists(folderTopicId, childTopicId)) {
            dms.createAssociation(new AssociationModel("dm4.core.aggregation",
                new TopicRoleModel(folderTopicId, "dm4.core.whole"),
                new TopicRoleModel(childTopicId,  "dm4.core.part")), null);    // clientState=null
        }
    }

    private boolean childAssociationExists(long folderTopicId, long childTopicId) {
        return dms.getAssociations(folderTopicId, childTopicId, "dm4.core.aggregation").size() > 0;
    }



    // === File Repository ===

    /**
     * @param   storagePath     Begins with "/". Has no "/" at end.
     */
    private Topic createFileTopic(UploadedFile file, String storagePath) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic fileTopic = dms.createTopic(new TopicModel("dm4.files.file"), null);       // FIXME: clientState=null
            //
            String fileName = fileTopic.getId() + "-" + file.getName();
            String path = storagePath + "/" + fileName;
            String mediaType = file.getMediaType();
            long size = file.getSize();
            //
            CompositeValue comp = new CompositeValue();
            comp.put("dm4.files.file_name", fileName);
            comp.put("dm4.files.path", path);
            if (mediaType != null) {
                comp.put("dm4.files.media_type", mediaType);
            }
            comp.put("dm4.files.size", size);
            //
            fileTopic.setCompositeValue(comp, null, null);
            //
            tx.success();
            return fileTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating file topic for uploaded file failed (" +
                file + ", storagePath=\"" + storagePath + "\")", e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Calculates the storage location for the file represented by the specified file topic.
     */
    private File repoFile(Topic fileTopic) {
        return new File(FILE_REPOSITORY_PATH, path(fileTopic));
    }

    // ---

    private String fileName(Topic fileTopic) {
        return fileTopic.getCompositeValue().getString("dm4.files.file_name");
    }

    private String path(Topic fileTopic) {
        return fileTopic.getCompositeValue().getString("dm4.files.path");
    }

    // ---

    private void checkRemoteAccess(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isInRange = JavaUtils.isInRange(remoteAddr, REMOTE_ACCESS_FILTER);
        //
        logger.info("Checking remote access to \"" + request.getRequestURL() + "\"\n      dm4.filerepo.netfilter=\"" +
            REMOTE_ACCESS_FILTER + "\", remote address=\"" + remoteAddr + "\" => " +
            (isInRange ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!isInRange) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

    /**
     * @return  The canonical file.
     */
    private File checkFileAccess(File file) {
        try {
            // 1) Check path
            //
            // Note 1: we use getCanonicalPath() to fight directory traversal attacks (../../).
            // Note 2: A directory path returned by getCanonicalPath() never contains a "/" at the end.
            // Thats why "dm4.filerepo.path" is expected to have no "/" at the end as well.
            String path = file.getCanonicalPath();
            boolean pointsToRepository = path.startsWith(FILE_REPOSITORY_PATH);
            //
            logger.info("Checking file repository access to \"" + file.getPath() + "\"\n      dm4.filerepo.path=" +
                "\"" + FILE_REPOSITORY_PATH + "\", canonical request path=\"" + path + "\" => " +
                (pointsToRepository ? "ALLOWED" : "FORBIDDEN"));
            //
            if (!pointsToRepository) {
                throw new WebApplicationException(Status.FORBIDDEN);
            }
            //
            // 2) Check existence
            //
            if (!file.exists()) {
                logger.info("Requested file/directory \"" + file.getPath() + "\" does not exist => NOT FOUND");
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            //
            return new File(path);
        } catch (IOException e) {
            throw new RuntimeException("Checking file repository access failed (file=\"" + file + "\")", e);
        }
    }
}
