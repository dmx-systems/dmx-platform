package de.deepamehta.plugins.geomaps.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A geomap model: a collection of Geo Coordinate topic models.
 * <p>
 * Features:
 * - load from DB (by constructor).
 * - Serialization to JSON.
 */
public class Geomap implements Iterable<TopicModel>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Topic geomapTopic;
    protected Map<Long, TopicModel> geoCoords = new HashMap();

    protected DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public Geomap(long geomapId, DeepaMehtaService dms) {
        logger.info("Loading geomap " + geomapId);
        // Note: a Geomap is not a DeepaMehtaObject. So the JerseyResponseFilter's automatic
        // child topic loading is not applied. We must load the child topics manually here.
        this.geomapTopic = dms.getTopic(geomapId).loadChildTopics();
        this.dms = dms;
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
                .put("topics", DeepaMehtaUtils.toJSONArray(geoCoords.values()));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
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

    private ResultList<RelatedTopic> fetchGeoCoordinates(Topic geomapTopic) {
        return geomapTopic.getRelatedTopics("dm4.geomaps.geotopic_mapcontext", "dm4.core.default",
            "dm4.topicmaps.topicmap_topic", "dm4.geomaps.geo_coordinate").loadChildTopics();
    }
}
