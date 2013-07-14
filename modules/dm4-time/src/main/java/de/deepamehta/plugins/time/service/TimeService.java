package de.deepamehta.plugins.time.service;

import de.deepamehta.core.service.PluginService;



public interface TimeService extends PluginService {

    long getTopicCreationTime(long topicId);

    long getAssociationCreationTime(long assocId);

    long getTopicModificationTime(long topicId);

    long getAssociationModificationTime(long assocId);
}
