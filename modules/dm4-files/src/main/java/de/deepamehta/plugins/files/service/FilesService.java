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
     * Creates a File topic for a given path.
     * If a File topic for that path exists already that topic is returned.
     */
    Topic createFileTopic(String path, ClientState clientState);

    /**
     * Creates a Folder topic for a given path.
     * If a Folder topic for that path exists already that topic is returned.
     */
    Topic createFolderTopic(String path, ClientState clientState);

    // ---

    Topic createChildFileTopic(long folderTopicId, String path, ClientState clientState);

    Topic createChildFolderTopic(long folderTopicId, String path, ClientState clientState);



    // === File Repository ===

    /**
     * Receives an uploaded file, stores it in the DeepaMehta file repository, and creates a corresponding File topic.
     *
     * @param   path    The directory where to store the uploaded file. Relative to the file repository root path.
     *                  Must begin with slash ('/'), no slash at the end. The directory must exist.
     *
     * @return  a StoredFile object which holds 2 information: the name of the uploaded file, and the ID
     *          of the created File topic.
     */
    StoredFile storeFile(UploadedFile file, String path, ClientState clientState);

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
     * Accesses a file/directory in the file repository by its path.
     * Note: this method doesn't require the corresponding File/Folder topic to exist.
     *
     * @param   path    a file repository path. Must begin with slash ('/'), no slash at the end.
     */
    File getFile(String path);

    /**
     * Accesses a file/directory in the file repository that is represented by the given File/Folder topic.
     */
    File getFile(long fileTopicId);

    // ---

    void openFile(long fileTopicId);
}
