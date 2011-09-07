package de.deepamehta.plugins.proxy.service;

import de.deepamehta.plugins.proxy.model.Resource;
import de.deepamehta.plugins.proxy.model.ResourceInfo;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

import java.net.URL;



public interface ProxyService extends PluginService {

    public Resource getResource(URL uri, String type, long size);

    public ResourceInfo getResourceInfo(URL uri);
}
