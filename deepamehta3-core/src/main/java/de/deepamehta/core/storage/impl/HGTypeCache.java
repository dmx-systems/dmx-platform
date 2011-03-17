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

    void invalidate(String typeUri) {
        if (cache.remove(typeUri) != null) {
            logger.info("Invalidating topic type \"" + typeUri + "\" in type cache");
        } else {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(TopicTypeDefinition topicTypeDef) {
        cache.put(topicTypeDef.getUri(), topicTypeDef);
    }

    // ---

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
            //
            RoleTypes roleTypes = getRoleTypes(edge);
            Cardinality cardinality = getCardinality(edge);
            AssociationDefinition assocDef = new AssociationDefinition(wholeTopicTypeUri, partTopicTypeUri,
                roleTypes.wholeRoleTypeUri, roleTypes.partRoleTypeUri);
            assocDef.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
            assocDef.setPartCardinalityUri(cardinality.partCardinalityUri);
            // FIXME: call assocDef's setUri() and setAssocTypeUri()
            //
            topicTypeDef.addAssociationDefinition(assocDef);
        }
        return topicTypeDef;
    }

    // ---

    private RoleTypes getRoleTypes(HyperEdge edge) {
        HyperNode wholeRoleType = edge.getHyperNode("dm3.core.whole_role_type");
        HyperNode  partRoleType = edge.getHyperNode("dm3.core.part_role_type");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (wholeRoleType != null) {
            roleTypes.setWholeRoleTypeUri(wholeRoleType.getString("uri"));
        }
        if (partRoleType != null) {
            roleTypes.setPartRoleTypeUri(partRoleType.getString("uri"));
        }
        return roleTypes;
    }

    private Cardinality getCardinality(HyperEdge edge) {
        HyperNode wholeCardinality = edge.getHyperNode("dm3.core.whole_cardinality");
        HyperNode  partCardinality = edge.getHyperNode("dm3.core.part_cardinality");
        Cardinality cardinality = new Cardinality();
        if (wholeCardinality != null) {
            cardinality.setWholeCardinalityUri(wholeCardinality.getString("uri"));
        }
        if (partCardinality != null) {
            cardinality.setPartCardinalityUri(partCardinality.getString("uri"));
        } else {
            throw new RuntimeException("Missing part cardinality in association definition");
        }
        return cardinality;
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    private class RoleTypes {

        private String wholeRoleTypeUri;
        private String  partRoleTypeUri;

        private void setWholeRoleTypeUri(String wholeRoleTypeUri) {
            this.wholeRoleTypeUri = wholeRoleTypeUri;
        }

        private void setPartRoleTypeUri(String partRoleTypeUri) {
            this.partRoleTypeUri = partRoleTypeUri;
        }
    }

    private class Cardinality {

        private String wholeCardinalityUri;
        private String  partCardinalityUri;

        private void setWholeCardinalityUri(String wholeCardinalityUri) {
            this.wholeCardinalityUri = wholeCardinalityUri;
        }

        private void setPartCardinalityUri(String partCardinalityUri) {
            this.partCardinalityUri = partCardinalityUri;
        }
    }
}
