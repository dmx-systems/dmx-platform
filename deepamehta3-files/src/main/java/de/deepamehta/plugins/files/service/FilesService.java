package de.deepamehta.plugins.files.service;

import de.deepamehta.plugins.files.model.Resource;
import de.deepamehta.plugins.files.model.ResourceInfo;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.PluginService;

import java.net.URL;



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

    // ---

    public Resource getResource(URL uri, String type, long size);

    public ResourceInfo getResourceInfo(URL uri);
}
