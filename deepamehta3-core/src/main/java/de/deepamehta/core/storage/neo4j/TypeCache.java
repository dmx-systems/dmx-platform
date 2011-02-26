package de.deepamehta.core.storage.neo4j;

import de.deepamehta.core.model.TopicType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



public class TypeCache {

    private Map<String, TopicType> topicTypes = new HashMap();
    private Neo4jStorage storage;

    private Logger logger = Logger.getLogger(getClass().getName());

    TypeCache(Neo4jStorage storage) {
        this.storage = storage;
    }

    // ---

    public TopicType get(String typeUri) {
        TopicType topicType = topicTypes.get(typeUri);
        if (topicType == null) {
            logger.info("Loading topic type \"" + typeUri + "\" into type cache");
            topicType = new Neo4jTopicType(typeUri, storage);
            put(topicType);
        }
        return topicType;
    }

    public void put(TopicType topicType) {
        String typeUri = topicType.getProperty("de/deepamehta/core/property/TypeURI").toString();
        topicTypes.put(typeUri, topicType);
    }

    public void remove(String typeUri) {
        if (topicTypes.remove(typeUri) != null) {
            logger.info("Removing topic type \"" + typeUri + "\" from type cache");
        } else {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found in type cache");
        }
    }
}
