package systems.dmx.files;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;

import java.io.File;
import java.io.InputStream;
import java.net.URL;



public interface FilesService {



    // === File System Representation ===

    /**
     * Returns the File topic representing the file at a given repository path.
     * If no such File topic exists it is created.
     *
     * @param   repoPath    A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @return  The File topic. Its child topics ("File Name", "Path", "Media Type", "Size") are included.
     */
    Topic getFileTopic(String repoPath);

    /**
     * Returns the Folder topic representing the folder at a given repository path.
     * If no such Folder topic exists it is created.
     *
     * @param   repoPath    A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @return  The Folder topic. Its child topics ("Folder Name", "Path") are included.
     */
    Topic getFolderTopic(String repoPath);

    // ---

    /**
     * Returns the File topic representing the file at a given repository path.
     * If no such File topic exists it is created.
     * <p>
     * Creates an association (type "Aggregation") between the File topic (role type "Child")
     * and its parent Folder topic (role type "Parent"), if not exists already.
     *
     * @param   repoPath        A repository path. Relative to the repository base path.
     *                          Must begin with slash, no slash at the end.
     *                          <p>
     *                          If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                          the repository path must contain the workspace prefix as the first path segment,
     *                          e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                          <p>
     *                          However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                          as the repository path the workspace prefix is determined automatically with the
     *                          semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                          <p>
     *                          For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @param   folderTopicId   ID of the parent Folder topic.
     *
     * @return  The File topic. Its child topics ("File Name", "Path", "Media Type", "Size") are included.
     */
    RelatedTopic getChildFileTopic(long folderTopicId, String repoPath);

    /**
     * Returns the Folder topic representing the folder at a given repository path.
     * If no such Folder topic exists it is created.
     * <p>
     * Creates an association (type "Aggregation") between the Folder topic (role type "Child")
     * and its parent Folder topic (role type "Parent"), if not exists already.
     *
     * @param   repoPath        A repository path. Relative to the repository base path.
     *                          Must begin with slash, no slash at the end.
     *                          <p>
     *                          If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                          the repository path must contain the workspace prefix as the first path segment,
     *                          e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                          <p>
     *                          However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                          as the repository path the workspace prefix is determined automatically with the
     *                          semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                          <p>
     *                          For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @param   folderTopicId   ID of the parent Folder topic.
     *
     * @return  The Folder topic. Its child topics ("Folder Name", "Path") are included.
     */
    RelatedTopic getChildFolderTopic(long folderTopicId, String repoPath);



    // === File Repository ===

    /**
     * Receives an uploaded file, stores it in the file repository, and creates a corresponding File topic.
     *
     * @param   repoPath    The directory where to store the uploaded file.
     *                      The directory must exist.
     *                      <p>
     *                      A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @return  a StoredFile object which holds 2 information: the name of the uploaded file, and the ID
     *          of the created File topic.
     */
    StoredFile storeFile(UploadedFile file, String repoPath);

    /**
     * Creates a file in the file repository and a corresponding File topic.
     *
     * @param   in          The input stream the file content is read from.
     * @param   repoPath    The path and filename of the file to be created.
     *                      If that file exists already it is overwritten. ### TODO: rethink overwriting
     *                      <p>
     *                      A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @return  the File topic that corresponds to the created file.
     */
    Topic createFile(InputStream in, String repoPath);

    /**
     * Creates a folder in the file repository.
     * Note: no corresponding Folder topic is created.
     *
     * @param   repoPath    The directory where to create the folder.
     *                      <p>
     *                      A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     */
    void createFolder(String folderName, String repoPath);

    // ---

    ResourceInfo getResourceInfo(String repoPath);

    DirectoryListing getDirectoryListing(String repoPath);

    /**
     * Checks if the given URL refers to the file repository of this DMX installation.
     *
     * @return  the refered file's/directory's repository path, or <code>null</code> if the URL
     *          does not refer to the file repository of this DMX installation.
     */
    String getRepositoryPath(URL url);

    // ---

    /**
     * Accesses a file/directory in the file repository by the given repository path.
     * If no such file/directory exists a FileRepositoryException is thrown.
     * <p>
     * Note: this method does not require the corresponding File/Folder <i>topic</i> to exist.
     *
     * @param   repoPath    A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @throws  FileRepositoryException with status code 404 if no such file/directory exists in the file repository.
     */
    File getFile(String repoPath);

    /**
     * Convenience method to access the file/directory in the file repository that is represented by the given
     * File/Folder topic.
     *
     * @param   fileTopicId     ID of a File/Folder topic.
     */
    File getFile(long fileTopicId);

    // ---

    /**
     * Checks if a file/directory with the given repository path exists in the file repository.
     *
     * @param   repoPath    A repository path. Relative to the repository base path.
     *                      Must begin with slash, no slash at the end.
     *                      <p>
     *                      If per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>)
     *                      the repository path must contain the workspace prefix as the first path segment,
     *                      e.g. <code>"/workspace-1234"</code> where <code>1234</code> is the workspace ID.
     *                      <p>
     *                      However there is one exception to that rule: if and only if <code>"/"</code> is passed
     *                      as the repository path the workspace prefix is determined automatically with the
     *                      semantics of <i>current workspace</i>, based on the request's workspace cookie.
     *                      <p>
     *                      For support with constructing a repository path see the {@link pathPrefix} methods.
     *
     * @return  <code>true</code> if the file exists, <code>false</code> otherwise.
     */
    boolean fileExists(String repoPath);

    // ---

    /**
     * Returns a prefix that can be used for constructing a repository path.
     * In case of per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>) the prefix
     * represents the <i>current</i> workspace (e.g. <code>"/workspace-1234"</code>), based on the workspace cookie.
     * In case of per-workspace file repos are <i>not</i> active an empty string is returned.
     */
    String pathPrefix();

    /**
     * Returns a prefix that can be used for constructing a repository path.
     * In case of per-workspace file repos are active (<code>dmx.filerepo.per_workspace=true</code>) the prefix
     * represents the <i>given</i> workspace (e.g. <code>"/workspace-1234"</code>).
     * In case of per-workspace file repos are <i>not</i> active an empty string is returned.
     */
    String pathPrefix(long workspaceId);

    // ---

    int openFile(long fileTopicId);
}
