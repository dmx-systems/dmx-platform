package systems.dmx.topicmaps;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Map;
import java.util.logging.Logger;



/**
 * A topicmap viewmodel: a collection of topics and associations plus their view properties.
 * <p>
 * Features:
 * - Serialization to JSON.
 */
public class Topicmap implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel topicmapTopic;
    private Map<Long, ViewTopic> topics;
    private Map<Long, ViewAssoc> assocs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Topicmap(TopicModel topicmapTopic, Map<Long, ViewTopic> topics, Map<Long, ViewAssoc> assocs) {
        this.topicmapTopic = topicmapTopic;
        this.topics = topics;
        this.assocs = assocs;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return topicmapTopic.getId();
    }

    // ---

    public Iterable<ViewTopic> getTopics() {
        return topics.values();
    }

    public Iterable<ViewAssoc> getAssociations() {
        return assocs.values();
    }

    // ---

    public ViewTopic getTopic(long id) {
        return topics.get(id);
    }

    public ViewAssoc getAssociation(long id) {
        return assocs.get(id);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("info", topicmapTopic.toJSON())
                .put("topics", DMXUtils.toJSONArray(topics.values()))
                .put("assocs", DMXUtils.toJSONArray(assocs.values()));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public String toString() {
        return "topicmap " + getId();
    }
}
