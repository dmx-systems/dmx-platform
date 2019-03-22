package systems.dmx.geomaps;

import systems.dmx.core.Topic;

import java.util.List;
import java.util.concurrent.Callable;



public interface GeomapsService {

    Geomap getGeomap(long geomapId);

    /**
     * Finds the domain topics (e.g. Persons, Organizations, Events) that correspond to a Geo Coordinate topic.
     */
    List<Topic> getDomainTopics(long geoCoordId);

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

    void setGeomapState(long geomapId, double lon, double lat, double zoom);

    /**
     * Calculates the distance between 2 geo coordinates in kilometer.
     */
    double getDistance(GeoCoordinate coord1, GeoCoordinate coord2);

    // ---

    /**
     * Executes the passed codeblock and suppresses geocoding for Address topics created/updated while execution.
     *
     * @return  the value returned by the codeblock.
     */
    <V> V runWithoutGeocoding(Callable<V> callable) throws Exception;
}
