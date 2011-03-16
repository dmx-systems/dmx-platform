package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.TopicTypeDefinition;
import de.deepamehta.core.storage.DeepaMehtaStorage;

import de.deepamehta.hypergraph.HyperEdge;
import de.deepamehta.hypergraph.HyperNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



class HGTypeCache {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TopicTypeDefinition> cache = new HashMap();
    private HGStorageBridge storage;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGTypeCache(HGStorageBridge storage) {
        this.storage = storage;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    TopicTypeDefinition get(String typeUri) {
        TopicTypeDefinition topicTypeDef = cache.get(typeUri);
        if (topicTypeDef == null) {
            logger.info("Loading topic type \"" + typeUri + "\" into type cache");
            topicTypeDef = loadTopicTypeDefinition(typeUri);
            put(topicTypeDef);
        }
        return topicTypeDef;
    }

    // FIXME: needed? (putting should be the cache's responsibility)
    void put(TopicTypeDefinition topicTypeDef) {
        String typeUri = topicTypeDef.getUri();
        cache.put(typeUri, topicTypeDef);
    }

    void remove(String typeUri) {
        if (cache.remove(typeUri) != null) {
            logger.info("Removing topic type \"" + typeUri + "\" from type cache");
        } else {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicTypeDefinition loadTopicTypeDefinition(String typeUri) {
        TopicTypeDefinition topicTypeDef = new TopicTypeDefinition(storage.getTopicType(typeUri));
        HyperNode topicType = storage.lookupTopicType(typeUri);
        for (HyperEdge edge : topicType.getHyperEdges("dm3.core.whole_topic_type")) {
            String wholeTopicTypeUri = edge.getHyperNode("dm3.core.whole_topic_type").getString("uri");
            String  partTopicTypeUri = edge.getHyperNode("dm3.core.part_topic_type").getString("uri");
            // sanity check
            if (!wholeTopicTypeUri.equals(typeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            // FIXME: association type = null
            AssociationDefinition assocDef = new AssociationDefinition(null, wholeTopicTypeUri, partTopicTypeUri);
            initRoleTypes(assocDef, edge);
            initCardinality(assocDef, edge);
        }
        return null;
    }

    private void initRoleTypes(AssociationDefinition assocDef, HyperEdge edge) {
        HyperNode wholeRoleType = edge.getHyperNode("dm3.core.whole_role_type");
        HyperNode  partRoleType = edge.getHyperNode("dm3.core.part_role_type");
        // role types are optional
        if (wholeRoleType != null) {
            assocDef.setWholeRoleTypeUri(wholeRoleType.getString("uri"));
        }
        if (partRoleType != null) {
            assocDef.setPartRoleTypeUri(partRoleType.getString("uri"));
        }
    }

    private void initCardinality(AssociationDefinition assocDef, HyperEdge edge) {
        HyperNode wholeCardinality = edge.getHyperNode("dm3.core.whole_cardinality");
        HyperNode  partCardinality = edge.getHyperNode("dm3.core.part_cardinality");
        if (wholeCardinality != null) {
            assocDef.setWholeCardinalityUri(wholeCardinality.getString("uri"));
        }
        if (partCardinality != null) {
            assocDef.setPartCardinalityUri(partCardinality.getString("uri"));
        } else {
            throw new RuntimeException("Missing part cardinality in association definition");
        }
    }
}
