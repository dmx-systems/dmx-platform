package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;

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
            topicType = loadTopicType(typeUri);
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

    private AttachedTopicType loadTopicType(String typeUri) {
        Topic typeTopic = dms.getTopic("uri", new TopicValue(typeUri));
        Topic dataType = fetchDataType(typeTopic);
        Set<TopicData> viewConfig = toTopicData(fetchViewConfig(typeTopic));
        TopicTypeData topicTypeData = new TopicTypeData(typeTopic, dataType.getUri(), viewConfig);
        for (Association assoc : typeTopic.getAssociations("dm3.core.whole_topic_type")) {
            String wholeTopicTypeUri = getTopic(assoc, "dm3.core.whole_topic_type").getUri();
            String  partTopicTypeUri = getTopic(assoc, "dm3.core.part_topic_type").getUri();
            // sanity check
            if (!wholeTopicTypeUri.equals(typeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            RoleTypes roleTypes = getRoleTypes(assoc);
            Cardinality cardinality = getCardinality(assoc);
            AssociationDefinition assocDef = new AssociationDefinition(wholeTopicTypeUri, partTopicTypeUri,
                roleTypes.wholeRoleTypeUri, roleTypes.partRoleTypeUri);
            assocDef.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
            assocDef.setPartCardinalityUri(cardinality.partCardinalityUri);
            // FIXME: call assocDef's setUri() and setAssocTypeUri()
            //
            topicTypeData.addAssocDef(assocDef);
        }
        return new AttachedTopicType(topicTypeData, dms);
    }

    // ---

    private Topic fetchDataType(Topic topic) {
        return topic.getRelatedTopic("dm3.core.association", "dm3.core.topic_type", "dm3.core.data_type");
    }

    private Set<Topic> fetchViewConfig(Topic topic) {
        return topic.getRelatedTopics("dm3.core.view_configuration", "dm3.core.topic_type", "dm3.core.view_config");
    }

    private Set<TopicData> toTopicData(Set<Topic> topics) {
        Set topicData = new HashSet();
        for (Topic topic : topics) {
            topicData.add(new TopicData(topic));
        }
        return topicData;
    }

    // ---

    private RoleTypes getRoleTypes(Association assoc) {
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

    private Cardinality getCardinality(Association assoc) {
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
        for (Role role : assoc.getRoles()) {
            if (role.getRoleTypeUri().equals(roleTypeUri)) {
                return dms.storage.getTopic(role.getTopicId());
            }
        }
        return null;
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
