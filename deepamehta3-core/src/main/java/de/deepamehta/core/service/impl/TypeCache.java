package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.ViewConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeCache(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    AttachedTopicType get(String topicTypeUri) {
        AttachedTopicType topicType = cache.get(topicTypeUri);
        if (topicType == null) {
            logger.info("Loading topic type \"" + topicTypeUri + "\" into type cache");
            topicType = fetchTopicType(topicTypeUri);
            put(topicType);
        }
        return topicType;
    }

    void invalidate(String topicTypeUri) {
        if (cache.remove(topicTypeUri) != null) {
            logger.info("Invalidating topic type \"" + topicTypeUri + "\" in type cache");
        } else {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(AttachedTopicType topicType) {
        cache.put(topicType.getUri(), topicType);
    }

    // ---

    private AttachedTopicType fetchTopicType(String topicTypeUri) {
        // Note: storage low-level call used here ### explain
        Topic typeTopic = dms.storage.getTopic("uri", new TopicValue(topicTypeUri));
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        Map<Long, AssociationDefinition> assocDefs = fetchAssociationDefinitions(typeTopic);
        //
        List<Long> sequenceIds = fetchSequenceIds(typeTopic);
        // sanity check
        if (assocDefs.size() != sequenceIds.size()) {
            throw new RuntimeException("Graph inconsistency: there are " + assocDefs.size() + " association " +
                "definitions but sequence length is " + sequenceIds.size());
        }
        // build topic type
        TopicTypeData topicTypeData = new TopicTypeData(typeTopic, fetchDataTypeTopic(typeTopic).getUri(),
                                                                   fetchIndexModes(typeTopic),
                                                                   fetchViewConfig(typeTopic));
        sortAssociationDefinitions(topicTypeData, assocDefs, sequenceIds);
        //
        return new AttachedTopicType(topicTypeData, dms);
    }

    // ---

    private Map<Long, AssociationDefinition> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AssociationDefinition> assocDefs = new HashMap();
        for (Association assoc : dms.getAssociations(typeTopic.getId(), "dm3.core.topic_type_1")) {
            AssociationDefinition assocDef = fetchAssociationDefinition(assoc, typeTopic.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    /**
     * @param   topicTypeUri    only used for sanity check
     */
    private AssociationDefinition fetchAssociationDefinition(Association assoc, String topicTypeUri) {
        TopicTypes topicTypes = fetchTopicTypes(assoc);
        // ### RoleTypes roleTypes = fetchRoleTypes(assoc);
        Cardinality cardinality = fetchCardinality(assoc);
        // sanity check
        if (!topicTypes.topicTypeUri1.equals(topicTypeUri)) {
            throw new RuntimeException("jri doesn't understand Neo4j traversal");
        }
        //
        AssociationDefinition assocDef = new AssociationDefinition(assoc.getId(),
            topicTypes.topicTypeUri1, topicTypes.topicTypeUri2
            /* ###, roleTypes.roleTypeUri1, roleTypes.roleTypeUri2 */);
        assocDef.setCardinalityUri1(cardinality.cardinalityUri1);
        assocDef.setCardinalityUri2(cardinality.cardinalityUri2);
        assocDef.setAssocTypeUri(assoc.getTypeUri());
        assocDef.setViewConfig(fetchViewConfig(assoc));
        return assocDef;
    }

    // ---

    private List<Long> fetchSequenceIds(Topic typeTopic) {
        try {
            // Note: storage low-level call used here
            // Note: the type topic is not attached to the service
            // ### should dms.getRelatedTopic() get a "includeComposite" parameter?
            List<Long> sequenceIds = new ArrayList();
            Association assocDef = dms.storage.getTopicRelatedAssociation(typeTopic.getId(), "dm3.core.association",
                                                               "dm3.core.topic_type", "dm3.core.first_assoc_def");
            if (assocDef != null) {
                sequenceIds.add(assocDef.getId());
                while ((assocDef = dms.storage.getAssociationRelatedAssociation(assocDef.getId(), "dm3.core.sequence",
                                                               "dm3.core.predecessor", "dm3.core.successor")) != null) {
                    sequenceIds.add(assocDef.getId());
                }
            }
            return sequenceIds;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence IDs for topic type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private void sortAssociationDefinitions(TopicTypeData topicTypeData, Map<Long, AssociationDefinition> assocDefs,
                                                                         List<Long> sequenceIds) {
        for (long assocDefId : sequenceIds) {
            AssociationDefinition assocDef = assocDefs.get(assocDefId);
            // sanity check
            if (assocDef == null) {
                throw new RuntimeException("Graph inconsistency: ID " + assocDefId +
                    " is in sequence but association definition is not found");
            }
            //
            topicTypeData.addAssocDef(assocDef);
        }
    }

    // ---

    private Topic fetchDataTypeTopic(Topic typeTopic) {
        try {
            // Note: storage low-level call used here
            // Note: the type topic is not attached to the service
            // ### should dms.getRelatedTopic() get a "includeComposite" parameter?
            Topic dataType = dms.storage.getRelatedTopic(typeTopic.getId(), "dm3.core.association",
                "dm3.core.topic_type", "dm3.core.data_type", "dm3.core.data_type");
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to topic type \"" + typeTopic.getUri() +
                    "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for topic type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private Set<IndexMode> fetchIndexModes(Topic typeTopic) {
        Set<Topic> topics = dms.getRelatedTopics(typeTopic.getId(), "dm3.core.association",
            "dm3.core.topic_type", "dm3.core.index_mode", "dm3.core.index_mode", false);
        return IndexMode.fromTopics(topics);
    }

    // ---

    private ViewConfiguration fetchViewConfig(Topic typeTopic) {
        // Note: the type topic is not attached to the service
        Set<Topic> topics = dms.getRelatedTopics(typeTopic.getId(), "dm3.core.association",
            "dm3.core.topic_type", "dm3.core.view_config", null, true);
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfiguration(topics);
    }

    private ViewConfiguration fetchViewConfig(Association assoc) {
        Set<Topic> topics = assoc.getTopics("dm3.core.view_config");
        return new ViewConfiguration(topics);
    }

    // ---

    private TopicTypes fetchTopicTypes(Association assoc) {
        String topicTypeUri1 = assoc.getTopic("dm3.core.topic_type_1").getUri();
        String topicTypeUri2 = assoc.getTopic("dm3.core.topic_type_2").getUri();
        return new TopicTypes(topicTypeUri1, topicTypeUri2);
    }

    /* ### private RoleTypes fetchRoleTypes(Association assoc) {
        Topic roleType1 = assoc.getTopic("dm3.core.role_type_1");
        Topic roleType2 = assoc.getTopic("dm3.core.role_type_2");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (roleType1 != null) {
            roleTypes.setRoleTypeUri1(roleType1.getUri());
        }
        if (roleType2 != null) {
            roleTypes.setRoleTypeUri2(roleType2.getUri());
        }
        return roleTypes;
    } */

    private Cardinality fetchCardinality(Association assoc) {
        Topic cardinality1 = assoc.getTopic("dm3.core.cardinality_1");
        Topic cardinality2 = assoc.getTopic("dm3.core.cardinality_2");
        Cardinality cardinality = new Cardinality();
        if (cardinality1 != null) {
            cardinality.setCardinalityUri1(cardinality1.getUri());
        }
        if (cardinality2 != null) {
            cardinality.setCardinalityUri2(cardinality2.getUri());
        } else {
            throw new RuntimeException("Missing cardinality of position 2 in association definition");
        }
        return cardinality;
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    private class TopicTypes {

        private String topicTypeUri1;
        private String topicTypeUri2;

        private TopicTypes(String topicTypeUri1, String topicTypeUri2) {
            this.topicTypeUri1 = topicTypeUri1;
            this.topicTypeUri2 = topicTypeUri2;
        }
    }

    /* ### private class RoleTypes {

        private String roleTypeUri1;
        private String roleTypeUri2;

        private void setRoleTypeUri1(String roleTypeUri1) {
            this.roleTypeUri1 = roleTypeUri1;
        }

        private void setRoleTypeUri2(String roleTypeUri2) {
            this.roleTypeUri2 = roleTypeUri2;
        }
    } */

    private class Cardinality {

        private String cardinalityUri1;
        private String cardinalityUri2;

        private void setCardinalityUri1(String cardinalityUri1) {
            this.cardinalityUri1 = cardinalityUri1;
        }

        private void setCardinalityUri2(String cardinalityUri2) {
            this.cardinalityUri2 = cardinalityUri2;
        }
    }
}
