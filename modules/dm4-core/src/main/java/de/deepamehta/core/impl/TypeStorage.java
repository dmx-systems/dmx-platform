package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.util.DeepaMehtaUtils;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Storage-impl agnostic support for fetching/storing type models.
 */
class TypeStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    // role types
    private static final String PARENT_CARDINALITY = "dm4.core.parent_cardinality";
    private static final String CHILD_CARDINALITY  = "dm4.core.child_cardinality";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TypeModelImpl> typeCache = new HashMap();   // type model cache

    private EndlessRecursionDetection endlessRecursionDetection = new EndlessRecursionDetection();

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeStorage(PersistenceLayer pl) {
        this.pl = pl;
        this.mf = pl.mf;
    }

    // --------------------------------------------------------------------------------------------------------- Methods



    // === Type Model Cache ===

    TopicTypeModelImpl getTopicType(String topicTypeUri) {
        TopicTypeModelImpl topicType = (TopicTypeModelImpl) getType(topicTypeUri);
        return topicType != null ? topicType : fetchTopicType(topicTypeUri);
    }

    AssociationTypeModelImpl getAssociationType(String assocTypeUri) {
        AssociationTypeModelImpl assocType = (AssociationTypeModelImpl) getType(assocTypeUri);
        return assocType != null ? assocType : fetchAssociationType(assocTypeUri);
    }

    // ---

    void putInTypeCache(TypeModelImpl type) {
        typeCache.put(type.uri, type);
    }

    void removeFromTypeCache(String typeUri) {
        logger.info("### Removing type \"" + typeUri + "\" from type cache");
        if (typeCache.remove(typeUri) == null) {
            throw new RuntimeException("Type \"" + typeUri + "\" not found in type cache");
        }
    }

    // ---

    // ### FIXME: make private
    TypeModelImpl getType(String typeUri) {
        return typeCache.get(typeUri);
    }



    // === Types ===

    // --- Fetch ---

    private TopicTypeModelImpl fetchTopicType(String topicTypeUri) {
        try {
            logger.info("Fetching topic type \"" + topicTypeUri + "\"");
            endlessRecursionDetection.check(topicTypeUri);
            //
            // fetch generic topic
            TopicModelImpl typeTopic = pl.fetchTopic("uri", new SimpleValue(topicTypeUri));
            checkTopicType(topicTypeUri, typeTopic);
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), topicTypeUri, "topic type").getUri();
            List<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
            List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
            ViewConfigurationModel viewConfig = fetchTypeViewConfig(typeTopic);
            //
            // create and cache type model
            TopicTypeModelImpl topicType = mf.newTopicTypeModel(typeTopic, dataTypeUri, indexModes,
                assocDefs, viewConfig);
            putInTypeCache(topicType);
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic type \"" + topicTypeUri + "\" failed", e);
        } finally {
            endlessRecursionDetection.reset(topicTypeUri);
        }
    }

    private AssociationTypeModelImpl fetchAssociationType(String assocTypeUri) {
        try {
            logger.info("Fetching association type \"" + assocTypeUri + "\"");
            endlessRecursionDetection.check(assocTypeUri);
            //
            // fetch generic topic
            TopicModelImpl typeTopic = pl.fetchTopic("uri", new SimpleValue(assocTypeUri));
            checkAssociationType(assocTypeUri, typeTopic);
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), assocTypeUri, "association type").getUri();
            List<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
            List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
            ViewConfigurationModel viewConfig = fetchTypeViewConfig(typeTopic);
            //
            // create and cache type model
            AssociationTypeModelImpl assocType = mf.newAssociationTypeModel(typeTopic, dataTypeUri, indexModes,
                assocDefs, viewConfig);
            putInTypeCache(assocType);
            return assocType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association type \"" + assocTypeUri + "\" failed", e);
        } finally {
            endlessRecursionDetection.reset(assocTypeUri);
        }
    }

    // ---

    private void checkTopicType(String topicTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.topic_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.topic_type\"");
        }
    }

    private void checkAssociationType(String assocTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.assoc_type")) {
            throw new RuntimeException("URI \"" + assocTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.assoc_type\"");
        }
    }

    // --- Store ---

    /**
     * Stores the type-specific parts of the given type model.
     * Prerequisite: the generic topic parts are stored already.
     * <p>
     * Called to store a newly created topic type or association type.
     */
    void storeType(TypeModelImpl type) {
        // 1) put in type model cache
        // Note: an association type must be put in type model cache *before* storing its association definitions.
        // Consider creation of association type "Composition Definition": it has a composition definition itself.
        putInTypeCache(type);
        //
        // 2) store type-specific parts
        storeDataType(type.getUri(), type.getDataTypeUri());
        storeIndexModes(type.getUri(), type.getIndexModes());
        storeAssocDefs(type.getId(), type.getAssocDefs());
        storeViewConfig(newTypeRole(type.getId()), type.getViewConfig());
    }



    // === Data Type ===

    // --- Fetch ---

    private RelatedTopicModel fetchDataTypeTopic(long typeId, String typeUri, String className) {
        try {
            RelatedTopicModel dataType = pl.fetchTopicRelatedTopic(typeId, "dm4.core.aggregation", "dm4.core.type",
                "dm4.core.default", "dm4.core.data_type");
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

    // ### TODO: compare to low-level method CoreServiceImpl._associateDataType(). Remove structural similarity.
    void storeDataType(String typeUri, String dataTypeUri) {
        try {
            pl.createAssociation("dm4.core.aggregation",
                mf.newTopicRoleModel(typeUri,     "dm4.core.type"),
                mf.newTopicRoleModel(dataTypeUri, "dm4.core.default")
            );
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + typeUri + "\" with data type \"" +
                dataTypeUri + "\" failed", e);
        }
    }



    // === Index Modes ===

    // --- Fetch ---

    private List<IndexMode> fetchIndexModes(long typeId) {
        List<RelatedTopicModelImpl> indexModes = pl.fetchTopicRelatedTopics(typeId, "dm4.core.aggregation",
            "dm4.core.type", "dm4.core.default", "dm4.core.index_mode");
        return IndexMode.fromTopics(indexModes);
    }

    // --- Store ---

    private void storeIndexModes(String typeUri, List<IndexMode> indexModes) {
        for (IndexMode indexMode : indexModes) {
            storeIndexMode(typeUri, indexMode);
        }
    }

    void storeIndexMode(String typeUri, IndexMode indexMode) {
        pl.createAssociation("dm4.core.aggregation",
            mf.newTopicRoleModel(typeUri,           "dm4.core.type"),
            mf.newTopicRoleModel(indexMode.toUri(), "dm4.core.default")
        );
    }



    // === Association Definitions ===

    // Note: if the underlying association was an association definition before it has cardinality
    // assignments already. These assignments are restored. Otherwise "One" is used as default.
    private String defaultCardinalityUri(AssociationModel assoc, String cardinalityRoleTypeUri) {
        RelatedTopicModel cardinality = fetchCardinality(assoc.getId(), cardinalityRoleTypeUri);
        if (cardinality != null) {
            return cardinality.getUri();
        } else {
            return "dm4.core.one";
        }
    }

    // --- Fetch ---

    private List<AssociationDefinitionModel> fetchAssociationDefinitions(TopicModelImpl typeTopic) {
        Map<Long, AssociationDefinitionModel> assocDefs = fetchAssociationDefinitionsUnsorted(typeTopic);
        List<RelatedAssociationModelImpl> sequence = fetchSequence(typeTopic);
        // error check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("DB inconsistency: type \"" + typeTopic.getUri() + "\" has " +
                assocDefs.size() + " association definitions but in sequence are " + sequence.size());
        }
        //
        return sortAssocDefs(assocDefs, DeepaMehtaUtils.idList(sequence));
    }

    private Map<Long, AssociationDefinitionModel> fetchAssociationDefinitionsUnsorted(TopicModelImpl typeTopic) {
        Map<Long, AssociationDefinitionModel> assocDefs = new HashMap();
        //
        // 1) fetch child topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered"). ### FIXDOC: this is obsolete
        // Note: the "othersTopicTypeUri" filter is not set here (null). We want match both "dm4.core.topic_type"
        // and "dm4.core.meta_type" (the latter is required e.g. by dm4-mail). ### TODO: add a getRelatedTopics()
        // method that takes a list of topic types.
        List<RelatedTopicModelImpl> childTypes = typeTopic.getRelatedTopics(asList("dm4.core.aggregation_def",
            "dm4.core.composition_def"), "dm4.core.parent_type", "dm4.core.child_type", null);
            // othersTopicTypeUri=null
        //
        // 2) create association definitions
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the assoc def's according to the sequence IDs.
        for (RelatedTopicModel childType : childTypes) {
            AssociationDefinitionModel assocDef = fetchAssociationDefinition(childType.getRelatingAssociation(),
                typeTopic.getUri(), childType.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // ---

    /**
     * Creates an assoc def from an association which may or may not have been an assoc def before.
     * This is needed when an association becomes an assoc def through retyping.
     * <p>
     * Note: the assoc is **not** required to identify its players by URI (by ID is OK)
     */
    AssociationDefinitionModelImpl newAssociationDefinition(AssociationModelImpl assoc) {
        // Note: we must not manipulate the assoc model in-place. The Webclient expects by-ID roles.
        AssociationModel model = mf.newAssociationModel(assoc);
        String parentTypeUri = fetchParentTypeTopic(assoc).getUri();
        String childTypeUri = fetchChildTypeTopic(assoc).getUri();
        prepareAssocModel(model, parentTypeUri, childTypeUri);
        return mf.newAssociationDefinitionModel(model,
            defaultCardinalityUri(assoc, PARENT_CARDINALITY),
            defaultCardinalityUri(assoc, CHILD_CARDINALITY),
            null   // viewConfigModel=null
        );
    }

    // Note: the assoc is **not** required to identify its players by URI (by ID is OK)
    private AssociationDefinitionModel fetchAssociationDefinition(AssociationModel assoc, String parentTypeUri,
                                                                                          String childTypeUri) {
        try {
            prepareAssocModel(assoc, parentTypeUri, childTypeUri);
            return mf.newAssociationDefinitionModel(assoc,
                fetchCardinalityOrThrow(assoc.getId(), PARENT_CARDINALITY).getUri(),
                fetchCardinalityOrThrow(assoc.getId(), CHILD_CARDINALITY).getUri(),
                fetchAssocDefViewConfig(assoc)
            );
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (parentTypeUri=\"" + parentTypeUri +
                "\", childTypeUri=" + childTypeUri + ", " + assoc + ")", e);
        }
    }

    /**
     * Prepares an assoc model for being used as the base for an assoc def model.
     */
    private void prepareAssocModel(AssociationModel assoc, String parentTypeUri, String childTypeUri) {
        long assocDefId = assoc.getId();
        assoc.setRoleModel1(mf.newTopicRoleModel(parentTypeUri, "dm4.core.parent_type"));
        assoc.setRoleModel2(mf.newTopicRoleModel(childTypeUri,  "dm4.core.child_type"));
        ChildTopicsModel childTopics = assoc.getChildTopicsModel();
        RelatedTopicModel customAssocType = fetchCustomAssocType(assocDefId);
        if (customAssocType != null) {
            childTopics.put("dm4.core.assoc_type#dm4.core.custom_assoc_type", customAssocType);
        }
        RelatedTopicModel includeInLabel = fetchIncludeInLabel(assocDefId);
        if (includeInLabel != null) {   // ### TODO: a includeInLabel topic should always exist
            childTopics.put("dm4.core.include_in_label", includeInLabel);
        }
    }

    private RelatedTopicModel fetchCustomAssocType(long assocDefId) {
        // ### TODO: can we use model-driven retrieval?
        return pl.fetchAssociationRelatedTopic(assocDefId, "dm4.core.custom_assoc_type", "dm4.core.parent",
            "dm4.core.child", "dm4.core.assoc_type");
    }

    private RelatedTopicModel fetchIncludeInLabel(long assocDefId) {
        // ### TODO: can we use model-driven retrieval?
        return pl.fetchAssociationRelatedTopic(assocDefId, "dm4.core.composition", "dm4.core.parent",
            "dm4.core.child", "dm4.core.include_in_label");
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

    private void storeAssocDefs(long typeId, Collection<AssociationDefinitionModelImpl> assocDefs) {
        for (AssociationDefinitionModelImpl assocDef : assocDefs) {
            storeAssociationDefinition(assocDef);
        }
        storeSequence(typeId, assocDefs);
    }

    void storeAssociationDefinition(AssociationDefinitionModelImpl assocDef) {
        try {
            long assocDefId = assocDef.getId();
            //
            // 1) create association
            // Note: if the association definition has been created interactively the underlying association
            // exists already. We must not create it again. We detect this case by inspecting the ID.
            if (assocDefId == -1) {
                assocDefId = pl.createAssociation(assocDef).getId();
            }
            //
            // 2) cardinality
            // Note: if the underlying association was an association definition before it has cardinality
            // assignments already. These must be removed before assigning new cardinality.
            removeCardinalityAssignmentIfExists(assocDefId, PARENT_CARDINALITY);
            removeCardinalityAssignmentIfExists(assocDefId, CHILD_CARDINALITY);
            associateCardinality(assocDefId, PARENT_CARDINALITY, assocDef.getParentCardinalityUri());
            associateCardinality(assocDefId, CHILD_CARDINALITY,  assocDef.getChildCardinalityUri());
            //
            // 3) view config
            storeViewConfig(newAssocDefRole(assocDefId), assocDef.getViewConfigModel());
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + assocDef.getAssocDefUri() +
                "\" of type \"" + assocDef.getParentTypeUri() + "\" failed", e);
        }
    }



    // === Parent Type / Child Type ===

    // --- Fetch ---

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the parent type topic.
     *          A topic representing either a topic type or an association type.
     */
    private TopicModel fetchParentTypeTopic(AssociationModelImpl assoc) {
        TopicModel parentType = (TopicModel) assoc.getPlayer("dm4.core.parent_type");
        // error check
        if (parentType == null) {
            throw new RuntimeException("DB inconsistency: topic role \"dm4.core.parent_type\" is missing in " + assoc);
        }
        //
        return parentType;
    }

    /**
     * @param   assoc   an association representing an association definition
     *
     * @return  the child type topic.
     *          A topic representing a topic type.
     */
    private TopicModel fetchChildTypeTopic(AssociationModelImpl assoc) {
        TopicModel childType = (TopicModel) assoc.getPlayer("dm4.core.child_type");
        // error check
        if (childType == null) {
            throw new RuntimeException("DB inconsistency: topic role \"dm4.core.child_type\" is missing in " + assoc);
        }
        //
        return childType;
    }

    // ---

    TypeModelImpl fetchParentType(AssociationModelImpl assoc) {
        TopicModel type = fetchParentTypeTopic(assoc);
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dm4.core.topic_type")) {
            return getTopicType(type.getUri());
        } else if (typeUri.equals("dm4.core.assoc_type")) {
            return getAssociationType(type.getUri());
        } else {
            throw new RuntimeException("DB inconsistency: the \"dm4.core.parent_type\" player is not a type " +
                "but of type \"" + typeUri + "\" in " + assoc);
        }
    }



    // === Cardinality ===

    // --- Fetch ---

    private RelatedTopicModelImpl fetchCardinality(long assocDefId, String cardinalityRoleTypeUri) {
        return pl.fetchAssociationRelatedTopic(assocDefId, "dm4.core.aggregation", "dm4.core.assoc_def",
            cardinalityRoleTypeUri, "dm4.core.cardinality");
    }

    private RelatedTopicModelImpl fetchCardinalityOrThrow(long assocDefId, String cardinalityRoleTypeUri) {
        RelatedTopicModelImpl cardinality = fetchCardinality(assocDefId, cardinalityRoleTypeUri);
        // error check
        if (cardinality == null) {
            throw new RuntimeException("DB inconsistency: association definition " + assocDefId +
                " is missing a cardinality (\"" + cardinalityRoleTypeUri + "\")");
        }
        //
        return cardinality;
    }

    // --- Store ---

    void storeParentCardinalityUri(long assocDefId, String parentCardinalityUri) {
        storeCardinalityUri(assocDefId, PARENT_CARDINALITY, parentCardinalityUri);
    }

    void storeChildCardinalityUri(long assocDefId, String childCardinalityUri) {
        storeCardinalityUri(assocDefId, CHILD_CARDINALITY, childCardinalityUri);
    }

    // ---

    private void storeCardinalityUri(long assocDefId, String cardinalityRoleTypeUri, String cardinalityUri) {
        // remove current assignment
        RelatedTopicModelImpl cardinality = fetchCardinalityOrThrow(assocDefId, cardinalityRoleTypeUri);
        removeCardinalityAssignment(cardinality);
        // create new assignment
        associateCardinality(assocDefId, cardinalityRoleTypeUri, cardinalityUri);
    }

    private void removeCardinalityAssignmentIfExists(long assocDefId, String cardinalityRoleTypeUri) {
        RelatedTopicModelImpl cardinality = fetchCardinality(assocDefId, cardinalityRoleTypeUri);
        if (cardinality != null) {
            removeCardinalityAssignment(cardinality);
        }
    }

    private void removeCardinalityAssignment(RelatedTopicModelImpl cardinalityAssignment) {
        cardinalityAssignment.getRelatingAssociation().delete();
    }

    private void associateCardinality(long assocDefId, String cardinalityRoleTypeUri, String cardinalityUri) {
        pl.createAssociation("dm4.core.aggregation",
            mf.newTopicRoleModel(cardinalityUri, cardinalityRoleTypeUri),
            mf.newAssociationRoleModel(assocDefId, "dm4.core.assoc_def")
        );
    }



    // === Sequence ===

    // --- Fetch ---

    // Note: the sequence is fetched in 2 situations:
    // 1) When fetching a type's association definitions.
    //    In this situation we don't have a DeepaMehtaType object at hand but a sole type topic.
    // 2) When deleting a sequence in order to rebuild it.
    private List<RelatedAssociationModelImpl> fetchSequence(TopicModel typeTopic) {
        try {
            List<RelatedAssociationModelImpl> sequence = new ArrayList();
            //
            RelatedAssociationModelImpl assocDef = fetchSequenceStart(typeTopic.getId());
            if (assocDef != null) {
                sequence.add(assocDef);
                while ((assocDef = fetchSuccessor(assocDef.getId())) != null) {
                    sequence.add(assocDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    // ---

    private RelatedAssociationModelImpl fetchSequenceStart(long typeId) {
        return pl.fetchTopicRelatedAssociation(typeId, "dm4.core.aggregation", "dm4.core.type",
            "dm4.core.sequence_start", null);   // othersAssocTypeUri=null
    }

    private RelatedAssociationModelImpl fetchSuccessor(long assocDefId) {
        return pl.fetchAssociationRelatedAssociation(assocDefId, "dm4.core.sequence", "dm4.core.predecessor",
            "dm4.core.successor", null);        // othersAssocTypeUri=null
    }

    private RelatedAssociationModelImpl fetchPredecessor(long assocDefId) {
        return pl.fetchAssociationRelatedAssociation(assocDefId, "dm4.core.sequence", "dm4.core.successor",
            "dm4.core.predecessor", null);      // othersAssocTypeUri=null
    }

    // --- Store ---

    private void storeSequence(long typeId, Collection<AssociationDefinitionModelImpl> assocDefs) {
        logger.fine("### Storing " + assocDefs.size() + " sequence segments for type " + typeId);
        long predAssocDefId = -1;
        for (AssociationDefinitionModel assocDef : assocDefs) {
            addAssocDefToSequence(typeId, assocDef.getId(), -1, -1, predAssocDefId);
            predAssocDefId = assocDef.getId();
        }
    }

    /**
     * Adds an assoc def to the sequence. Depending on the last 3 arguments either appends it at end, inserts it at
     * start, or inserts it in the middle.
     *
     * @param   beforeAssocDefId    the ID of the assoc def <i>before</i> the assoc def is added
     *                              If <code>-1</code> the assoc def is <b>appended at end</b>.
     *                              In this case <code>lastAssocDefId</code> must identify the end.
     *                              (<code>firstAssocDefId</code> is not relevant in this case.)
     * @param   firstAssocDefId     Identifies the first assoc def. If this equals the ID of the assoc def to add
     *                              the assoc def is <b>inserted at start</b>.
     */
    void addAssocDefToSequence(long typeId, long assocDefId, long beforeAssocDefId, long firstAssocDefId,
                                                                                    long lastAssocDefId) {
        if (beforeAssocDefId == -1) {
            // append at end
            appendToSequence(typeId, assocDefId, lastAssocDefId);
        } else if (firstAssocDefId == assocDefId) {
            // insert at start
            insertAtSequenceStart(typeId, assocDefId);
        } else {
            // insert in the middle
            insertIntoSequence(assocDefId, beforeAssocDefId);
        }
    }

    private void appendToSequence(long typeId, long assocDefId, long predAssocDefId) {
        if (predAssocDefId == -1) {
            storeSequenceStart(typeId, assocDefId);
        } else {
            storeSequenceSegment(predAssocDefId, assocDefId);
        }
    }

    private void insertAtSequenceStart(long typeId, long assocDefId) {
        // delete sequence start
        RelatedAssociationModelImpl assocDef = fetchSequenceStart(typeId);
        assocDef.getRelatingAssociation().delete();
        // reconnect
        storeSequenceStart(typeId, assocDefId);
        storeSequenceSegment(assocDefId, assocDef.getId());
    }

    private void insertIntoSequence(long assocDefId, long beforeAssocDefId) {
        // delete sequence segment
        RelatedAssociationModelImpl assocDef = fetchPredecessor(beforeAssocDefId);
        assocDef.getRelatingAssociation().delete();
        // reconnect
        storeSequenceSegment(assocDef.getId(), assocDefId);
        storeSequenceSegment(assocDefId, beforeAssocDefId);
    }

    // ---

    private void storeSequenceStart(long typeId, long assocDefId) {
        pl.createAssociation("dm4.core.aggregation",
            mf.newTopicRoleModel(typeId, "dm4.core.type"),
            mf.newAssociationRoleModel(assocDefId, "dm4.core.sequence_start")
        );
    }

    private void storeSequenceSegment(long predAssocDefId, long succAssocDefId) {
        pl.createAssociation("dm4.core.sequence",
            mf.newAssociationRoleModel(predAssocDefId, "dm4.core.predecessor"),
            mf.newAssociationRoleModel(succAssocDefId, "dm4.core.successor")
        );
    }

    // ---

    void rebuildSequence(TypeModelImpl type) {
        deleteSequence(type);
        storeSequence(type.getId(), type.getAssocDefs());
    }

    private void deleteSequence(TopicModel typeTopic) {
        List<RelatedAssociationModelImpl> sequence = fetchSequence(typeTopic);
        logger.info("### Deleting " + sequence.size() + " sequence segments of type \"" + typeTopic.getUri() + "\"");
        for (RelatedAssociationModelImpl assoc : sequence) {
            assoc.getRelatingAssociation().delete();
        }
    }



    // === View Configurations ===

    // --- Fetch ---

    private ViewConfigurationModel fetchTypeViewConfig(TopicModel typeTopic) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            return viewConfigModel(pl.fetchTopicRelatedTopics(typeTopic.getId(), "dm4.core.aggregation",
                "dm4.core.type", "dm4.core.view_config", null));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private ViewConfigurationModel fetchAssocDefViewConfig(AssociationModel assocDef) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            return viewConfigModel(pl.fetchAssociationRelatedTopics(assocDef.getId(), "dm4.core.aggregation",
                "dm4.core.assoc_def", "dm4.core.view_config", null));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for assoc def " + assocDef.getId() + " failed", e);
        }
    }

    // ---

    private ViewConfigurationModel viewConfigModel(Iterable<? extends TopicModel> configTopics) {
        fetchChildTopics(configTopics);
        return mf.newViewConfigurationModel(configTopics);
    }

    // --- Store ---

    private void storeViewConfig(RoleModel configurable, ViewConfigurationModelImpl viewConfig) {
        try {
            for (TopicModelImpl configTopic : viewConfig.getConfigTopics()) {
                storeViewConfigTopic(configurable, configTopic);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration failed (configurable=" + configurable +
                ", viewConfig=" + viewConfig + ")", e);
        }
    }

    void storeViewConfigTopic(RoleModel configurable, TopicModelImpl configTopic) {
        pl.createTopic(configTopic);
        pl.createAssociation("dm4.core.aggregation", configurable,
            mf.newTopicRoleModel(configTopic.getId(), "dm4.core.view_config"));
    }

    // --- Helper ---

    private void fetchChildTopics(Iterable<? extends DeepaMehtaObjectModel> objects) {
        for (DeepaMehtaObjectModel object : objects) {
            pl.valueStorage.fetchChildTopics(object);
        }
    }

    // ---

    RoleModel newTypeRole(long typeId) {
        return mf.newTopicRoleModel(typeId, "dm4.core.type");
    }

    RoleModel newAssocDefRole(long assocDefId) {
        return mf.newAssociationRoleModel(assocDefId, "dm4.core.assoc_def");
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private static final class EndlessRecursionDetection {

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
