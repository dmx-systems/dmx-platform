package de.deepamehta.plugins.geomaps.service;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;



public interface GeomapsService extends PluginService {

    Geomap getGeomap(long geomapId);

    void addTopicToGeomap(long geomapId, long topicId);

    Topic getGeoTopic(long topicId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);
}
