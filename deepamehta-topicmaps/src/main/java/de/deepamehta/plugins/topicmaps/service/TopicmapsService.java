package de.deepamehta.plugins.topicmaps.service;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.core.service.PluginService;



public interface TopicmapsService extends PluginService {

    Topicmap getTopicmap(long topicmapId);

    long addTopicToTopicmap(long topicmapId, long topicId, int x, int y);

    long addAssociationToTopicmap(long topicmapId, long assocId);

    void removeAssociationFromTopicmap(long topicmapId, long assocId, long refId);

    void setTopicmapTranslation(long topicmapId, int trans_x, int trans_y);
}
