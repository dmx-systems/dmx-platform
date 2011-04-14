package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
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
                                                                   fetchViewConfig(typeTopic));
        sortAssociationDefinitions(topicTypeData, assocDefs, sequenceIds);
        //
        return new AttachedTopicType(topicTypeData, dms);
    }

    // ---

    private Map<Long, AssociationDefinition> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AssociationDefinition> assocDefs = new HashMap();
        for (Association assoc : dms.getAssociations(typeTopic.getId(), "dm3.core.whole_topic_type")) {
            AssociationDefinition assocDef = fetchAssociationDefinition(assoc, typeTopic.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    /**
     * @param   topicTypeUri    only used for sanity check
     */
    private AssociationDefinition fetchAssociationDefinition(Association assoc, String topicTypeUri) {
        TopicTypes  topicTypes  = fetchTopicTypes(assoc);
        RoleTypes   roleTypes   = fetchRoleTypes(assoc);
        Cardinality cardinality = fetchCardinality(assoc);
        // sanity check
        if (!topicTypes.wholeTopicTypeUri.equals(topicTypeUri)) {
            throw new RuntimeException("jri doesn't understand Neo4j traversal");
        }
        //
        AssociationDefinition assocDef = new AssociationDefinition(assoc.getId(),
            topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri,
            roleTypes.wholeRoleTypeUri,   roleTypes.partRoleTypeUri
        );
        assocDef.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
        assocDef.setPartCardinalityUri(cardinality.partCardinalityUri);
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
                                                                        "dm3.core.topic_type", "dm3.core.data_type");
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

    // ---

    private ViewConfiguration fetchViewConfig(Topic typeTopic) {
        // Note: the type topic is not attached to the service
        Set<Topic> topics = dms.getRelatedTopics(typeTopic.getId(), "dm3.core.association", "dm3.core.topic_type",
                                                                                          "dm3.core.view_config", true);
        return new ViewConfiguration(topics);
    }

    private ViewConfiguration fetchViewConfig(Association assoc) {
        Set<Topic> topics = assoc.getTopics("dm3.core.view_config");
        return new ViewConfiguration(topics);
    }

    // ---

    private TopicTypes fetchTopicTypes(Association assoc) {
        String wholeTopicTypeUri = assoc.getTopic("dm3.core.whole_topic_type").getUri();
        String  partTopicTypeUri = assoc.getTopic("dm3.core.part_topic_type").getUri();
        return new TopicTypes(wholeTopicTypeUri, partTopicTypeUri);
    }

    private RoleTypes fetchRoleTypes(Association assoc) {
        Topic wholeRoleType = assoc.getTopic("dm3.core.whole_role_type");
        Topic  partRoleType = assoc.getTopic("dm3.core.part_role_type");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (wholeRoleType != null) {
            roleTypes.setWholeRoleTypeUri(wholeRoleType.getUri());
        }
        if (partRoleType != null) {
            roleTypes.setPartRoleTypeUri(partRoleType.getUri());
        }
        return roleTypes;
    }

    private Cardinality fetchCardinality(Association assoc) {
        Topic wholeCardinality = assoc.getTopic("dm3.core.whole_cardinality");
        Topic  partCardinality = assoc.getTopic("dm3.core.part_cardinality");
        Cardinality cardinality = new Cardinality();
        if (wholeCardinality != null) {
            cardinality.setWholeCardinalityUri(wholeCardinality.getUri());
        }
        if (partCardinality != null) {
            cardinality.setPartCardinalityUri(partCardinality.getUri());
        } else {
            throw new RuntimeException("Missing part cardinality in association definition");
        }
        return cardinality;
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    private class TopicTypes {

        private String wholeTopicTypeUri;
        private String  partTopicTypeUri;

        private TopicTypes(String wholeTopicTypeUri, String partTopicTypeUri) {
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = partTopicTypeUri;
        }
    }

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
