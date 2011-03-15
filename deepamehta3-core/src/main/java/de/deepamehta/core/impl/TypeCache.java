package de.deepamehta.core.impl;

import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.Storage;

import de.deepamehta.hypergraph.HyperNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



class TypeCache {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TopicType> topicTypes = new HashMap();
    private DeepaMehtaStorage storage;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(DeepaMehtaStorage storage) {
        this.storage = storage;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    TopicType get(String typeUri) {
        TopicType topicType = topicTypes.get(typeUri);
        if (topicType == null) {
            logger.info("Loading topic type \"" + typeUri + "\" into type cache");
            topicType = loadTopicType(typeUri);
            put(topicType);
        }
        return topicType;
    }

    void put(TopicType topicType) {
        String typeUri = topicType.getUri();
        topicTypes.put(typeUri, topicType);
    }

    void remove(String typeUri) {
        if (topicTypes.remove(typeUri) != null) {
            logger.info("Removing topic type \"" + typeUri + "\" from type cache");
        } else {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicType loadTopicType(String typeUri) {
        HyperNode topicType = storage.lookupTopicType(typeUri);
        return null;
    }
}
