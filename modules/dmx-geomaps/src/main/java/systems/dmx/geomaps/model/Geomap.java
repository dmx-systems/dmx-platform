package systems.dmx.geomaps.model;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A geomap model: a collection of Geo Coordinate topic models.
 * <p>
 * Features:
 * - load from DB (by constructor).
 * - Serialization to JSON.
 *
 * ### TODO: rename to GeomapViewmodel
 */
public class Geomap implements Iterable<TopicModel>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Topic geomapTopic;
    protected Map<Long, TopicModel> geoCoords = new HashMap();

    protected CoreService dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     *
     * ### TODO: pass geomap data instead of retrieving in constructor. Don't pass core service.
     */
    public Geomap(long geomapId, CoreService dmx) {
        logger.info("Loading geomap " + geomapId);
        // Note: a Geomap is not a DMXObject. So the JerseyResponseFilter's automatic
        // child topic loading is not applied. We must load the child topics manually here.
        this.geomapTopic = dmx.getTopic(geomapId).loadChildTopics();
        this.dmx = dmx;
        //
        fetchGeoCoordinates();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return geomapTopic.getId();
    }

    // ### TODO: needed?
    public boolean containsTopic(long geoCoordId) {
        return geoCoords.get(geoCoordId) != null;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("info", geomapTopic.toJSON())
                .put("topics", DMXUtils.toJSONArray(geoCoords.values()));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public Iterator<TopicModel> iterator() {
        return geoCoords.values().iterator();
    }

    @Override
    public String toString() {
        return "geomap " + getId();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void fetchGeoCoordinates() {
        for (Topic geoCoord : fetchGeoCoordinates(geomapTopic)) {
            geoCoords.put(geoCoord.getId(), geoCoord.getModel());
        }
    }

    private List<RelatedTopic> fetchGeoCoordinates(Topic geomapTopic) {
        return DMXUtils.loadChildTopics(geomapTopic.getRelatedTopics("dmx.geomaps.geotopic_mapcontext",
            "dmx.core.default", "dmx.topicmaps.topicmap_topic", "dmx.geomaps.geo_coordinate"));
    }
}
