package de.deepamehta.plugins.geomaps.service;

import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;



public interface GeomapsService extends PluginService {

    Geomap getGeomap(long geomapId);

    /**
     * Finds the domain topic that corresponds to a Geo Coordinate topic.
     */
    Topic getDomainTopic(long geoCoordId);

    /**
     * Adds a Geo Coordinate topic to a geomap.
     */
    void addCoordinateToGeomap(long geomapId, long geoCoordId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);
}
