package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.TopicType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A memory-cache for topic type definitions.
 * <p>
 * Types are accessed by the {@link get} method. They are lazy-loaded from the DB.
 * <p>
 * This class is internally used by the {@link EmbeddedService}. The plugin developer accesses topic types via the
 * {@link de.deepamehta.core.service.CoreService#getTopicType} core service call.
 */
class TypeCache {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, AttachedTopicType> cache = new HashMap();   // key: topic type URI
    private EmbeddedService dms;

    private int callCount = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    AttachedTopicType get(String topicTypeUri) {
        AttachedTopicType topicType = cache.get(topicTypeUri);
        if (topicType == null) {
            // error check
            if (topicTypeUri.equals("dm3.core.topic_type")) {
                callCount++;
                if (callCount == 3) {
                    throw new RuntimeException("Endless Recursion!");
                }
            }
            // fetch topic type
            logger.info("Loading topic type \"" + topicTypeUri + "\"");
            topicType = new AttachedTopicType(dms);
            topicType.fetch(topicTypeUri);
            //
            put(topicType);
        }
        return topicType;
    }

    void invalidate(String topicTypeUri) {
        logger.info("Invalidating topic type \"" + topicTypeUri + "\"");
        if (cache.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(AttachedTopicType topicType) {
        cache.put(topicType.getUri(), topicType);
    }
}
