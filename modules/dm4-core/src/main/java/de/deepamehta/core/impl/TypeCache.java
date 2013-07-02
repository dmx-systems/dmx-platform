package de.deepamehta.core.impl;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TopicTypeModel;


import java.util.HashMap;
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

    private Map<String, TopicType>       topicTypes = new HashMap();   // key: topic type URI
    private Map<String, AssociationType> assocTypes = new HashMap();   // key: assoc type URI

    private EmbeddedService dms;

    private EndlessRecursionProtection endlessRecursionProtection = new EndlessRecursionProtection();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    TopicType getTopicType(String topicTypeUri) {
        TopicType topicType = topicTypes.get(topicTypeUri);
        if (topicType == null) {
            topicType = loadTopicType(topicTypeUri);
            putTopicType(topicType);
        }
        return topicType;
    }

    AssociationType getAssociationType(String assocTypeUri) {
        AssociationType assocType = assocTypes.get(assocTypeUri);
        if (assocType == null) {
            assocType = loadAssociationType(assocTypeUri);
            putAssociationType(assocType);
        }
        return assocType;
    }

    // ---

    void putTopicType(TopicType topicType) {
        topicTypes.put(topicType.getUri(), topicType);
    }

    void putAssociationType(AssociationType assocType) {
        assocTypes.put(assocType.getUri(), assocType);
    }

    // ---

    void removeTopicType(String topicTypeUri) {
        logger.info("### Removing topic type \"" + topicTypeUri + "\" from type cache");
        if (topicTypes.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    void removeAssociationType(String assocTypeUri) {
        logger.info("### Removing association type \"" + assocTypeUri + "\" from type cache");
        if (assocTypes.remove(assocTypeUri) == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicType loadTopicType(String topicTypeUri) {
        TopicType topicType = null;
        try {
            logger.info("Loading topic type \"" + topicTypeUri + "\"");
            endlessRecursionProtection.check(topicTypeUri);
            //
            TopicTypeModel model = dms.typeStorage.getTopicType(topicTypeUri);
            topicType = new AttachedTopicType(model, dms);
            return topicType;
        } finally {
            // Note: if loading fails (e.g. type URI is invalid) the protection counter must be decremented.
            // Otherwise a 2nd load try would raise a bogus "Endless recursion" exception.
            if (topicType == null) {
                endlessRecursionProtection.uncheck(topicTypeUri);
            }
        }
    }

    private AssociationType loadAssociationType(String assocTypeUri) {
        AssociationType assocType = null;
        try {
            logger.info("Loading association type \"" + assocTypeUri + "\"");
            endlessRecursionProtection.check(assocTypeUri);
            //
            AssociationTypeModel model = dms.typeStorage.getAssociationType(assocTypeUri);
            assocType = new AttachedAssociationType(model, dms);
            return assocType;
        } finally {
            // Note: if loading fails (e.g. type URI is invalid) the protection counter must be decremented.
            // Otherwise a 2nd load try would raise a bogus "Endless recursion" exception.
            if (assocType == null) {
                endlessRecursionProtection.uncheck(assocTypeUri);
            }
        }
    }

    // ---

    private class EndlessRecursionProtection {

        private Map<String, Integer> callCount = new HashMap();

        private void check(String typeUri) {
            int count = incCount(typeUri);
            if (count >= 2) {
                throw new RuntimeException("Endless recursion while loading type \"" + typeUri +
                    "\" (count=" + count + ")");
            }
        }

        private void uncheck(String typeUri) {
            decCount(typeUri);
        }

        // ---

        private int incCount(String typeUri) {
            Integer count = callCount.get(typeUri);
            if (count == null) {
                count = 0;
            }
            count++;
            callCount.put(typeUri, count);
            return count;
        }

        private int decCount(String typeUri) {
            Integer count = callCount.get(typeUri);
            // Note: null check is not required here. Decrement always follows increment.
            count--;
            callCount.put(typeUri, count);
            return count;
        }
    }
}
