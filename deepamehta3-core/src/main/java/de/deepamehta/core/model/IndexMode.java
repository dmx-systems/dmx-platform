package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public enum IndexMode {

    OFF, KEY, FULLTEXT, FULLTEXT_KEY;

    private static final String INDEX_MODES_NAMESPACE = "dm3.core.";

    // -------------------------------------------------------------------------------------------------- Public Methods

    public static Set<IndexMode> fromTopics(Set<Topic> topics) {
        Set<IndexMode> indexModes = new HashSet();
        for (Topic topic : topics) {
            indexModes.add(fromUri(topic.getUri()));
        }
        return indexModes;
    }

    public String toUri() {
        return INDEX_MODES_NAMESPACE + name().toLowerCase();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static Set<IndexMode> parse(JSONObject topicTypeData) {
        try {
            Set<IndexMode> indexModes = new HashSet();
            JSONArray indexModeUris = topicTypeData.optJSONArray("index_mode_uris");
            if (indexModeUris != null) {
                for (int i = 0; i < indexModeUris.length(); i++) {
                    indexModes.add(fromUri(indexModeUris.getString(i)));
                }
            }
            return indexModes;
        } catch (Exception e) {
            throw new RuntimeException("Parsing index modes failed (topicTypeData=" + topicTypeData + ")", e);
        }
    }

    static void toJSON(Set<IndexMode> indexModes, JSONObject o) throws Exception {
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
