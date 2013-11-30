package de.deepamehta.plugins.geomaps.service;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;



public interface GeomapsService extends PluginService {

    Geomap getGeomap(long geomapId);

    /**
     * Retrieves the domain topic that corresponds to a Geo Coordinate topic.
     * ### TODO: rename method and argument
     */
    Topic getGeoTopic(long topicId);

    // ### TODO: drop this
    ResultList<RelatedTopic> getGeomapTopics(long geomapId);

    /**
     * Adds a Geo Coordinate topic to a geomap.
     * ### TODO: rename method and argument
     */
    void addTopicToGeomap(long geomapId, long topicId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);
}
