package de.deepamehta.core.impl.service;

import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicTypeModel;


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

    // private int callCount = 0;  // endless recursion protection ### FIXME: not in use
    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    AttachedTopicType getTopicType(String topicTypeUri) {
        AttachedTopicType topicType = topicTypes.get(topicTypeUri);
        if (topicType == null) {
            // endlessRecursionProtection(topicTypeUri);    // ### FIXME: not in use
            topicType = loadTopicType(topicTypeUri);
        }
        return topicType;
    }

    AttachedAssociationType getAssociationType(String assocTypeUri) {
        AttachedAssociationType assocType = assocTypes.get(assocTypeUri);
        if (assocType == null) {
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
        logger.info("Invalidating topic type \"" + topicTypeUri + "\"");
        if (topicTypes.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private AttachedTopicType loadTopicType(String topicTypeUri) {
        logger.info("Loading topic type \"" + topicTypeUri + "\"");
        AttachedTopic typeTopic = dms.getTopic("uri", new SimpleValue(topicTypeUri), false);     // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        AttachedTopicType topicType = new AttachedTopicType(dms);
        topicType.fetch(new TopicTypeModel(typeTopic.getModel()));
        put(topicType);
        // Note: the topic type must be put in cache *before* its view configuration is fetched. Othewise endless
        // recursion might occur. Consider this case: fetching the topic type "Icon Source" implies fetching
        // its view configuration which in turn implies fetching the topic type "Icon Source"! This is because
        // "Icon Source" *is part of* "View Configuration" and has a view configuration itself (provided by the
        // deepamehta-iconpicker module).
        // We resolve that circle by postponing the view configuration retrieval. This works because Icon Source's
        // view configuration is actually not required while fetching, but solely its data type is
        // (see AttachedDeepaMehtaObject.fetchComposite()).
        topicType.fetchViewConfig();
        //
        return topicType;
    }

    private AttachedAssociationType loadAssociationType(String assocTypeUri) {
        logger.info("Loading association type \"" + assocTypeUri + "\"");
        AttachedTopic typeTopic = dms.getTopic("uri", new SimpleValue(assocTypeUri), false);     // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        //
        AttachedAssociationType assocType = new AttachedAssociationType(dms);
        assocType.fetch(new AssociationTypeModel(typeTopic.getModel()));
        // Note: the association type must be put in cache *before* its view configuration is fetched. See above.
        put(assocType);
        assocType.fetchViewConfig();
        //
        return assocType;
    }

    // ---

    /* ### FIXME: not in use
    private void endlessRecursionProtection(String topicTypeUri) {
        if (topicTypeUri.equals("dm4.webclient.icon_src")) {
            callCount++;
            logger.info("########## Loading topic type \"dm4.webclient.icon_src\" => count=" + callCount);
            if (callCount >= 2) {
                throw new RuntimeException("Endless Recursion!");
            }
        }
    } */
}
