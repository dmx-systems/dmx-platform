package de.deepamehta.core.model;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public enum IndexMode {

    OFF, KEY, FULLTEXT, FULLTEXT_KEY;

    private static final String INDEX_MODES_NAMESPACE = "dm4.core.";

    // -------------------------------------------------------------------------------------------------- Public Methods

    public static Set<IndexMode> fromTopics(Set<RelatedTopic> topics) {
        Set<IndexMode> indexModes = new HashSet<IndexMode>();
        for (Topic topic : topics) {
            indexModes.add(fromUri(topic.getUri()));
        }
        return indexModes;
    }

    public String toUri() {
        return INDEX_MODES_NAMESPACE + name().toLowerCase();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static Set<IndexMode> parse(JSONObject topicTypeModel) {
        try {
            Set<IndexMode> indexModes = new HashSet<IndexMode>();
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

    static void toJSON(Set<IndexMode> indexModes, JSONObject o) throws Exception {
        List<String> indexModeUris = new ArrayList<String>();
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
