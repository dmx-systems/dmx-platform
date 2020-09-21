package systems.dmx.core.service;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.util.DMXUtils;
import org.codehaus.jettison.json.JSONObject;
import java.util.List;



// TODO: unify with core.QueryResult
public class RelatedTopicResult implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String topicQuery,           assocQuery;
    public String topicTypeUri,         assocTypeUri;
    public boolean searchTopicChildren, searchAssocChildren;
    public List<RelatedTopic> topics;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopicResult(String topicQuery, String topicTypeUri, boolean searchTopicChildren,
                              String assocQuery, String assocTypeUri, boolean searchAssocChildren,
                              List<RelatedTopic> topics) {
        this.topicQuery = topicQuery;
        this.topicTypeUri = topicTypeUri;
        this.searchTopicChildren = searchTopicChildren;
        this.assocQuery = assocQuery;
        this.assocTypeUri = assocTypeUri;
        this.searchAssocChildren = searchAssocChildren;
        this.topics = topics;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("topicQuery", topicQuery)
                .put("topicTypeUri", topicTypeUri)
                .put("searchTopicChildren", searchTopicChildren)
                .put("assocQuery", assocQuery)
                .put("assocTypeUri", assocTypeUri)
                .put("searchAssocChildren", searchAssocChildren)
                .put("topics", DMXUtils.toJSONArray(topics));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
