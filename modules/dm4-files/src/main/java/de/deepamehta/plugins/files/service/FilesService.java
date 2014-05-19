package de.deepamehta.plugins.files.service;

import de.deepamehta.plugins.files.DirectoryListing;
import de.deepamehta.plugins.files.ResourceInfo;
import de.deepamehta.plugins.files.StoredFile;
import de.deepamehta.plugins.files.UploadedFile;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;

import java.io.File;
import java.net.URL;



public interface FilesService extends PluginService {



    // === File System Representation ===

    /**
     * Creates and returns a File topic representing the file at a given repository path.
     * If such a File topic exists already that topic is returned.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     */
    Topic createFileTopic(String path, ClientState clientState);

    /**
     * Creates and returns a Folder topic representing the folder at a given repository path.
     * If such a Folder topic exists already that topic is returned.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     */
    Topic createFolderTopic(String path, ClientState clientState);

    // ---

    Topic createChildFileTopic(long folderTopicId, String path, ClientState clientState);

    Topic createChildFolderTopic(long folderTopicId, String path, ClientState clientState);



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
    StoredFile storeFile(UploadedFile file, String path, ClientState clientState);

    /**
     * Creates a folder in the file repository.
     * Note: to corresponding Folder topic is created.
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
     * Note: this method doesn't require the corresponding File/Folder topic to exist.
     *
     * @param   path    A repository path. Relative to the repository base path.
     *                  Must begin with slash, no slash at the end.
     */
    File getFile(String path);

    /**
     * Accesses a file/directory in the file repository that is represented by the given File/Folder topic.
     */
    File getFile(long fileTopicId);

    // ---

    void openFile(long fileTopicId);
}
