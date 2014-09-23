package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.service.FilesService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.SecurityHandler;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;



@Path("/files")
@Produces("application/json")
public class FilesPlugin extends PluginActivator implements FilesService, SecurityHandler {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.filerepo.path");
    private static final String FILE_REPOSITORY_URI = "/filerepo";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***********************************
    // *** FilesService Implementation ***
    // ***********************************



    // === File System Representation ===

    @POST
    @Path("/file/{path:.+}")       // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Transactional
    @Override
    public Topic createFileTopic(@PathParam("path") String path) {
        String operation = "Creating file topic for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // ### FIXME: drag'n'drop files from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) pre-checks
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            //
            // 2) check if topic already exists
            Topic fileTopic = fetchFileTopic(file);
            if (fileTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return fileTopic;
            }
            // 3) create topic
            return createFileTopic(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @POST
    @Path("/folder/{path:.+}")     // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Transactional
    @Override
    public Topic createFolderTopic(@PathParam("path") String path) {
        String operation = "Creating folder topic for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // ### FIXME: drag'n'drop folders from arbitrary locations (in particular different Windows drives)
            // collides with the concept of a single-rooted file repository (as realized by the Files module).
            // For the moment we just strip a possible drive letter to be compatible with the Files module.
            path = JavaUtils.stripDriveLetter(path);
            //
            // 1) pre-checks
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            //
            // 2) check if topic already exists
            Topic folderTopic = fetchFolderTopic(file);
            if (folderTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return folderTopic;
            }
            // 3) create topic
            return createFolderTopic(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // ---

    @POST
    @Path("/parent/{id}/file/{path:.+}")    // Note: we also match slashes as they are already decoded by an apache ...
    @Transactional
    @Override
    public Topic createChildFileTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFileTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }

    @POST
    @Path("/parent/{id}/folder/{path:.+}")  // Note: we also match slashes as they are already decoded by an apache ...
    @Transactional
    @Override
    public Topic createChildFolderTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic childTopic = createFolderTopic(path);
        associateChildTopic(folderTopicId, childTopic.getId());
        return childTopic;
    }



    // === File Repository ===

    @POST
    @Path("/{path:.+}")     // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Consumes("multipart/form-data")
    @Transactional
    @Override
    public StoredFile storeFile(UploadedFile file, @PathParam("path") String path) {
        String operation = "Storing " + file + " at repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) pre-checks
            File directory = enforeSecurity(path);  // throws FileRepositoryException
            checkFileExistence(directory);          // throws FileRepositoryException
            //
            // 2) store file
            File repoFile = repoFile(directory, file);
            file.write(repoFile);
            //
            // 3) create topic
            Topic fileTopic = createFileTopic(repoFile);
            return new StoredFile(repoFile.getName(), fileTopic.getId());
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // Note: this is not a resource method. So we don't throw a WebApplicationException here.
    @Override
    public Topic createFile(InputStream in, String path) {
        String operation = "Creating file (from input stream) at repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) pre-checks
            File file = enforeSecurity(path);       // throws FileRepositoryException
            //
            // 2) store file
            FileOutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            in.close();
            out.close();
            //
            // 3) create topic
            // ### TODO: think about overwriting an existing file.
            // ### FIXME: in this case the existing file topic is not updated and might reflect e.g. the wrong size.
            return createFileTopic(path);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @POST
    @Path("/{path:.+}/folder/{folder_name}") // Note: we also match slashes as they are already decoded by an apache ...
    @Override
    public void createFolder(@PathParam("folder_name") String folderName, @PathParam("path") String path) {
        String operation = "Creating folder \"" + folderName + "\" at repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) pre-checks
            File directory = enforeSecurity(path);  // throws FileRepositoryException
            checkFileExistence(directory);          // throws FileRepositoryException
            //
            // 2) create directory
            File repoFile = repoFile(directory, folderName);
            if (repoFile.exists()) {
                throw new RuntimeException("File or directory \"" + repoFile + "\" already exists");
            }
            //
            boolean success = repoFile.mkdir();
            //
            if (!success) {
                throw new RuntimeException("File.mkdir() failed (file=\"" + repoFile + "\")");
            }
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // ---

    @GET
    @Path("/{path:.+}/info")    // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public ResourceInfo getResourceInfo(@PathParam("path") String path) {
        String operation = "Getting resource info for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            //
            return new ResourceInfo(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @GET
    @Path("/{path:.+}")         // Note: we also match slashes as they are already decoded by an apache reverse proxy
    @Override
    public DirectoryListing getDirectoryListing(@PathParam("path") String path) {
        String operation = "Getting directory listing for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File folder = enforeSecurity(path); // throws FileRepositoryException
            checkFileExistence(folder);         // throws FileRepositoryException
            //
            return new DirectoryListing(folder);    // ### TODO: if folder is no directory send NOT FOUND
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @Override
    public String getRepositoryPath(URL url) {
        String operation = "Checking for file repository URL (\"" + url + "\")";
        try {
            if (!DeepaMehtaUtils.isDeepaMehtaURL(url)) {
                logger.info(operation + " => null");
                return null;
            }
            //
            String path = url.getPath();
            if (!path.startsWith(FILE_REPOSITORY_URI)) {
                logger.info(operation + " => null");
                return null;
            }
            //
            String repoPath = path.substring(FILE_REPOSITORY_URI.length());
            logger.info(operation + " => \"" + repoPath + "\"");
            return repoPath;
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // ---

    // Note: this is not a resource method. So we don't throw a WebApplicationException here.
    // To access a file remotely use the /filerepo resource.
    @Override
    public File getFile(String path) {
        String operation = "Accessing the file at \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            return file;
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // Note: this is not a resource method. So we don't throw a WebApplicationException here.
    // To access a file remotely use the /filerepo resource.
    @Override
    public File getFile(long fileTopicId) {
        String operation = "Accessing the file of file topic " + fileTopicId;
        try {
            logger.info(operation);
            //
            String path = repoPath(fileTopicId);
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            return file;
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // ---

    @POST
    @Path("/open/{id}")
    @Override
    public void openFile(@PathParam("id") long fileTopicId) {
        String operation = "Opening the file of file topic " + fileTopicId;
        try {
            logger.info(operation);
            //
            String path = repoPath(fileTopicId);
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            //
            logger.info("### Opening file \"" + file + "\"");
            Desktop.getDesktop().open(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }



    // **************************************
    // *** SecurityHandler Implementation ***
    // **************************************



    // ### TODO: to be dropped?
    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI().substring(FILE_REPOSITORY_URI.length());
            path = JavaUtils.decodeURIComponent(path);
            logger.info("### repository path=\"" + path + "\"");
            File file = enforeSecurity(path);   // throws FileRepositoryException
            checkFileExistence(file);           // throws FileRepositoryException
            return true;
        } catch (FileRepositoryException e) {
            response.setStatus(e.getStatusCode());
            return false;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        publishDirectory(FILE_REPOSITORY_PATH, FILE_REPOSITORY_URI, this);      // securityHandler=this
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === File System Representation ===

    /**
     * Fetches the File topic representing the file at a given absolute path.
     * If no such File topic exists <code>null</code> is returned.
     *
     * @param   file   An absolute path.
     */
    private Topic fetchFileTopic(File file) {
        return fetchTopic(file, "dm4.files.file");
    }

    /**
     * Fetches the Folder topic representing the folder at a given absolute path.
     * If no such Folder topic exists <code>null</code> is returned.
     *
     * @param   file   An absolute path.
     */
    private Topic fetchFolderTopic(File file) {
        return fetchTopic(file, "dm4.files.folder");
    }

    // ---

    /**
     * @param   file   An absolute path.
     */
    private Topic fetchTopic(File file, String parentTypeUri) {
        String path = repoPath(file);
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(path));
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
                parentTypeUri);     // ### FIXME: had fetchComposite=true
        }
        return null;
    }

    // ---

    private Topic createFileTopic(File file) {
        String mediaType = JavaUtils.getFileType(file.getName());
        //
        CompositeValueModel comp = new CompositeValueModel();
        comp.put("dm4.files.file_name", file.getName());
        comp.put("dm4.files.path",      repoPath(file));
        if (mediaType != null) {
            comp.put("dm4.files.media_type", mediaType);
        }
        comp.put("dm4.files.size",      file.length());
        //
        return dms.createTopic(new TopicModel("dm4.files.file", comp));
    }

    private Topic createFolderTopic(File file) {
        String folderName = file.getName();
        String path = repoPath(file);
        //
        // root folder needs special treatment
        if (path.equals("/")) {
            folderName = "";
        }
        //
        CompositeValueModel comp = new CompositeValueModel();
        comp.put("dm4.files.folder_name", folderName);
        comp.put("dm4.files.path",        path);
        //
        return dms.createTopic(new TopicModel("dm4.files.folder", comp));
    }

    // ---

    private void associateChildTopic(long folderTopicId, long childTopicId) {
        if (!childAssociationExists(folderTopicId, childTopicId)) {
            dms.createAssociation(new AssociationModel("dm4.core.aggregation",
                new TopicRoleModel(folderTopicId, "dm4.core.parent"),
                new TopicRoleModel(childTopicId,  "dm4.core.child")
            ));
        }
    }

    private boolean childAssociationExists(long folderTopicId, long childTopicId) {
        return dms.getAssociations(folderTopicId, childTopicId, "dm4.core.aggregation").size() > 0;
    }



    // === File Repository ===

    /**
     * Constructs an absolute path from a repository path.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     */
    private File repoFile(String path) {
        return new File(FILE_REPOSITORY_PATH, path);
    }

    /**
     * Calculates the storage location for the uploaded file.
     *
     * @param   directory   An absolute path.
     */
    private File repoFile(File directory, UploadedFile file) {
        return JavaUtils.findUnusedFile(repoFile(directory, file.getName()));
    }

    /**
     * @param   directory   An absolute path.
     */
    private File repoFile(File directory, String fileName) {
        return new File(directory, fileName);
    }

    // ---

    /**
     * Returns the repository path that corresponds to the given absolute path.
     *
     * @param   file    An absolute path.
     */
    private String repoPath(File file) {
        String path = file.getPath().substring(FILE_REPOSITORY_PATH.length());
        // root folder needs special treatment
        if (path.equals("")) {
            path = "/";
        }
        //
        return path;
        // ### TODO: there is a principle copy in DirectoryListing
        // ### FIXME: Windows drive letter? See DirectoryListing
    }

    /**
     * Returns the repository path of the given File/Folder topic.
     */
    private String repoPath(long fileTopicId) {
        Topic fileTopic = dms.getTopic(fileTopicId);    // ### FIXME: had fetchComposite=true
        return fileTopic.getCompositeValue().getString("dm4.files.path");
    }



    // === Security ===

    /**
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *
     * @return  An absolute path.
     */
    private File enforeSecurity(String path) throws FileRepositoryException {
        try {
            // Note 1: we use getCanonicalPath() to fight directory traversal attacks (../../).
            // Note 2: A directory path returned by getCanonicalPath() never contains a "/" at the end.
            // Thats why "dm4.filerepo.path" is expected to have no "/" at the end as well.
            File file = repoFile(path).getCanonicalFile();  // throws IOException
            checkFilePath(file);                            // throws FileRepositoryException
            //
            return file;
        } catch (FileRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Enforcing security for repository path \"" + path + "\" failed", e);
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
            logger.info("File or directory \"" + file + "\" does not exist => NOT FOUND");
            throw new FileRepositoryException("\"" + file + "\" does not exist in file repository", Status.NOT_FOUND);
        }
    }
}
