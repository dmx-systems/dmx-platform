package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.TypeStorage;
import de.deepamehta.core.util.DeepaMehtaUtils;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Helper for storing and fetching type models.
 * ### TODO: rename class.
 */
class TypeStorageImpl implements TypeStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TypeModel> typeCache = new HashMap();

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeStorageImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // --------------------------------------------------------------------------------------------------------- Methods



    // === Type Cache ===

    private TypeModel getType(String typeUri) {
        return typeCache.get(typeUri);
    }

    private void putInTypeCache(TypeModel type) {
        typeCache.put(type.getUri(), type);
    }

    // ---

    TopicTypeModel getTopicType(String topicTypeUri) {
        TopicTypeModel topicType = (TopicTypeModel) getType(topicTypeUri);
        return topicType != null ? topicType : fetchTopicType(topicTypeUri);
    }

    AssociationTypeModel getAssociationType(String assocTypeUri) {
        AssociationTypeModel assocType = (AssociationTypeModel) getType(assocTypeUri);
        return assocType != null ? assocType : fetchAssociationType(assocTypeUri);
    }



    // === Types ===

    // --- Fetch ---

    // ### TODO: unify with next method
    private TopicTypeModel fetchTopicType(String topicTypeUri) {
        Topic typeTopic = dms.getTopic("uri", new SimpleValue(topicTypeUri), false);
        checkTopicType(topicTypeUri, typeTopic);
        //
        // 1) fetch type components
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), topicTypeUri, "topic type").getUri();
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
        List<String> labelConfig = fetchLabelConfig(assocDefs);
        ViewConfigurationModel viewConfig = fetchTypeViewConfig(typeTopic);
        //
        // 2) build type model
        TopicTypeModel topicType = new TopicTypeModel(typeTopic.getModel(), dataTypeUri, indexModes,
            assocDefs, labelConfig, viewConfig);
        //
        // 3) put in type cache
        putInTypeCache(topicType);
        //
        return topicType;
    }

    // ### TODO: unify with previous method
    private AssociationTypeModel fetchAssociationType(String assocTypeUri) {
        Topic typeTopic = dms.getTopic("uri", new SimpleValue(assocTypeUri), false);
        checkAssociationType(assocTypeUri, typeTopic);
        //
        // 1) fetch type components
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), assocTypeUri, "association type").getUri();
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
        List<String> labelConfig = fetchLabelConfig(assocDefs);
        ViewConfigurationModel viewConfig = fetchTypeViewConfig(typeTopic);
        //
        // 2) build type model
        AssociationTypeModel assocType = new AssociationTypeModel(typeTopic.getModel(), dataTypeUri, indexModes,
            assocDefs, labelConfig, viewConfig);
        //
        // 3) put in type cache
        putInTypeCache(assocType);
        //
        return assocType;
    }

    // ---

    private void checkTopicType(String topicTypeUri, Topic typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.topic_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.topic_type\"");
        }
    }

    private void checkAssociationType(String assocTypeUri, Topic typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.assoc_type")) {
            throw new RuntimeException("URI \"" + assocTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.assoc_type\"");
        }
    }

    // --- Store ---

    void storeType(TypeModel type) {
        // 1) put in type cache
        // Note: an association type must be put in type cache *before* storing its association definitions.
        // Consider creation of association type "Composition Definition": it has a composition definition itself.
        putInTypeCache(type);
        //
        // 2) store type-specific parts
        associateDataType(type.getUri(), type.getDataTypeUri());
        storeIndexModes(type.getUri(), type.getIndexModes());
        storeAssocDefs(type.getUri(), type.getAssocDefs());
        storeLabelConfig(type.getLabelConfig(), type.getAssocDefs());
        storeViewConfig(createConfigurableType(type.getId()), type.getViewConfigModel());
    }



    // === Data Type ===

    // --- Fetch ---

    private RelatedTopicModel fetchDataTypeTopic(long typeId, String typeUri, String className) {
        try {
            RelatedTopicModel dataType = dms.storage.fetchTopicRelatedTopic(typeId, "dm4.core.aggregation",
                "dm4.core.type", null, "dm4.core.data_type");   // ### FIXME: null
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to " + className + " \"" + typeUri + "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic of " + className + " \"" + typeUri + "\" failed",
                e);
        }
    }

    // --- Store ---

    void storeDataTypeUri(long typeId, String typeUri, String className, String dataTypeUri) {
        // remove current assignment
        long assocId = fetchDataTypeTopic(typeId, typeUri, className).getRelatingAssociation().getId();
        dms.deleteAssociation(assocId);
        // create new assignment
        associateDataType(typeUri, dataTypeUri);
    }

    // ### TODO: drop in favor of _associateDataType() below?
    private void associateDataType(String typeUri, String dataTypeUri) {
        try {
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(typeUri,     "dm4.core.type"),
                new TopicRoleModel(dataTypeUri, "dm4.core.default"));
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + typeUri + "\" with data type \"" +
                dataTypeUri + "\" failed", e);
        }
    }

    // ---

    /**
     * Low-level method. Used for bootstrapping.
     */
    void _associateDataType(String typeUri, String dataTypeUri) {
        AssociationModel assoc = new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(typeUri,     "dm4.core.type"),
            new TopicRoleModel(dataTypeUri, "dm4.core.default"));
        dms.storage.storeAssociation(assoc);
        dms.storage.storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
    }



    // === Index Modes ===

    // --- Fetch ---

    private Set<IndexMode> fetchIndexModes(long typeId) {
        ResultSet<RelatedTopicModel> indexModes = dms.storage.fetchTopicRelatedTopics(typeId, "dm4.core.aggregation",
            "dm4.core.type", null, "dm4.core.index_mode", 0);   // ### FIXME: null
        return IndexMode.fromTopics(indexModes.getItems());
    }

    // --- Store ---

    void storeIndexModes(String typeUri, Set<IndexMode> indexModes) {
        for (IndexMode indexMode : indexModes) {
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(typeUri,           "dm4.core.type"),
                new TopicRoleModel(indexMode.toUri(), "dm4.core.default"));
        }
    }



    // === Association Definitions ===

    // --- Fetch ---

    private List<AssociationDefinitionModel> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AssociationDefinitionModel> assocDefs = fetchAssociationDefinitionsUnsorted(typeTopic);
        List<RelatedAssociationModel> sequence = fetchSequence(typeTopic);
        // error check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("DB inconsistency: type \"" + typeTopic.getUri() + "\" has " +
                assocDefs.size() + " association definitions but in sequence are " + sequence.size());
        }
        //
        return sortAssocDefs(assocDefs, DeepaMehtaUtils.idList(sequence));
    }

    private Map<Long, AssociationDefinitionModel> fetchAssociationDefinitionsUnsorted(Topic typeTopic) {
        Map<Long, AssociationDefinitionModel> assocDefs = new HashMap();
        //
        // 1) fetch child topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered").
        // Note: "othersTopicTypeUri" is set to null. We want consider "dm4.core.topic_type" and "dm4.core.meta_type"
        // as well (the latter required e.g. by dm4-mail) ### TODO: add a getRelatedTopics() method that takes a list
        // of topic types.
        ResultSet<RelatedTopic> childTypes = typeTopic.getRelatedTopics(asList("dm4.core.aggregation_def",
            "dm4.core.composition_def"), "dm4.core.parent_type", "dm4.core.child_type", null, false, false, 0);
            // othersTopicTypeUri=null, fetchComposite=false, fetchRelatingComposite=false, clientState=null
        //
        // 2) create association definitions
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the assoc def's according to the sequence IDs.
        for (RelatedTopic childType : childTypes) {
            AssociationDefinitionModel assocDef = fetchAssociationDefinition(childType.getRelatingAssociation(),
                typeTopic.getUri(), childType.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // ---

    @Override
    public AssociationDefinitionModel fetchAssociationDefinition(Association assoc) {
        return fetchAssociationDefinition(assoc, fetchParentType(assoc).getUri(), fetchChildType(assoc).getUri());
    }

    private AssociationDefinitionModel fetchAssociationDefinition(Association assoc, String parentTypeUri,
                                                                                     String childTypeUri) {
        try {
            long assocId = assoc.getId();
            return new AssociationDefinitionModel(
                assocId, assoc.getUri(), assoc.getTypeUri(),
                parentTypeUri, childTypeUri,
                fetchParentCardinality(assocId).getUri(), fetchChildCardinality(assocId).getUri(),
                fetchAssocDefViewConfig(assoc)
            );
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (parentTypeUri=\"" + parentTypeUri +
                "\", childTypeUri=" + childTypeUri + ", " + assoc + ")", e);
        }
    }

    // ---

    private List<AssociationDefinitionModel> sortAssocDefs(Map<Long, AssociationDefinitionModel> assocDefs,
                                                           List<Long> sequence) {
        List<AssociationDefinitionModel> sortedAssocDefs = new ArrayList();
        for (long assocDefId : sequence) {
            AssociationDefinitionModel assocDef = assocDefs.get(assocDefId);
            // error check
            if (assocDef == null) {
                throw new RuntimeException("DB inconsistency: ID " + assocDefId +
                    " is in sequence but not in the type's association definitions");
            }
            sortedAssocDefs.add(assocDef);
        }
        return sortedAssocDefs;
    }

    // --- Store ---

    private void storeAssocDefs(String typeUri, Collection<AssociationDefinitionModel> assocDefs) {
        for (AssociationDefinitionModel assocDef : assocDefs) {
            storeAssociationDefinition(assocDef);
        }
        storeSequence(typeUri, assocDefs);
    }

    void storeAssociationDefinition(AssociationDefinitionModel assocDef) {
        try {
            // Note: creating the underlying association is conditional. It exists already for
            // an interactively created association definition. Its ID is already set.
            if (assocDef.getId() == -1) {
                dms.createAssociation(assocDef, null);      // clientState=null
            }
            // Note: the assoc def ID is known only after creating the association
            long assocDefId = assocDef.getId();
            // cardinality
            associateParentCardinality(assocDefId, assocDef.getParentCardinalityUri());
            associateChildCardinality(assocDefId, assocDef.getChildCardinalityUri());
            //
            storeViewConfig(createConfigurableAssocDef(assocDefId), assocDef.getViewConfigModel());
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + assocDef.getChildTypeUri() +
                "\" of type \"" + assocDef.getParentTypeUri() + "\" failed", e);
        }
    }



    // === Parent Type / Child Type ===

    // --- Fetch ---

    @Override
    public Topic fetchParentType(Association assoc) {
        Topic parentTypeTopic = assoc.getTopic("dm4.core.parent_type");
        // error check
        if (parentTypeTopic == null) {
            throw new RuntimeException("Invalid association definition: topic role dm4.core.parent_type " +
                "is missing in " + assoc);
        }
        //
        return parentTypeTopic;
    }

    @Override
    public Topic fetchChildType(Association assoc) {
        Topic childTypeTopic = assoc.getTopic("dm4.core.child_type");
        // error check
        if (childTypeTopic == null) {
            throw new RuntimeException("Invalid association definition: topic role dm4.core.child_type " +
                "is missing in " + assoc);
        }
        //
        return childTypeTopic;
    }



    // === Cardinality ===

    // --- Fetch ---

    // ### TODO: pass Association instead ID?
    private RelatedTopicModel fetchParentCardinality(long assocDefId) {
        RelatedTopicModel parentCard = dms.storage.fetchAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.parent_cardinality", "dm4.core.cardinality");
        // error check
        if (parentCard == null) {
            throw new RuntimeException("Invalid association definition: parent cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return parentCard;
    }

    // ### TODO: pass Association instead ID?
    private RelatedTopicModel fetchChildCardinality(long assocDefId) {
        RelatedTopicModel childCard = dms.storage.fetchAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.child_cardinality", "dm4.core.cardinality");
        // error check
        if (childCard == null) {
            throw new RuntimeException("Invalid association definition: child cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return childCard;
    }

    // --- Store ---

    void storeParentCardinalityUri(long assocDefId, String parentCardinalityUri) {
        // remove current assignment
        long assocId = fetchParentCardinality(assocDefId).getRelatingAssociation().getId();
        dms.deleteAssociation(assocId);
        // create new assignment
        associateParentCardinality(assocDefId, parentCardinalityUri);
    }

    void storeChildCardinalityUri(long assocDefId, String childCardinalityUri) {
        // remove current assignment
        long assocId = fetchChildCardinality(assocDefId).getRelatingAssociation().getId();
        dms.deleteAssociation(assocId);
        // create new assignment
        associateChildCardinality(assocDefId, childCardinalityUri);
    }

    // ---

    private void associateParentCardinality(long assocDefId, String parentCardinalityUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(parentCardinalityUri, "dm4.core.parent_cardinality"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }

    private void associateChildCardinality(long assocDefId, String childCardinalityUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(childCardinalityUri, "dm4.core.child_cardinality"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }



    // === Sequence ===

    // --- Fetch ---

    List<RelatedAssociationModel> fetchSequence(Topic typeTopic) {
        try {
            List<RelatedAssociationModel> sequence = new ArrayList();
            // find sequence start
            RelatedAssociation assocDef = typeTopic.getRelatedAssociation("dm4.core.aggregation", "dm4.core.type",
                "dm4.core.sequence_start", null, false, false);     // othersAssocTypeUri=null
            // fetch sequence segments
            if (assocDef != null) {
                sequence.add(assocDef.getModel());
                while ((assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor",
                    "dm4.core.successor")) != null) {
                    //
                    sequence.add(assocDef.getModel());
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    // --- Store ---

    private void storeSequence(String typeUri, Collection<AssociationDefinitionModel> assocDefs) {
        logger.fine("Storing " + assocDefs.size() + " sequence segments for type \"" + typeUri + "\"");
        AssociationDefinitionModel predecessor = null;
        for (AssociationDefinitionModel assocDef : assocDefs) {
            appendToSequence(typeUri, assocDef, predecessor);
            predecessor = assocDef;
        }
    }

    void appendToSequence(String typeUri, AssociationDefinitionModel assocDef, AssociationDefinitionModel predecessor) {
        if (predecessor == null) {
            storeSequenceStart(typeUri, assocDef.getId());
        } else {
            storeSequenceSegment(predecessor.getId(), assocDef.getId());
        }
    }

    private void storeSequenceStart(String typeUri, long assocDefId) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(typeUri, "dm4.core.type"),
            new AssociationRoleModel(assocDefId, "dm4.core.sequence_start"));
    }

    private void storeSequenceSegment(long predAssocDefId, long succAssocDefId) {
        dms.createAssociation("dm4.core.sequence",
            new AssociationRoleModel(predAssocDefId, "dm4.core.predecessor"),
            new AssociationRoleModel(succAssocDefId, "dm4.core.successor"));
    }

    // ---

    void rebuildSequence(Type type) {
        deleteSequence(type);
        storeSequence(type.getUri(), type.getModel().getAssocDefs());
    }

    private void deleteSequence(Topic typeTopic) {
        List<RelatedAssociationModel> sequence = fetchSequence(typeTopic);
        logger.info("### Deleting " + sequence.size() + " sequence segments of type \"" + typeTopic.getUri() + "\"");
        for (RelatedAssociationModel assoc : sequence) {
            long assocId = assoc.getRelatingAssociation().getId();
            dms.deleteAssociation(assocId);
        }
    }



    // === Label Configuration ===

    // --- Fetch ---

    private List<String> fetchLabelConfig(List<AssociationDefinitionModel> assocDefs) {
        List<String> labelConfig = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            RelatedTopicModel includeInLabel = fetchLabelConfigTopic(assocDef.getId());
            if (includeInLabel != null && includeInLabel.getSimpleValue().booleanValue()) {
                labelConfig.add(assocDef.getChildTypeUri());
            }
        }
        return labelConfig;
    }

    private RelatedTopicModel fetchLabelConfigTopic(long assocDefId) {
        return dms.storage.fetchAssociationRelatedTopic(assocDefId, "dm4.core.composition",
            "dm4.core.parent", "dm4.core.child", "dm4.core.include_in_label");
    }

    // --- Store ---

    void storeLabelConfig(List<String> labelConfig, Collection<AssociationDefinitionModel> assocDefs) {
        for (AssociationDefinitionModel assocDef : assocDefs) {
            boolean includeInLabel = labelConfig.contains(assocDef.getChildTypeUri());
            new AttachedAssociationDefinition(assocDef, dms).getCompositeValue()
                .set("dm4.core.include_in_label", includeInLabel, null, new Directives());
        }
    }



    // === View Configurations ===

    // --- Fetch ---

    private ViewConfigurationModel fetchTypeViewConfig(Topic typeTopic) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            ResultSet<RelatedTopic> configTopics = typeTopic.getRelatedTopics("dm4.core.aggregation",
                "dm4.core.type", "dm4.core.view_config", null, true, false, 0);
            return new ViewConfigurationModel(DeepaMehtaUtils.toTopicModels(configTopics.getItems()));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private ViewConfigurationModel fetchAssocDefViewConfig(Association assocDef) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            ResultSet<RelatedTopic> configTopics = assocDef.getRelatedTopics("dm4.core.aggregation",
                "dm4.core.assoc_def", "dm4.core.view_config", null, true, false, 0);
            return new ViewConfigurationModel(DeepaMehtaUtils.toTopicModels(configTopics.getItems()));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for association definition " + assocDef.getId() +
                " failed", e);
        }
    }

    // ---

    private RelatedTopicModel fetchTypeViewConfigTopic(long typeId, String configTypeUri) {
        // Note: the composite is not fetched as it is not needed
        return dms.storage.fetchTopicRelatedTopic(typeId, "dm4.core.aggregation",
            "dm4.core.type", "dm4.core.view_config", configTypeUri);
    }

    private RelatedTopicModel fetchAssocDefViewConfigTopic(long assocDefId, String configTypeUri) {
        // Note: the composite is not fetched as it is not needed
        return dms.storage.fetchAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.view_config", configTypeUri);
    }

    // ---

    private TopicModel fetchViewConfigTopic(RoleModel configurable, String configTypeUri) {
        if (configurable instanceof TopicRoleModel) {
            long typeId = configurable.getPlayerId();
            return fetchTypeViewConfigTopic(typeId, configTypeUri);
        } else if (configurable instanceof AssociationRoleModel) {
            long assocDefId = configurable.getPlayerId();
            return fetchAssocDefViewConfigTopic(assocDefId, configTypeUri);
        } else {
            throw new RuntimeException("Unexpected configurable: " + configurable);
        }
    }

    // --- Store ---

    private void storeViewConfig(RoleModel configurable, ViewConfigurationModel viewConfig) {
        try {
            for (TopicModel configTopic : viewConfig.getConfigTopics()) {
                storeViewConfigTopic(configurable, configTopic);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration failed (configurable=" + configurable + ")", e);
        }
    }

    void storeViewConfigTopic(RoleModel configurable, TopicModel configTopic) {
        // Note: null is passed as clientState. Called only (indirectly) from a migration ### FIXME: is this true?
        // and in a migration we have no clientState anyway.
        Topic topic = dms.createTopic(configTopic, null);   // clientState=null
        dms.createAssociation("dm4.core.aggregation", configurable,
            new TopicRoleModel(topic.getId(), "dm4.core.view_config"));
    }

    // ---

    /**
     * Prerequisite: for the configurable a config topic of type configTypeUri exists in the DB.
     */
    void storeViewConfigSetting(RoleModel configurable, String configTypeUri, String settingUri, Object value) {
        try {
            TopicModel configTopic = fetchViewConfigTopic(configurable, configTypeUri);
            // ### TODO: do not create an attached topic here. Can we use the value storage?
            new AttachedTopic(configTopic, dms).getCompositeValue().set(settingUri, value, null, new Directives());
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration setting failed (configurable=" + configurable +
                ", configTypeUri=\"" + configTypeUri + "\", settingUri=\"" + settingUri + "\", value=\"" + value +
                "\")", e);
        }
    }

    // --- Helper ---

    RoleModel createConfigurableType(long typeId) {
        return new TopicRoleModel(typeId, "dm4.core.type");
    }

    RoleModel createConfigurableAssocDef(long assocDefId) {
        return new AssociationRoleModel(assocDefId, "dm4.core.assoc_def");
    }
}
