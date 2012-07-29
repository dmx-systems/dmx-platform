package de.deepamehta.plugins.files.service;

import de.deepamehta.plugins.files.StoredFile;
import de.deepamehta.plugins.files.UploadedFile;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;

import java.net.URL;



public interface FilesService extends PluginService {

    /**
     * Creates a File topic for a given path.
     * If a File topic for that path exists already that topic is returned.
     */
    Topic createFileTopic(String path);

    /**
     * Creates a Folder topic for a given path.
     * If a Folder topic for that path exists already that topic is returned.
     */
    Topic createFolderTopic(String path);

    // ---

    Topic createChildFileTopic(long folderTopicId, String path);

    Topic createChildFolderTopic(long folderTopicId, String path);

    // ---

    StoredFile storeFile(UploadedFile file);

    // ---

    void openFile(long fileTopicId);
}
