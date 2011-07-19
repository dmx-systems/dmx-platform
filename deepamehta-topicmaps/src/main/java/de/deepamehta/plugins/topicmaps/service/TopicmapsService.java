package de.deepamehta.plugins.topicmaps.service;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.core.service.PluginService;



public interface TopicmapsService extends PluginService {

    public Topicmap getTopicmap(long topicmapId);

    public long addTopicToTopicmap(long topicmapId, long topicId, int x, int y);

    public long addAssociationToTopicmap(long topicmapId, long assocId);

    public void removeAssociationFromTopicmap(long topicmapId, long assocId, long refId);
}
