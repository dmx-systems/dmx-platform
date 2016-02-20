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

    private EndlessRecursionDetection endlessRecursionDetection = new EndlessRecursionDetection();

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
        // ### TODO: put in type model cache as well, analogous the cache removal (see below).
        // At the moment we could not do this as the type model must be in cache *before* the topic type is stored
        // (see EmbeddedService#topicTypeFactory()). The aim is to attach the topic type *before* it is stored.
        // But at the moment this in turn is not possible because the relating associations are initialized only
        // while storing (see ValueStorage#associateChildTopic()). The aim is to fully initialize the relating
        // association model *at construction time*, that is when a RelatedTopicModel is constructed. This in turn
        // is not possible as the model classes have no access to the type definitions. The eventual solution is to
        // introduce a model factory which have access to the type definitions.
    }

    void putAssociationType(AssociationType assocType) {
        assocTypes.put(assocType.getUri(), assocType);
        // ### TODO: put in type model cache as well, analogous the cache removal (see below).
        // At the moment we could not do this as the type model must be in cache *before* the assoc type is stored
        // (see EmbeddedService#associationTypeFactory()). The aim is to attach the assoc type *before* it is stored.
        // But at the moment this in turn is not possible because the relating associations are initialized only
        // while storing (see ValueStorage#associateChildTopic()). The aim is to fully initialize the relating
        // association model *at construction time*, that is when a RelatedTopicModel is constructed. This in turn
        // is not possible as the model classes have no access to the type definitions. The eventual solution is to
        // introduce a model factory which have access to the type definitions.
    }

    // ---

    void removeTopicType(String topicTypeUri) {
        logger.info("### Removing topic type \"" + topicTypeUri + "\" from type cache");
        if (topicTypes.remove(topicTypeUri) == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
        dms.pl.typeStorage.removeFromTypeCache(topicTypeUri);
    }

    void removeAssociationType(String assocTypeUri) {
        logger.info("### Removing association type \"" + assocTypeUri + "\" from type cache");
        if (assocTypes.remove(assocTypeUri) == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found in type cache");
        }
        dms.pl.typeStorage.removeFromTypeCache(assocTypeUri);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicType loadTopicType(String topicTypeUri) {
        try {
            logger.info("Loading topic type \"" + topicTypeUri + "\"");
            endlessRecursionDetection.check(topicTypeUri);
            //
            TopicTypeModel model = dms.pl.typeStorage.getTopicType(topicTypeUri);
            return new TopicTypeImpl(model, dms);
        } finally {
            endlessRecursionDetection.reset(topicTypeUri);
        }
    }

    private AssociationType loadAssociationType(String assocTypeUri) {
        try {
            logger.info("Loading association type \"" + assocTypeUri + "\"");
            endlessRecursionDetection.check(assocTypeUri);
            //
            AssociationTypeModel model = dms.pl.typeStorage.getAssociationType(assocTypeUri);
            return new AssociationTypeImpl(model, dms);
        } finally {
            endlessRecursionDetection.reset(assocTypeUri);
        }
    }

    // ---

    private class EndlessRecursionDetection {

        private Map<String, Boolean> loadInProgress = new HashMap();

        private void check(String typeUri) {
            if (loadInProgress.get(typeUri) != null) {
                throw new RuntimeException("Endless recursion detected while loading type \"" + typeUri + "\"");
            }
            loadInProgress.put(typeUri, true);
        }

        private void reset(String typeUri) {
            loadInProgress.remove(typeUri);
        }
    }
}
