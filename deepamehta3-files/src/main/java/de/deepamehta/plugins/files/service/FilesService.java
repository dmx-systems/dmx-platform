package de.deepamehta.plugins.files.service;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.PluginService;



public interface FilesService extends PluginService {

    /**
     * Creates a File topic for a given path.
     * If a File topic for that path exists already that topic is returned.
     */
    public Topic createFileTopic(String path);

    /**
     * Creates a Folder topic for a given path.
     * If a Folder topic for that path exists already that topic is returned.
     */
    public Topic createFolderTopic(String path);
}
