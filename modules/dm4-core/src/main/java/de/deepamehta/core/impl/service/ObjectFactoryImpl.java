package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
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
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ObjectFactory;
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
class ObjectFactoryImpl implements ObjectFactory {

    private static final String DEFAULT_URI_PREFIX = "domain.project.topic_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TypeModel> typeCache = new HashMap();

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectFactoryImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // --------------------------------------------------------------------------------------------------------- Methods



    // === DeepaMehta Objects (Topics and Associations) ===

    // --- Fetch ---

    RelatedTopicModel fetchTopicTypeTopic(long topicId) {
        return dms.storage.getTopicRelatedTopic(topicId, "dm4.core.instantiation",
            "dm4.core.instance", "dm4.core.type", "dm4.core.topic_type");
    }

    RelatedTopicModel fetchAssociationTypeTopic(long assocId) {
        // assocTypeUri=null (supposed to be "dm4.core.instantiation" but not possible ### explain)
        return dms.storage.getAssociationRelatedTopic(assocId, null,
            "dm4.core.instance", "dm4.core.type", "dm4.core.assoc_type");
    }

    // --- Store ---

    /**
     * Low-level method. Used for bootstrapping.
     */
    void _createTopic(TopicModel model) {
        // Note: low-level (storage) call used here ### explain
        dms.storage.createTopic(model);
        dms.storage.setTopicValue(model.getId(), model.getSimpleValue());
    }

    // ---

    Topic storeTopic(TopicModel model, ClientState clientState, Directives directives) {
        setDefaults(model);
        dms.storage.createTopic(model);
        associateWithTopicType(model.getId(), model.getTypeUri());
        //
        AttachedTopic topic = new AttachedTopic(model, dms);
        topic.storeValue(clientState, directives);
        //
        return topic;
    }

    Association storeAssociation(AssociationModel model, ClientState clientState, Directives directives) {
        setDefaults(model);
        dms.storage.createAssociation(model);
        associateWithAssociationType(model.getId(), model.getTypeUri());
        //
        AttachedAssociation assoc = new AttachedAssociation(model, dms);
        assoc.storeValue(clientState, directives);
        //
        return assoc;
    }

    // ---

    // ### TODO: differentiate between a model and an update model and then drop this method
    private void setDefaults(DeepaMehtaObjectModel model) {
        if (model.getUri() == null) {
            model.setUri("");
        }
        if (model.getSimpleValue() == null) {
            model.setSimpleValue("");
        }
    }

    // ---

    void associateWithTopicType(long topicId, String topicTypeUri) {
        try {
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(topicTypeUri, "dm4.core.type"),
                new TopicRoleModel(topicId, "dm4.core.instance"));
            dms.storage.createAssociation(assoc);
            dms.storage.setAssociationValue(assoc.getId(), assoc.getSimpleValue());
            associateWithAssociationType(assoc.getId(), assoc.getTypeUri());
            // low-level (storage) call used here ### explain
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId +
                " with topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void associateWithAssociationType(long assocId, String assocTypeUri) {
        try {
            AssociationModel assoc = new AssociationModel("dm4.core.instantiation",
                new TopicRoleModel(assocTypeUri, "dm4.core.type"),
                new AssociationRoleModel(assocId, "dm4.core.instance"));
            dms.storage.createAssociation(assoc);  // low-level (storage) call used here ### explain
            dms.storage.setAssociationValue(assoc.getId(), assoc.getSimpleValue());
        } catch (Exception e) {
            throw new RuntimeException("Associating association " + assocId +
                " with association type \"" + assocTypeUri + "\" failed", e);
        }
    }



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

    private TopicTypeModel fetchTopicType(String topicTypeUri) {
        Topic typeTopic = dms.getTopic("uri", new SimpleValue(topicTypeUri), false, null);
        checkTopicType(topicTypeUri, typeTopic);
        //
        // 1) fetch type components
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), topicTypeUri, "topic type").getUri();
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic.getId(), topicTypeUri,
            "topic type");
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

    private AssociationTypeModel fetchAssociationType(String assocTypeUri) {
        Topic typeTopic = dms.getTopic("uri", new SimpleValue(assocTypeUri), false, null);
        checkAssociationType(assocTypeUri, typeTopic);
        //
        // 1) fetch type components
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), assocTypeUri, "association type").getUri();
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic.getId(), assocTypeUri,
            "association type");
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
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.topic_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.topic_type\"");
        }
    }

    private void checkAssociationType(String assocTypeUri, Topic typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.assoc_type")) {
            throw new RuntimeException("URI \"" + assocTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.assoc_type\"");
        }
    }

    // --- Store ---

    void storeType(TypeModel type) {
        // 1) store the base-topic parts ### FIXME: call storeTopic() instead?
        dms.storage.createTopic(type);
        associateWithTopicType(type.getId(), type.getTypeUri());
        // Note: the created AttachedTopic is just a temporary vehicle to
        // let us call its setUri() and storeAndIndexValue() methods.
        AttachedTopic typeTopic = new AttachedTopic(type, dms);
        // If no URI is set the type gets a default URI based on its ID.
        // Note: this must be done *after* the topic is created. The ID is not known before.
        if (typeTopic.getUri().equals("")) {
            typeTopic.setUri(DEFAULT_URI_PREFIX + type.getId());
        }
        //
        typeTopic.storeAndIndexValue(type.getSimpleValue());
        //
        // 2) put in type cache
        // Note: an association type must be put in type cache *before* storing its association definitions.
        // Consider creation of association type "Composition Definition": it has a composition definition itself.
        putInTypeCache(type);
        //
        // 3) store the type-specific parts
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
            RelatedTopicModel dataType = dms.storage.getTopicRelatedTopic(typeId, "dm4.core.aggregation",
                "dm4.core.type", null, "dm4.core.data_type");   // ### FIXME: null
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to " + className + " \"" + typeUri + "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for " + className + " \"" + typeUri + "\" failed",
                e);
        }
    }

    // --- Store ---

    void storeDataTypeUri(long typeId, String typeUri, String className, String dataTypeUri) {
        // remove current assignment
        long assocId = fetchDataTypeTopic(typeId, typeUri, className).getAssociationModel().getId();
        dms.deleteAssociation(assocId, null);   // clientState=null
        // create new assignment
        associateDataType(typeUri, dataTypeUri);
    }

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
        dms.storage.createAssociation(assoc);
        dms.storage.setAssociationValue(assoc.getId(), assoc.getSimpleValue());
        associateWithAssociationType(assoc.getId(), assoc.getTypeUri());
    }



    // === Index Modes ===

    // --- Fetch ---

    private Set<IndexMode> fetchIndexModes(long typeId) {
        ResultSet<RelatedTopicModel> indexModes = dms.storage.getTopicRelatedTopics(typeId, "dm4.core.aggregation",
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

    private List<AssociationDefinitionModel> fetchAssociationDefinitions(long typeId, String typeUri,
                                                                                      String className) {
        Map<Long, AssociationDefinitionModel> assocDefs = fetchAssociationDefinitionsUnsorted(typeId, typeUri);
        List<RelatedAssociationModel> sequence = fetchSequence(typeId, typeUri, className);
        // error check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("DB inconsistency: for " + className + " \"" + typeUri + "\" there are " +
                assocDefs.size() + " association definitions but sequence has " + sequence.size());
        }
        //
        return sortAssocDefs(assocDefs, DeepaMehtaUtils.idList(sequence));
    }

    private Map<Long, AssociationDefinitionModel> fetchAssociationDefinitionsUnsorted(long typeId, String typeUri) {
        Map<Long, AssociationDefinitionModel> assocDefs = new HashMap();
        //
        // 1) fetch part topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered"). ### FIXDOC
        ResultSet<RelatedTopicModel> partTopicTypes = dms.storage.getTopicRelatedTopics(typeId,
            asList("dm4.core.aggregation_def", "dm4.core.composition_def"), "dm4.core.whole_type", "dm4.core.part_type",
            "dm4.core.topic_type", 0);
        //
        // 2) create association definitions
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the assoc def's according to the sequence IDs.
        for (RelatedTopicModel partTopicType : partTopicTypes) {
            AssociationDefinitionModel assocDef = fetchAssociationDefinition(
                partTopicType.getAssociationModel(), typeUri, partTopicType.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // ---

    @Override
    public AssociationDefinitionModel fetchAssociationDefinition(Association assoc) {
        return fetchAssociationDefinition(assoc.getModel(), fetchWholeTopicType(assoc).getUri(),
                                                            fetchPartTopicType(assoc).getUri());
    }

    private AssociationDefinitionModel fetchAssociationDefinition(AssociationModel assoc, String wholeTopicTypeUri,
                                                                                          String partTopicTypeUri) {
        try {
            long assocId = assoc.getId();
            return new AssociationDefinitionModel(assocId, assoc.getTypeUri(), wholeTopicTypeUri, partTopicTypeUri,
                fetchWholeCardinality(assocId).getUri(), fetchPartCardinality(assocId).getUri(),
                fetchAssocDefViewConfig(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (wholeTopicTypeUri=\"" +
                wholeTopicTypeUri + "\", partTopicTypeUri=" + partTopicTypeUri + ", " + assoc + ")", e);
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
            // role types
            associateWholeRoleType(assocDefId, assocDef.getWholeRoleTypeUri());
            associatePartRoleType(assocDefId, assocDef.getPartRoleTypeUri());
            // cardinality
            associateWholeCardinality(assocDefId, assocDef.getWholeCardinalityUri());
            associatePartCardinality(assocDefId, assocDef.getPartCardinalityUri());
            //
            storeViewConfig(createConfigurableAssocDef(assocDefId), assocDef.getViewConfigModel());
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + assocDef.getUri() +
                "\" of type \"" + assocDef.getWholeTopicTypeUri() + "\" failed", e);
        }
    }



    // === Whole Type / Part Type ===

    // --- Fetch ---

    @Override
    public Topic fetchWholeTopicType(Association assoc) {
        Topic wholeTypeTopic = assoc.getTopic("dm4.core.whole_type");
        // error check
        if (wholeTypeTopic == null) {
            throw new RuntimeException("Invalid association definition: topic role dm4.core.whole_type " +
                "is missing in " + assoc);
        }
        //
        return wholeTypeTopic;
    }

    @Override
    public Topic fetchPartTopicType(Association assoc) {
        Topic partTypeTopic = assoc.getTopic("dm4.core.part_type");
        // error check
        if (partTypeTopic == null) {
            throw new RuntimeException("Invalid association definition: topic role dm4.core.part_type " +
                "is missing in " + assoc);
        }
        //
        return partTypeTopic;
    }



    // === Role Types ===

    private void associateWholeRoleType(long assocDefId, String wholeRoleTypeUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(wholeRoleTypeUri, "dm4.core.whole_role_type"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }

    private void associatePartRoleType(long assocDefId, String partRoleTypeUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(partRoleTypeUri,  "dm4.core.part_role_type"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }



    // === Cardinality ===

    // --- Fetch ---

    private RelatedTopicModel fetchWholeCardinality(long assocDefId) {
        RelatedTopicModel wholeCard = dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.whole_cardinality", "dm4.core.cardinality");
        // error check
        if (wholeCard == null) {
            throw new RuntimeException("Invalid association definition: whole cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return wholeCard;
    }

    private RelatedTopicModel fetchPartCardinality(long assocDefId) {
        RelatedTopicModel partCard = dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.part_cardinality", "dm4.core.cardinality");
        // error check
        if (partCard == null) {
            throw new RuntimeException("Invalid association definition: part cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return partCard;
    }

    // --- Store ---

    void storeWholeCardinalityUri(long assocDefId, String wholeCardinalityUri) {
        // remove current assignment
        long assocId = fetchWholeCardinality(assocDefId).getAssociationModel().getId();
        dms.deleteAssociation(assocId, null);   // clientState=null
        // create new assignment
        associateWholeCardinality(assocDefId, wholeCardinalityUri);
    }

    void storePartCardinalityUri(long assocDefId, String partCardinalityUri) {
        // remove current assignment
        long assocId = fetchPartCardinality(assocDefId).getAssociationModel().getId();
        dms.deleteAssociation(assocId, null);   // clientState=null
        // create new assignment
        associatePartCardinality(assocDefId, partCardinalityUri);
    }

    // ---

    private void associateWholeCardinality(long assocDefId, String wholeCardinalityUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(wholeCardinalityUri, "dm4.core.whole_cardinality"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }

    private void associatePartCardinality(long assocDefId, String partCardinalityUri) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(partCardinalityUri, "dm4.core.part_cardinality"),
            new AssociationRoleModel(assocDefId, "dm4.core.assoc_def"));
    }



    // === Sequence ===

    // --- Fetch ---

    List<RelatedAssociationModel> fetchSequence(long typeId, String typeUri, String className) {
        try {
            List<RelatedAssociationModel> sequence = new ArrayList();
            // find sequence start
            RelatedAssociationModel assocDef = dms.storage.getTopicRelatedAssociation(typeId, "dm4.core.aggregation",
                "dm4.core.type", "dm4.core.sequence_start", null);      // othersAssocTypeUri=null
            // fetch sequence segments
            if (assocDef != null) {
                sequence.add(assocDef);
                while ((assocDef = dms.storage.getAssociationRelatedAssociation(assocDef.getId(), "dm4.core.sequence",
                    "dm4.core.predecessor", "dm4.core.successor")) != null) {
                    //
                    sequence.add(assocDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for " + className + " \"" + typeUri + "\" failed", e);
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

    void rebuildSequence(long typeId, String typeUri, String className,
                         Collection<AssociationDefinitionModel> assocDefs) {
        deleteSequence(typeId, typeUri, className);
        storeSequence(typeUri, assocDefs);
    }

    private void deleteSequence(long typeId, String typeUri, String className) {
        List<RelatedAssociationModel> sequence = fetchSequence(typeId, typeUri, className);
        logger.info("### Deleting " + sequence.size() + " sequence segments of " + className + " \"" + typeUri + "\"");
        for (RelatedAssociationModel assoc : sequence) {
            long assocId = assoc.getRelatingAssociationModel().getId();
            dms.deleteAssociation(assocId, null);   // clientState=null
        }
    }



    // === Label Configuration ===

    // --- Fetch ---

    private List<String> fetchLabelConfig(List<AssociationDefinitionModel> assocDefs) {
        List<String> labelConfig = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            RelatedTopicModel includeInLabel = fetchLabelConfigTopic(assocDef.getId());
            if (includeInLabel != null && includeInLabel.getSimpleValue().booleanValue()) {
                labelConfig.add(assocDef.getUri());
            }
        }
        return labelConfig;
    }

    private RelatedTopicModel fetchLabelConfigTopic(long assocDefId) {
        return dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.composition",
            "dm4.core.whole", "dm4.core.part", "dm4.core.include_in_label");
    }

    // --- Store ---

    void storeLabelConfig(List<String> labelConfig, Collection<AssociationDefinitionModel> assocDefs) {
        for (AssociationDefinitionModel assocDef : assocDefs) {
            boolean includeInLabel = labelConfig.contains(assocDef.getUri());
            new AttachedAssociationDefinition(assocDef, dms).setChildTopicValue("dm4.core.include_in_label",
                new SimpleValue(includeInLabel));
        }
    }



    // === View Configurations ===

    // --- Fetch ---

    private ViewConfigurationModel fetchTypeViewConfig(Topic typeTopic) {
        try {
            ResultSet<RelatedTopic> configTopics = typeTopic.getRelatedTopics("dm4.core.aggregation",
                "dm4.core.type", "dm4.core.view_config", null, true, false, 0, null);
            // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
            return new ViewConfigurationModel(DeepaMehtaUtils.toTopicModels(configTopics.getItems()));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    private ViewConfigurationModel fetchAssocDefViewConfig(long assocDefId) {
        try {
            ResultSet<RelatedTopicModel> topics = dms.storage.getAssociationRelatedTopics(assocDefId,
                "dm4.core.aggregation", "dm4.core.assoc_def", "dm4.core.view_config", null, 0);
            // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
            // ### FIXME: the composites must be fetched
            return new ViewConfigurationModel(topics.getItems());
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for association definition " + assocDefId +
                " failed", e);
        }
    }

    // ---

    private RelatedTopicModel fetchTypeViewConfigTopic(long typeId, String configTypeUri) {
        // Note: the composite is not fetched as it is not needed
        return dms.storage.getTopicRelatedTopic(typeId, "dm4.core.aggregation",
            "dm4.core.type", "dm4.core.view_config", configTypeUri);
    }

    private RelatedTopicModel fetchAssocDefViewConfigTopic(long assocDefId, String configTypeUri) {
        // Note: the composite is not fetched as it is not needed
        return dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.view_config", configTypeUri);
    }

    // ---

    private TopicModel fetchViewConfigTopic(RoleModel configurable, String configTypeUri) {
        if (configurable instanceof TopicRoleModel) {
            long typeId = ((TopicRoleModel) configurable).getTopicId();
            return fetchTypeViewConfigTopic(typeId, configTypeUri);
        } else if (configurable instanceof AssociationRoleModel) {
            long assocDefId = ((AssociationRoleModel) configurable).getAssociationId();
            return fetchAssocDefViewConfigTopic(assocDefId, configTypeUri);
        } else {
            throw new RuntimeException("Unexpected configurable: " + configurable);
        }
    }

    // --- Store ---

    private void storeViewConfig(RoleModel configurable, ViewConfigurationModel viewConfig) {
        try {
            for (TopicModel configTopic : viewConfig.getConfigTopics()) {
                storeConfigTopic(configurable, configTopic);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration failed (configurable=" + configurable + ")", e);
        }
    }

    // ---

    private void storeConfigTopic(RoleModel configurable, TopicModel configTopic) {
        // Note: null is passed as clientState. Called only (indirectly) from a migration ### FIXME: is this true?
        // and in a migration we have no clientState anyway.
        Topic topic = dms.createTopic(configTopic, null);   // clientState=null
        dms.createAssociation("dm4.core.aggregation", configurable,
            new TopicRoleModel(topic.getId(), "dm4.core.view_config"));
    }

    // ---

    void storeViewConfigSetting(RoleModel configurable, String configTypeUri, String settingUri, Object value) {
        try {
            TopicModel configTopic = fetchViewConfigTopic(configurable, configTypeUri);
            if (configTopic == null) {
                configTopic = new TopicModel(configTypeUri);
                storeConfigTopic(configurable, configTopic);
            }
            new AttachedTopic(configTopic, dms).setChildTopicValue(settingUri, new SimpleValue(value));
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
