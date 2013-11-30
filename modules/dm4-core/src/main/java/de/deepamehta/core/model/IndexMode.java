package de.deepamehta.core.model;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public enum IndexMode {

    OFF, KEY, FULLTEXT, FULLTEXT_KEY;

    private static final String INDEX_MODES_NAMESPACE = "dm4.core.";

    // -------------------------------------------------------------------------------------------------- Public Methods

    public static List<IndexMode> fromTopics(List<RelatedTopicModel> topics) {
        List<IndexMode> indexModes = new ArrayList();
        for (TopicModel topic : topics) {
            indexModes.add(fromUri(topic.getUri()));
        }
        return indexModes;
    }

    public String toUri() {
        return INDEX_MODES_NAMESPACE + name().toLowerCase();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static List<IndexMode> parse(JSONObject topicTypeModel) {
        try {
            List<IndexMode> indexModes = new ArrayList();
            JSONArray indexModeUris = topicTypeModel.optJSONArray("index_mode_uris");
            if (indexModeUris != null) {
                for (int i = 0; i < indexModeUris.length(); i++) {
                    indexModes.add(fromUri(indexModeUris.getString(i)));
                }
            }
            return indexModes;
        } catch (Exception e) {
            throw new RuntimeException("Parsing index modes failed (topicTypeModel=" + topicTypeModel + ")", e);
        }
    }

    static void toJSON(List<IndexMode> indexModes, JSONObject o) throws Exception {
        List indexModeUris = new ArrayList();
        for (IndexMode indexMode : indexModes) {
            indexModeUris.add(indexMode.toUri());
        }
        o.put("index_mode_uris", indexModeUris);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static IndexMode fromUri(String uri) {
        if (!uri.startsWith(INDEX_MODES_NAMESPACE)) {
            throw new RuntimeException("\"" + uri + "\" is not a valid index mode URI");
        }
        String name = uri.substring(INDEX_MODES_NAMESPACE.length()).toUpperCase();
        return IndexMode.valueOf(name);
    }
}
