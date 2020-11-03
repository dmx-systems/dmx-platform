package systems.dmx.core;

import systems.dmx.core.util.DMXUtils;
import org.codehaus.jettison.json.JSONObject;
import java.util.List;



public class TopicResult implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String query;
    public String topicTypeUri;
    public boolean searchChildTopics;
    public List<Topic> topics;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicResult(String query, String topicTypeUri, boolean searchChildTopics, List<Topic> topics) {
        this.query = query;
        this.topicTypeUri = topicTypeUri;
        this.searchChildTopics = searchChildTopics;
        this.topics = topics;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("query", query)
                .put("topicTypeUri", topicTypeUri)
                .put("searchChildTopics", searchChildTopics)
                .put("topics", DMXUtils.toJSONArray(topics));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
