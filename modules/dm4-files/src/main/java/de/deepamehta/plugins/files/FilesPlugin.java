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
import de.deepamehta.core.service.SecurityHandler;
import de.deepamehta.core.service.listener.InitializePluginListener;
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
import javax.servlet.http.HttpServletResponse;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;



@Path("/files")
@Produces("application/json")
public class FilesPlugin extends PluginActivator implements FilesService, SecurityHandler, InitializePluginListener {

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



    // === File System Representation ===

    @POST
    @Path("/file/{path:.+}")       // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createFileTopic(@PathParam("path") String path) {
        String operation = "Creating file topic for request path \"" + path + "\"";
        try {
            logger.info(operation);
            // ### FIXME: drag'n'drop files from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) enforce security
            File file = enforeSecurity(request, path);
            //
            // 2) check if topic already exists
            Topic fileTopic = fetchFileTopic(file);
            if (fileTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return fileTopic;
            }
            // 3) create topic
            return createFileTopic(file);
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
        }
    }

    @POST
    @Path("/folder/{path:.+}")     // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public Topic createFolderTopic(@PathParam("path") String path) {
        String operation = "Creating folder topic for request path \"" + path + "\"";
        try {
            logger.info(operation);
            // ### FIXME: drag'n'drop folders from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) enforce security
            File file = enforeSecurity(request, path);
            //
            // 2) check if topic already exists
            Topic folderTopic = fetchFolderTopic(file);
            if (folderTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return folderTopic;
            }
            // 3) create topic
            return createFolderTopic(file);
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
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

    @POST
    @Path("/{path:.+}")
    @Consumes("multipart/form-data")
    @Override
    public StoredFile storeFile(UploadedFile file, @PathParam("path") String path) {
        String operation = "Storing " + file + " at request path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) enforce security
            File directory = enforeSecurity(request, path);
            //
            // 2) store file
            File repoFile = repoFile(directory, file);
            file.write(repoFile);
            //
            // 3) create topic
            Topic fileTopic = createFileTopic(repoFile);
            return new StoredFile(repoFile.getName(), fileTopic.getId());
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
        }
    }

    @Override
    public void createFolder(String folderName, String path) {
        try {
            logger.info("folderName=\"" + folderName + "\", path=\"" + path + "\"");
            // ### TODO
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Creating folder \"" + folderName +
                "\" failed (path=\"" + path + "\")", e));
        }
    }

    // ---

    @GET
    @Path("/{path:.+}")      // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public DirectoryListing getDirectoryListing(@PathParam("path") String path) {
        String operation = "Getting directory listing for request path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File folder = enforeSecurity(request, path);
            //
            // ### TODO: if folder is no directory send NOT FOUND
            return new DirectoryListing(folder);
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
        }
    }

    @GET
    @Path("/{path:.+}/info") // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public ResourceInfo getResourceInfo(@PathParam("path") String path) {
        String operation = "Getting resource info for request path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File file = enforeSecurity(request, path);
            //
            return new ResourceInfo(file);
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
        }
    }

    // ---

    @POST
    @Path("/{id}")
    @Override
    public void openFile(@PathParam("id") long fileTopicId) {
        String operation = "Opening file of topic " + fileTopicId;
        try {
            Topic fileTopic = dms.getTopic(fileTopicId, true, null);    // fetchComposite=true, clientState=null
            String path = fileTopic.getCompositeValue().getString("dm4.files.path");
            //
            File file = enforeSecurity(request, path);
            //
            logger.info("### Opening file \"" + file + "\"");
            Desktop.getDesktop().open(file);
            //
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e));
        }
    }



    // **************************************
    // *** SecurityHandler Implementation ***
    // **************************************



    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI().substring(FILE_REPOSITORY_URI.length());
            path = JavaUtils.decodeURIComponent(path);
            logger.info("### request path=\"" + path + "\"");
            File file = enforeSecurity(request, path);
            return true;
        } catch (FileRepositoryException e) {
            response.setStatus(e.getStatusCode());
            return false;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void initializePlugin() {
        publishDirectory(FILE_REPOSITORY_PATH, FILE_REPOSITORY_URI, this);      // securityHandler=this
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === File System Representation ===

    private Topic fetchFileTopic(File file) {
        String path = truncate(file.getPath());
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false, null);   // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.file",
                true, false, null);
        }
        return null;
    }

    private Topic fetchFolderTopic(File file) {
        String path = truncate(file.getPath());
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path), false, null);   // fetchComposite=false
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole", "dm4.files.folder",
                true, false, null);
        }
        return null;
    }

    // ---

    private Topic createFileTopic(File file) {
        String mediaType = JavaUtils.getFileType(file.getName());
        //
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.file_name", file.getName());
        comp.put("dm4.files.path",      truncate(file.getPath()));
        if (mediaType != null) {
            comp.put("dm4.files.media_type", mediaType);
        }
        comp.put("dm4.files.size",      file.length());
        //
        return dms.createTopic(new TopicModel("dm4.files.file", comp), null);       // FIXME: clientState=null
    }

    private Topic createFolderTopic(File file) {
        String folderName = file.getName();
        String path = truncate(file.getPath());
        //
        // root folder needs special treatment
        if (path.equals("/")) {
            folderName = "";
        }
        //
        CompositeValue comp = new CompositeValue();
        comp.put("dm4.files.folder_name", folderName);
        comp.put("dm4.files.path",        path);
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

    private File mapRequestPathToRepository(String path) {
        try {
            File file = new File(FILE_REPOSITORY_PATH, path);
            // Note 1: we use getCanonicalPath() to fight directory traversal attacks (../../).
            // Note 2: A directory path returned by getCanonicalPath() never contains a "/" at the end.
            // Thats why "dm4.filerepo.path" is expected to have no "/" at the end as well.
            return file.getCanonicalFile();     // throws IOException
        } catch (Exception e) {
            throw new RuntimeException("Mapping request path \"" + path + "\" to file repository failed", e);
        }
    }

    /**
     * Calculates the storage location for the uploaded file.
     */
    private File repoFile(File directory, UploadedFile file) {
        return JavaUtils.findUnusedFile(new File(directory, file.getName()));
    }

    /**
     * Truncates an absolute path to a request path.
     */
    private String truncate(String path) {
        path = path.substring(FILE_REPOSITORY_PATH.length());
        // root folder needs special treatment
        if (path.equals("")) {
            path = "/";
        }
        //
        return path;
        // ### TODO: there is a principle copy in DirectoryListing
        // ### FIXME: Windows drive letter? See DirectoryListing
    }



    // === Security ===

    private File enforeSecurity(HttpServletRequest request, String path) throws FileRepositoryException {
        checkRemoteAccess(request);
        //
        File file = mapRequestPathToRepository(path);
        checkFilePath(file);
        checkFileExistence(file);
        //
        return file;
    }

    // --- Remote Access ---

    private void checkRemoteAccess(HttpServletRequest request) throws FileRepositoryException {
        String remoteAddr = request.getRemoteAddr();
        boolean isInRange = JavaUtils.isInRange(remoteAddr, REMOTE_ACCESS_FILTER);
        //
        logger.info("Checking remote access to \"" + request.getRequestURL() + "\"\n      dm4.filerepo.netfilter=\"" +
            REMOTE_ACCESS_FILTER + "\", remote address=\"" + remoteAddr + "\" => " +
            (isInRange ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!isInRange) {
            throw new FileRepositoryException("Request from \"" + remoteAddr + "\" is not allowed " +
                "(dm4.filerepo.netfilter=\"" + REMOTE_ACCESS_FILTER + "\")", Status.FORBIDDEN);
        }
    }

    // --- File Access ---

    /**
     * Prerequisite: the file's path is canonical.
     */
    private void checkFilePath(File file) throws FileRepositoryException {
        boolean pointsToRepository = file.getPath().startsWith(FILE_REPOSITORY_PATH);
        //
        logger.info("Checking file repository access to \"" + file + "\"\n      dm4.filerepo.path=" +
            "\"" + FILE_REPOSITORY_PATH + "\" => " + (pointsToRepository ? "ALLOWED" : "FORBIDDEN"));
        //
        if (!pointsToRepository) {
            throw new FileRepositoryException("\"" + file + "\" is not a file repository path", Status.FORBIDDEN);
        }
    }

    private void checkFileExistence(File file) throws FileRepositoryException {
        if (!file.exists()) {
            logger.info("File/directory \"" + file + "\" does not exist => NOT FOUND");
            throw new FileRepositoryException("\"" + file + "\" does not exist in file repository", Status.NOT_FOUND);
        }
    }
}
