package de.deepamehta.plugins.geomaps;

import de.deepamehta.plugins.geomaps.model.GeoCoordinate;
import de.deepamehta.plugins.geomaps.model.Geomap;
import de.deepamehta.core.Topic;



public interface GeomapsService {

    Geomap getGeomap(long geomapId);

    /**
     * Finds the domain topic that corresponds to a Geo Coordinate topic.
     */
    Topic getDomainTopic(long geoCoordId);

    /**
     * Returns the geo coordinate of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     *
     * @return  the geo coordinate, or <code>null</code>.
     */
    GeoCoordinate getGeoCoordinate(Topic geoTopic);

    /**
     * Returns the geo coordinate encoded in a Geo Coordinate topic.
     */
    GeoCoordinate geoCoordinate(Topic geoCoordTopic);

    /**
     * Adds a Geo Coordinate topic to a geomap.
     */
    void addCoordinateToGeomap(long geomapId, long geoCoordId);

    void setGeomapState(long geomapId, double lon, double lat, int zoom);

    /**
     * Calculates the distance between 2 geo coordinates in kilometer.
     */
    double getDistance(GeoCoordinate coord1, GeoCoordinate coord2);
}
