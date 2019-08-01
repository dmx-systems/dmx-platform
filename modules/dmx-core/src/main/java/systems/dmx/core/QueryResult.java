package systems.dmx.core;

import systems.dmx.core.util.DMXUtils;
import org.codehaus.jettison.json.JSONObject;
import java.util.List;



public class QueryResult implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String query;
    public List<Topic> topics;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public QueryResult(String query, List<Topic> topics) {
        this.query = query;
        this.topics = topics;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("query", query)
                .put("topics", DMXUtils.toJSONArray(topics));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
