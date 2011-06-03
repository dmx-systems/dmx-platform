package de.deepamehta.core.impl.service;

import de.deepamehta.core.TopicType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A memory-cache for type definitions: topic types and association types.
 * <p>
 * Types are accessed by the {@link get} methods. They are lazy-loaded from the DB.
 * <p>
 * This class is internally used by the {@link EmbeddedService}. The plugin developer accesses topic types via the
 * {@link de.deepamehta.core.service.DeepaMehtaService#getTopicType} core service call.
 */
class TypeCache {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, AttachedTopicType>       topicTypes = new HashMap();   // key: topic type URI
    private Map<String, AttachedAssociationType> assocTypes = new HashMap();   // key: assoc type URI

    private EmbeddedService dms;

    private int callCount = 0;  // endless recursion protection
    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    AttachedTopicType getTopicType(String topicTypeUri) {
        AttachedTopicType topicType = topicTypes.get(topicTypeUri);
        if (topicType == null) {
            // error check
            endlessRecursionProtection(topicTypeUri);
            // fetch topic type
            logger.info("Loading topic type \"" + topicTypeUri + "\"");
            topicType = new AttachedTopicType(dms);
            topicType.fetch(topicTypeUri);
            //
            put(topicType);
        }
        return topicType;
    }

    AttachedAssociationType getAssociationType(String assocTypeUri) {
        AttachedAssociationType assocType = assocTypes.get(assocTypeUri);
        if (assocType == null) {
            // fetch association type
            logger.info("Loading association type \"" + assocTypeUri + "\"");
            assocType = new AttachedAssociationType(dms);
            assocType.fetch(assocTypeUri);
            //
            put(assocType);
        }
        return assocType;
    }

    // ---

    void put(AttachedTopicType topicType) {
        topicTypes.put(topicType.getUri(), topicType);
    }

    void put(AttachedAssociationType assocType) {
        assocTypes.put(assocType.getUri(), assocType);
    }

    // ---

    void invalidate(String topicTypeUri) {
        logger.info("Invalidating topic type \"" + topicTypeUri + "\"");
        if (topicTypes.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void endlessRecursionProtection(String topicTypeUri) {
        if (topicTypeUri.equals("dm3.core.topic_type")) {
            callCount++;
            if (callCount >= 3) {
                throw new RuntimeException("Endless Recursion!");
            }
        }
    }
}
