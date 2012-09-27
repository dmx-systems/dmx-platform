package de.deepamehta.core.impl.service;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
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

    private Map<String, AttachedTopicType>       topicTypes = new HashMap<String, AttachedTopicType>();       // key: topic type URI
    private Map<String, AttachedAssociationType> assocTypes = new HashMap<String, AttachedAssociationType>(); // key: assoc type URI

    private EmbeddedService dms;

    // ### private EndlessRecursionProtection endlessRecursionProtection = new EndlessRecursionProtection();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    AttachedTopicType getTopicType(String topicTypeUri) {
        AttachedTopicType topicType = topicTypes.get(topicTypeUri);
        if (topicType == null) {
            // ### endlessRecursionProtection.check(topicTypeUri);
            topicType = loadTopicType(topicTypeUri);
        }
        return topicType;
    }

    AttachedAssociationType getAssociationType(String assocTypeUri) {
        AttachedAssociationType assocType = assocTypes.get(assocTypeUri);
        if (assocType == null) {
            // ### endlessRecursionProtection.check(assocTypeUri);
            assocType = loadAssociationType(assocTypeUri);
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
        logger.info("### Invalidating topic type \"" + topicTypeUri + "\"");
        if (topicTypes.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private AttachedTopicType loadTopicType(String topicTypeUri) {
        logger.info("Loading topic type \"" + topicTypeUri + "\"");
        // Note: the low-level storage call prevents possible endless recursion (caused by POST_FETCH_HOOK).
        // Consider the Access Control plugin: loading topic type dm4.accesscontrol.acl_facet would imply
        // loading its ACL which in turn would rely on this very topic type.
        TopicModel model = dms.storage.getTopic("uri", new SimpleValue(topicTypeUri));
        // error check
        if (model == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        AttachedTopicType topicType = new AttachedTopicType(dms);
        topicType.fetch(new TopicTypeModel(model));
        //
        return topicType;
    }

    private AttachedAssociationType loadAssociationType(String assocTypeUri) {
        logger.info("Loading association type \"" + assocTypeUri + "\"");
        TopicModel model = dms.storage.getTopic("uri", new SimpleValue(assocTypeUri));
        // error check
        if (model == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        //
        AttachedAssociationType assocType = new AttachedAssociationType(dms);
        assocType.fetch(new AssociationTypeModel(model));
        //
        return assocType;
    }

    // ---

    @SuppressWarnings("unused")
    private class EndlessRecursionProtection {

        private Map<String, Integer> callCount = new HashMap<String, Integer>();

        private void check(String typeUri) {
            Integer count = callCount.get(typeUri);
            if (count == null) {
                count = 0;
            }
            count++;
            callCount.put(typeUri, count);
            if (count >= 2) {
                throw new RuntimeException("Endless recursion while loading type \"" + typeUri + "\"");
            }
        }
    }
}
