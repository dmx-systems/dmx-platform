package systems.dmx.core.model;

import java.util.ArrayList;
import java.util.List;



// TODO: drop it or make package private
public enum IndexMode {

    OFF, KEY, FULLTEXT, FULLTEXT_KEY;

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String INDEX_MODES_NAMESPACE = "dmx.core.";

    // -------------------------------------------------------------------------------------------------- Public Methods

    // TODO: drop it?
    public static List<IndexMode> fromTopics(List<? extends RelatedTopicModel> topics) {
        List<IndexMode> indexModes = new ArrayList();
        for (TopicModel topic : topics) {
            indexModes.add(fromUri(topic.getUri()));
        }
        return indexModes;
    }

    // TODO: drop it?
    public static IndexMode fromUri(String uri) {
        if (!uri.startsWith(INDEX_MODES_NAMESPACE)) {
            throw new RuntimeException("\"" + uri + "\" is not a valid index mode URI");
        }
        String name = uri.substring(INDEX_MODES_NAMESPACE.length()).toUpperCase();
        return IndexMode.valueOf(name);
    }

    // TODO: drop it?
    public String toUri() {
        return INDEX_MODES_NAMESPACE + name().toLowerCase();
    }
}
