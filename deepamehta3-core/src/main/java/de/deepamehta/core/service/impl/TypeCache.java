package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.ViewConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



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

    AttachedTopicType get(String typeUri) {
        AttachedTopicType topicType = cache.get(typeUri);
        if (topicType == null) {
            logger.info("Loading topic type \"" + typeUri + "\" into type cache");
            topicType = fetchTopicType(typeUri);
            put(topicType);
        }
        return topicType;
    }

    void invalidate(String typeUri) {
        if (cache.remove(typeUri) != null) {
            logger.info("Invalidating topic type \"" + typeUri + "\" in type cache");
        } else {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found in type cache");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(AttachedTopicType topicType) {
        cache.put(topicType.getUri(), topicType);
    }

    // ---

    private AttachedTopicType fetchTopicType(String typeUri) {
        // Note: storage low-level call used here ### explain
        Topic typeTopic = dms.storage.getTopic("uri", new TopicValue(typeUri));
        Topic dataType = fetchDataType(typeTopic);
        TopicTypeData topicTypeData = new TopicTypeData(typeTopic, dataType.getUri(), fetchViewConfig(typeTopic));
        for (Association assoc : dms.storage.getAssociations(typeTopic.getId(), "dm3.core.whole_topic_type")) {
            String wholeTopicTypeUri = getTopic(assoc, "dm3.core.whole_topic_type").getUri();
            String  partTopicTypeUri = getTopic(assoc, "dm3.core.part_topic_type").getUri();
            // sanity check
            if (!wholeTopicTypeUri.equals(typeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            RoleTypes roleTypes = fetchRoleTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            AssociationDefinition assocDef = new AssociationDefinition(wholeTopicTypeUri, partTopicTypeUri,
                roleTypes.wholeRoleTypeUri, roleTypes.partRoleTypeUri);
            assocDef.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
            assocDef.setPartCardinalityUri(cardinality.partCardinalityUri);
            assocDef.setViewConfig(fetchViewConfig(assoc));
            // FIXME: call assocDef's setUri() and setAssocTypeUri()
            //
            topicTypeData.addAssocDef(assocDef);
        }
        return new AttachedTopicType(topicTypeData, dms);
    }

    // ---

    private Topic fetchDataType(Topic typeTopic) {
        // Note: storage low-level call used here ### explain
        Topic dataType = dms.storage.getRelatedTopic(typeTopic.getId(), "dm3.core.association", "dm3.core.topic_type",
                                                                                                "dm3.core.data_type");
        if (dataType == null) {
            throw new RuntimeException("Determining data type failed (topic type=" + typeTopic + ")");
        }
        return dataType;
    }

    // ---

    private ViewConfiguration fetchViewConfig(Topic topic) {
        // ### Note: storage low-level call used here ### explain
        Set<Topic> topics = dms.getRelatedTopics(topic.getId(), "dm3.core.view_configuration",
                                                                "dm3.core.topic_type", "dm3.core.view_config", true);
        return new ViewConfiguration(topics);
    }

    private ViewConfiguration fetchViewConfig(Association assoc) {
        Set<Topic> topics = getTopics(assoc, "dm3.core.view_config");
        return new ViewConfiguration(topics);
    }

    // ---

    private RoleTypes fetchRoleTypes(Association assoc) {
        Topic wholeRoleType = getTopic(assoc, "dm3.core.whole_role_type");
        Topic  partRoleType = getTopic(assoc, "dm3.core.part_role_type");
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
        Topic wholeCardinality = getTopic(assoc, "dm3.core.whole_cardinality");
        Topic  partCardinality = getTopic(assoc, "dm3.core.part_cardinality");
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

    // ---

    // FIXME: introduce attached associations and drop this method
    Topic getTopic(Association assoc, String roleTypeUri) {
        Set<Topic> topics = getTopics(assoc, roleTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity in association: " + topics.size() + " topics have role type \"" +
                roleTypeUri + "\" (" + assoc + ")");
        }
    }

    // FIXME: introduce attached associations and drop this method
    Set<Topic> getTopics(Association assoc, String roleTypeUri) {
        Set<Topic> topics = new HashSet();
        for (Role role : assoc.getRoles()) {
            if (role.getRoleTypeUri().equals(roleTypeUri)) {
                // Note: storage low-level call used here ### explain
                topics.add(dms.storage.getTopic(role.getTopicId()));
            }
        }
        return topics;
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
