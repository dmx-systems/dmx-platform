package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.event.CheckDiskQuotaListener;
import de.deepamehta.plugins.files.service.FilesService;

import de.deepamehta.plugins.config.ModificationRole;
import de.deepamehta.plugins.config.TypeConfigDefinition;
import de.deepamehta.plugins.config.service.ConfigService;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.accesscontrol.AccessControl;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.webpublishing.listeners.ResourceRequestFilterListener;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import javax.servlet.http.HttpServletRequest;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Path("/files")
@Produces("application/json")
public class FilesPlugin extends PluginActivator implements FilesService, ResourceRequestFilterListener, PathMapper {

    // ------------------------------------------------------------------------------------------------------- Constants

    public static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.filerepo.path", "/");
    public static final boolean FILE_REPOSITORY_PER_WORKSPACE = Boolean.getBoolean("dm4.filerepo.per_workspace");
    public static final int DISK_QUOTA_MB = Integer.getInteger("dm4.filerepo.disk_quota", 150);
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default value must match the value defined in global POM.

    private static final String FILE_REPOSITORY_URI = "/filerepo";

    private static final Pattern PER_WORKSPACE_PATH_PATTERN = Pattern.compile("/workspace-(\\d+).*");

    // Events
    public static DeepaMehtaEvent CHECK_DISK_QUOTA = new DeepaMehtaEvent(CheckDiskQuotaListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((CheckDiskQuotaListener) listener).checkDiskQuota(
                (String) params[0], (Long) params[1], (Long) params[2]
            );
        }
    };

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // Note: this instance variable is not used but we must declare it in order to initiate service tracking.
    // The Config service is accessed only on-the-fly within the serviceArrived() and serviceGone() hooks.
    @Inject
    private ConfigService configService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***********************************
    // *** FilesService Implementation ***
    // ***********************************



    // === File System Representation ===

    @GET
    @Path("/file/{path}")
    @Transactional
    @Override
    public Topic getFileTopic(@PathParam("path") String path) {
        String operation = "Creating File topic for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            // 1) pre-checks
            File file = absolutePath(path);     // throws FileRepositoryException
            checkExistence(file);               // throws FileRepositoryException
            //
            // 2) check if topic already exists
            Topic fileTopic = fetchFileTopic(file);
            if (fileTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return fileTopic.loadChildTopics();
            }
            // 3) create topic
            return createFileTopic(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @GET
    @Path("/folder/{path}")
    @Transactional
    @Override
    public Topic getFolderTopic(@PathParam("path") String path) {
        String operation = "Creating Folder topic for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            // 1) pre-checks
            File file = absolutePath(path);     // throws FileRepositoryException
            checkExistence(file);               // throws FileRepositoryException
            //
            // 2) check if topic already exists
            Topic folderTopic = fetchFolderTopic(file);
            if (folderTopic != null) {
                logger.info(operation + " ABORTED -- already exists");
                return folderTopic.loadChildTopics();
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

    @GET
    @Path("/parent/{id}/file/{path}")
    @Transactional
    @Override
    public Topic getChildFileTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic topic = getFileTopic(path);
        createFolderAssociation(folderTopicId, topic);
        return topic;
    }

    @GET
    @Path("/parent/{id}/folder/{path}")
    @Transactional
    @Override
    public Topic getChildFolderTopic(@PathParam("id") long folderTopicId, @PathParam("path") String path) {
        Topic topic = getFolderTopic(path);
        createFolderAssociation(folderTopicId, topic);
        return topic;
    }



    // === File Repository ===

    @POST
    @Path("/{path}")
    @Consumes("multipart/form-data")
    @Transactional
    @Override
    public StoredFile storeFile(UploadedFile file, @PathParam("path") String path) {
        String operation = "Storing " + file + " at repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) pre-checks
            File directory = absolutePath(path);    // throws FileRepositoryException
            checkExistence(directory);              // throws FileRepositoryException
            //
            // 2) store file
            File repoFile = unusedPath(directory, file);
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
            File file = absolutePath(path);         // throws FileRepositoryException
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
            return getFileTopic(path);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @POST
    @Path("/{path}/folder/{folder_name}")
    @Override
    public void createFolder(@PathParam("folder_name") String folderName, @PathParam("path") String path) {
        String operation = "Creating folder \"" + folderName + "\" at repository path \"" + path + "\"";
        try {
            logger.info(operation);
            // 1) pre-checks
            File directory = absolutePath(path);    // throws FileRepositoryException
            checkExistence(directory);              // throws FileRepositoryException
            //
            // 2) create directory
            File repoFile = path(directory, folderName);
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
    @Path("/{path}/info")
    @Override
    public ResourceInfo getResourceInfo(@PathParam("path") String path) {
        String operation = "Getting resource info for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File file = absolutePath(path);         // throws FileRepositoryException
            checkExistence(file);                   // throws FileRepositoryException
            //
            return new ResourceInfo(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    @GET
    @Path("/{path}")
    @Override
    public DirectoryListing getDirectoryListing(@PathParam("path") String path) {
        String operation = "Getting directory listing for repository path \"" + path + "\"";
        try {
            logger.info(operation);
            //
            File directory = absolutePath(path);    // throws FileRepositoryException
            checkExistence(directory);              // throws FileRepositoryException
            //
            return new DirectoryListing(directory, this);
            // ### TODO: if directory is no directory send NOT FOUND
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
            File file = absolutePath(path);     // throws FileRepositoryException
            checkExistence(file);               // throws FileRepositoryException
            return file;
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }

    // Note: this is not a resource method. So we don't throw a WebApplicationException here.
    // To access a file remotely use the /filerepo resource.
    @Override
    public File getFile(long fileTopicId) {
        String operation = "Accessing the file of File topic " + fileTopicId;
        try {
            logger.info(operation);
            //
            String path = repoPath(fileTopicId);
            File file = absolutePath(path);     // throws FileRepositoryException
            checkExistence(file);               // throws FileRepositoryException
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
        String operation = "Opening the file of File topic " + fileTopicId;
        try {
            logger.info(operation);
            //
            String path = repoPath(fileTopicId);
            File file = absolutePath(path);     // throws FileRepositoryException
            checkExistence(file);               // throws FileRepositoryException
            //
            logger.info("### Opening file \"" + file + "\"");
            Desktop.getDesktop().open(file);
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(new RuntimeException(operation + " failed", e), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        publishFileSystem(FILE_REPOSITORY_URI, FILE_REPOSITORY_PATH);
    }

    @Override
    public void serviceArrived(PluginService service) {
        ((ConfigService) service).registerConfigDefinition(new TypeConfigDefinition(
            "dm4.accesscontrol.username",
            new TopicModel("dm4.files.disk_quota", new SimpleValue(DISK_QUOTA_MB)),
            ModificationRole.ADMIN
        ));
    }

    @Override
    public void serviceGone(PluginService service) {
        // Note 1: unregistering is crucial e.g. for redeploying the Files plugin. The next register call (at plugin
        // start time) would fail as the Config service holds a registration for that config type URI already.
        // Note 2: we must unregister via serviceGone() hook, that is immediately when the Config service is about
        // to go away. Using the shutdown() hook instead would be too late as the Config service might already gone.
        ((ConfigService) service).unregisterConfigDefinition("dm4.files.disk_quota");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void resourceRequestFilter(HttpServletRequest request) {
        try {
            String repoPath = repoPath(request);
            if (repoPath != null) {
                logger.info("### Checking access to repository path \"" + repoPath + "\"");
                File path = absolutePath(repoPath);             // throws FileRepositoryException 403 Forbidden
                checkExistence(path);                           // throws FileRepositoryException 404 Not Found
                checkAuthorization(path, repoPath, request);    // throws FileRepositoryException 401 Unauthorized
            }
        } catch (FileRepositoryException e) {
            throw new WebApplicationException(e, e.getStatus());
        }
    }



    // *********************************
    // *** PathMapper Implementation ***
    // *********************************



    @Override
    public String repoPath(File path) {
        try {
            String repoPath = path.getPath();
            //
            if (!repoPath.startsWith(FILE_REPOSITORY_PATH)) {
                throw new RuntimeException("Absolute path \"" + path + "\" is not a repository path");
            }
            //
            if (!FILE_REPOSITORY_PATH.equals("/")) {
                repoPath = repoPath.substring(FILE_REPOSITORY_PATH.length());
            }
            // ### FIXME: Windows drive letter?
            return repoPath;
        } catch (Exception e) {
            throw new RuntimeException("Mapping absolute path \"" + path + "\" to a repository path failed", e);
        }
    }




    // ------------------------------------------------------------------------------------------------- Private Methods



    // === File System Representation ===

    /**
     * Fetches the File topic representing the file at the given absolute path.
     * If no such File topic exists <code>null</code> is returned.
     *
     * @param   path   An absolute path.
     */
    private Topic fetchFileTopic(File path) {
        return fetchTopic(path, "dm4.files.file");
    }

    /**
     * Fetches the Folder topic representing the folder at the given absolute path.
     * If no such Folder topic exists <code>null</code> is returned.
     *
     * @param   path   An absolute path.
     */
    private Topic fetchFolderTopic(File path) {
        return fetchTopic(path, "dm4.files.folder");
    }

    // ---

    /**
     * Fetches the File/Folder topic representing the file/directory at the given absolute path.
     * If no such File/Folder topic exists <code>null</code> is returned.
     *
     * @param   path            An absolute path.
     * @param   topicTypeUri    The type of the topic to fetch: either "dm4.files.file" or "dm4.files.folder".
     */
    private Topic fetchTopic(File path, String topicTypeUri) {
        String repoPath = repoPath(path);
        Topic topic = dms.getTopic("dm4.files.path", new SimpleValue(repoPath));
        if (topic != null) {
            return topic.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent", topicTypeUri);
                // ### FIXME: had fetchComposite=true
        }
        return null;
    }

    // ---

    /**
     * Creates a File topic representing the file at the given absolute path.
     *
     * @param   path            An absolute path.
     */
    private Topic createFileTopic(File path) throws Exception {
        ChildTopicsModel childTopics = new ChildTopicsModel()
            .put("dm4.files.file_name", path.getName())
            .put("dm4.files.path", repoPath(path))
            .put("dm4.files.size", path.length());
        //
        String mediaType = JavaUtils.getFileType(path.getName());
        if (mediaType != null) {
            childTopics.put("dm4.files.media_type", mediaType);
        }
        //
        return createFileOrFolderTopic(new TopicModel("dm4.files.file", childTopics));      // throws Exception
    }

    /**
     * Creates a Folder topic representing the directory at the given absolute path.
     *
     * @param   path            An absolute path.
     */
    private Topic createFolderTopic(File path) throws Exception {
        String folderName = path.getName();
        String repoPath = repoPath(path);
        //
        // root folder needs special treatment
        if (repoPath.equals("/")) {
            folderName = "";
        }
        //
        ChildTopicsModel childTopics = new ChildTopicsModel()
            .put("dm4.files.folder_name", folderName)
            .put("dm4.files.path",        repoPath);
        //
        return createFileOrFolderTopic(new TopicModel("dm4.files.folder", childTopics));    // throws Exception
    }

    // ---

    private Topic createFileOrFolderTopic(final TopicModel model) throws Exception {
        // We suppress standard workspace assignment here as File and Folder topics require a special assignment
        Topic topic = dms.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Topic>() {  // throws Exception
            @Override
            public Topic call() {
                return dms.createTopic(model);
            }
        });
        createWorkspaceAssignment(topic, repoPath(topic));
        return topic;
    }

    /**
     * @param   topic   a File topic, or a Folder topic.
     */
    private void createFolderAssociation(final long folderTopicId, Topic topic) {
        try {
            final long topicId = topic.getId();
            boolean exists = dms.getAssociations(folderTopicId, topicId, "dm4.core.aggregation").size() > 0;
            if (!exists) {
                // We suppress standard workspace assignment as the folder association requires a special assignment
                Association assoc = dms.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Association>() {
                    @Override
                    public Association call() {
                        return dms.createAssociation(new AssociationModel("dm4.core.aggregation",
                            new TopicRoleModel(folderTopicId, "dm4.core.parent"),
                            new TopicRoleModel(topicId,       "dm4.core.child")
                        ));
                    }
                });
                createWorkspaceAssignment(assoc, repoPath(topic));
            }
        } catch (Exception e) {
            throw new RuntimeException("Creating association to Folder topic " + folderTopicId + " failed", e);
        }
    }

    // ---

    /**
     * Creates a workspace assignment for a File topic, a Folder topic, or a folder association (type "Aggregation").
     * The workspce is calculated from both, the "dm4.filerepo.per_workspace" flag and the given repository path.
     *
     * @param   object  a File topic, a Folder topic, or a folder association (type "Aggregation").
     */
    private void createWorkspaceAssignment(DeepaMehtaObject object, String repoPath) {
        try {
            long workspaceId;
            AccessControl ac = dms.getAccessControl();
            if (FILE_REPOSITORY_PER_WORKSPACE) {
                Matcher m = PER_WORKSPACE_PATH_PATTERN.matcher(repoPath);
                if (m.matches()) {
                    workspaceId = Long.parseLong(m.group(1));
                } else {
                    throw new RuntimeException("No workspace recognized in repository path \"" + repoPath + "\"");
                }
            } else {
                workspaceId = ac.getDeepaMehtaWorkspaceId();
            }
            //
            ac.assignToWorkspace(object, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Creating workspace assignment for File/Folder topic or folder association " +
                "failed", e);
        }
    }



    // === File Repository ===

    /**
     * Maps a repository path to an absolute path.
     * Checks the repository path to fight directory traversal attacks.
     *
     * @param   repoPath    A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *
     * @return  The absolute path.
     */
    private File absolutePath(String repoPath) throws FileRepositoryException {
        try {
            File repo = new File(FILE_REPOSITORY_PATH);
            //
            if (!repo.exists()) {
                throw new RuntimeException("File repository \"" + repo + "\" does not exist");
            }
            //
            if (FILE_REPOSITORY_PER_WORKSPACE && repoPath.equals("/")) {
                repo = new File(repo, "/workspace-" + getWorkspaceId());
                createWorkspaceFileRepository(repo);
            }
            //
            repo = new File(repo, repoPath);
            //
            return checkPath(repo);         // throws FileRepositoryException 403 Forbidden
        } catch (FileRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Mapping repository path \"" + repoPath + "\" to an absolute path failed", e);
        }
    }

    private long getWorkspaceId() {
        Cookies cookies = Cookies.get();
        if (!cookies.has("dm4_workspace_id")) {
            throw new RuntimeException("If \"dm4.filerepo.per_workspace\" is set the request requires a " +
                "\"dm4_workspace_id\" cookie");
        }
        return cookies.getLong("dm4_workspace_id");
    }

    private void createWorkspaceFileRepository(File repo) {
        try {
            if (!repo.exists()) {
                boolean created = repo.mkdir();
                if (created) {
                    logger.info("### Per-workspace file repository created: \"" + repo + "\"");
                } else {
                    throw new RuntimeException("Directory \"" + repo + "\" not created successfully");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Creating per-workspace file repository failed", e);
        }
    }

    // ---

    /**
     * Checks if the absolute path represents a directory traversal attack.
     * If so a FileRepositoryException (403 Forbidden) is thrown.
     *
     * @param   path    The absolute path to check.
     *
     * @return  The canonical form of the absolute path.
     */
    private File checkPath(File path) throws FileRepositoryException, IOException {
        // Note: a directory path returned by getCanonicalPath() never contains a "/" at the end.
        // Thats why "dm4.filerepo.path" is expected to have no "/" at the end as well.
        path = path.getCanonicalFile();     // throws IOException
        boolean pointsToRepository = path.getPath().startsWith(FILE_REPOSITORY_PATH);
        //
        logger.info("Checking path \"" + path + "\"\n  dm4.filerepo.path=" +
            "\"" + FILE_REPOSITORY_PATH + "\" => " + (pointsToRepository ? "PATH OK" : "FORBIDDEN"));
        //
        if (!pointsToRepository) {
            throw new FileRepositoryException("\"" + path + "\" does not point to file repository", Status.FORBIDDEN);
        }
        //
        return path;
    }

    private void checkExistence(File path) throws FileRepositoryException {
        boolean exists = path.exists();
        //
        logger.info("Checking existence of \"" + path + "\" => " + (exists ? "EXISTS" : "NOT FOUND"));
        //
        if (!exists) {
            throw new FileRepositoryException("File or directory \"" + path + "\" does not exist", Status.NOT_FOUND);
        }
    }

    /**
     * Checks if the user associated with a request is authorized to access a repository file.
     * If not authorized a FileRepositoryException (401 Unauthorized) is thrown.
     *
     * @param   path        The absolute path of the file to check (as calculated already from the repository path).
     * @param   repoPath    The repository path of the file to check.
     * @param   request     The request.
     */
    private void checkAuthorization(File path, String repoPath, HttpServletRequest request)
                                                                                        throws FileRepositoryException {
        try {
            if (FILE_REPOSITORY_PER_WORKSPACE) {
                // We check authorization for the repository path by checking access to the corresponding File topic.
                Topic fileTopic = fetchFileTopic(path);
                if (fileTopic != null) {
                    // We must perform access control for the fetchFileTopic() call manually here.
                    //
                    // Although the AccessControlPlugin's PreGetTopicListener kicks in, the request is *not* injected
                    // into the AccessControlPlugin letting fetchFileTopic() effectively run as "System".
                    //
                    // Note: checkAuthorization() is called (indirectly) from an OSGi HTTP service static resource
                    // HttpContext. JAX-RS is not involved here. That's why no JAX-RS injection takes place.
                    String username = dms.getAccessControl().getUsername(request);
                    long fileTopicId = fileTopic.getId();
                    if (!dms.getAccessControl().hasPermission(username, Operation.READ, fileTopicId)) {
                        throw new FileRepositoryException(userInfo(username) + " has no READ permission for " +
                            "repository path \"" + repoPath + "\" (File topic ID=" + fileTopicId + ")",
                            Status.UNAUTHORIZED);
                    }
                } else {
                    throw new RuntimeException("Missing File topic for repository path \"" + repoPath + "\"");
                }
            }
        } catch (FileRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Checking authorization for repository path \"" + repoPath + "\" failed", e);
        }
    }

    private String userInfo(String username) {
        return "user " + (username != null ? "\"" + username + "\"" : "<anonymous>");
    }

    // ---

    /**
     * Constructs an absolute path from an absolute path and a file name.
     *
     * @param   directory   An absolute path.
     *
     * @return  The constructed absolute path.
     */
    private File path(File directory, String fileName) {
        return new File(directory, fileName);
    }

    /**
     * Constructs an absolute path for storing an uploaded file.
     * If a file with that name already exists in the specified directory it remains untouched and the uploaded file
     * is stored with a unique name (by adding a number).
     *
     * @param   directory   The directory to store the uploaded file to.
     *                      An absolute path.
     *
     * @return  The constructed absolute path.
     */
    private File unusedPath(File directory, UploadedFile file) {
        return JavaUtils.findUnusedFile(path(directory, file.getName()));
    }

    // ---

    /**
     * Returns the repository path of a File/Folder topic.
     */
    private String repoPath(long fileTopicId) {
        return repoPath(dms.getTopic(fileTopicId));
    }

    private String repoPath(Topic topic) {
        return topic.getChildTopics().getString("dm4.files.path");
    }

    /**
     * Returns the repository path of a filerepo request.
     *
     * @return  The repository path or <code>null</code> if the request is not a filerepo request.
     */
    public String repoPath(HttpServletRequest request) {
        String repoPath = null;
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith(FILE_REPOSITORY_URI)) {
            repoPath = requestURI.substring(FILE_REPOSITORY_URI.length());
            repoPath = JavaUtils.decodeURIComponent(repoPath);
        }
        return repoPath;
    }
}
