package de.deepamehta.plugins.geomaps.service;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface GeomapsService extends PluginService {

    Geomap getGeomap(long geomapId);

    Topic getGeoTopic(long topicId, ClientState clientState);

    ResultSet<RelatedTopic> getGeomapTopics(long geomapId);

    void addTopicToGeomap(long geomapId, long topicId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);
}
