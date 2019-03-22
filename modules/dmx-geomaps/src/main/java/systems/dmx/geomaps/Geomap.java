package systems.dmx.geomaps;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A geomap model: a collection of Geo Coordinate topics.
 * <p>
 * Features:
 * - Serialization to JSON.
 */
public class Geomap implements Iterable<TopicModel>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel geomapTopic;
    private ViewProps viewProps;
    private Map<Long, TopicModel> geoCoords;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    Geomap(TopicModel geomapTopic, ViewProps viewProps, Map<Long, TopicModel> geoCoords) {
        this.geomapTopic = geomapTopic;
        this.viewProps = viewProps;
        this.geoCoords = geoCoords;
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
                .put("topic", geomapTopic.toJSON())
                .put("viewProps", viewProps.toJSON())
                .put("geoCoordTopics", DMXUtils.toJSONArray(geoCoords.values()));
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
}
