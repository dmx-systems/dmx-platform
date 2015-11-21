package de.deepamehta.plugins.files;

import de.deepamehta.core.Topic;

import java.io.File;
import java.io.InputStream;
import java.net.URL;



public interface FilesService {



    // === File System Representation ===

    /**
     * Returns the File topic representing the file at a given repository path.
     * If no such File topic exists it is created.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *
     * @return  The File topic. Its child topics ("File Name", "Path", "Media Type", "Size") are included.
     */
    Topic getFileTopic(String path);

    /**
     * Returns the Folder topic representing the folder at a given repository path.
     * If no such Folder topic exists it is created.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *
     * @return  The Folder topic. Its child topics ("Folder Name", "Path") are included.
     */
    Topic getFolderTopic(String path);

    // ---

    /**
     * Returns the File topic representing the file at a given repository path.
     * If no such File topic exists it is created.
     * <p>
     * Creates an association (type "Aggregation") between the File topic (role type "Child")
     * and its parent Folder topic (role type "Parent"), if not exists already.
     *
     * @param   path            A repository path. Relative to the repository base path.
     *                          Must begin with slash, no slash at the end.
     * @param   folderTopicId   ID of the parent Folder topic.
     *
     * @return  The File topic. Its child topics ("File Name", "Path", "Media Type", "Size") are included.
     */
    Topic getChildFileTopic(long folderTopicId, String path);

    /**
     * Returns the Folder topic representing the folder at a given repository path.
     * If no such Folder topic exists it is created.
     * <p>
     * Creates an association (type "Aggregation") between the Folder topic (role type "Child")
     * and its parent Folder topic (role type "Parent"), if not exists already.
     *
     * @param   path            A repository path. Relative to the repository base path.
     *                          Must begin with slash, no slash at the end.
     * @param   folderTopicId   ID of the parent Folder topic.
     *
     * @return  The Folder topic. Its child topics ("Folder Name", "Path") are included.
     */
    Topic getChildFolderTopic(long folderTopicId, String path);



    // === File Repository ===

    /**
     * Receives an uploaded file, stores it in the file repository, and creates a corresponding File topic.
     *
     * @param   path    The directory where to store the uploaded file.
     *                  A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *                  The directory must exist.
     *
     * @return  a StoredFile object which holds 2 information: the name of the uploaded file, and the ID
     *          of the created File topic.
     */
    StoredFile storeFile(UploadedFile file, String path);

    /**
     * Creates a file in the file repository and a corresponding File topic.
     *
     * @param   in      The input stream the file content is read from.
     * @param   path    The path and filename of the file to be created.
     *                  A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *                  If that file exists already it is overwritten. ### TODO: rethink overwriting
     *
     * @return  the File topic that corresponds to the created file.
     */
    Topic createFile(InputStream in, String path);

    /**
     * Creates a folder in the file repository.
     * Note: no corresponding Folder topic is created.
     *
     * @param   path    The directory where to create the folder.
     *                  A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     */
    void createFolder(String folderName, String path);

    // ---

    ResourceInfo getResourceInfo(String path);

    DirectoryListing getDirectoryListing(String path);

    /**
     * Checks if the given URL refers to the file repository of this DeepaMehta installation.
     *
     * @return  the refered file's/directory's repository path, or <code>null</code> if the URL
     *          does not refer to the file repository of this DeepaMehta installation.
     */
    String getRepositoryPath(URL url);

    // ---

    /**
     * Accesses a file/directory in the file repository by the given repository path.
     * If no such file/directory exists a FileRepositoryException is thrown.
     * <p>
     * Note: this method does not require the corresponding File/Folder <i>topic</i> to exist.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *
     * @throws  FileRepositoryException with status code 404 if no such file/directory exists in the file repository.
     */
    File getFile(String path);

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
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     *
     * @return  <code>true</code> if the file exists, <code>false</code> otherwise.
     */
    boolean fileExists(String path);

    // ---

    void openFile(long fileTopicId);
}
