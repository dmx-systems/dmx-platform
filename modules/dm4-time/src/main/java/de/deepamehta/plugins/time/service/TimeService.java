package de.deepamehta.plugins.time.service;

import de.deepamehta.core.service.PluginService;



public interface TimeService extends PluginService {

    String getTimeCreated(long objectId);

    String getTimeModified(long objectId);
}
