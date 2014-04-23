package de.deepamehta.plugins.geomaps.service;

import de.deepamehta.plugins.geomaps.model.GeoCoordinate;
import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;



public interface GeomapsService extends PluginService {

    Geomap getGeomap(long geomapId);

    /**
     * Finds the domain topic that corresponds to a Geo Coordinate topic.
     */
    Topic getDomainTopic(long geoCoordId);

    /**
     * Returns the geo coordinate of a geo-facetted topic (e.g. an Address).
     *
     * @return  the geo coordinate, or <code>null</code> if no geo-facet is stored.
     */
    GeoCoordinate getGeoCoordinate(Topic geoTopic);

    /**
     * Adds a Geo Coordinate topic to a geomap.
     */
    void addCoordinateToGeomap(long geomapId, long geoCoordId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);
}
