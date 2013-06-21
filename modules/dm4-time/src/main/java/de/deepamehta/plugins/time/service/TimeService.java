package de.deepamehta.plugins.time.service;

import de.deepamehta.core.service.PluginService;



public interface TimeService extends PluginService {

    long getTimeCreated(long objectId);

    long getTimeModified(long objectId);
}
