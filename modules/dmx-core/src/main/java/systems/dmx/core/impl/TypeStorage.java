package systems.dmx.core.impl;

import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.IndexMode;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.util.DMXUtils;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Storage vendor agnostic support for fetching/storing type models.
 */
class TypeStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TypeModelImpl> typeCache = new HashMap();

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
            long typeId = typeTopic.getId();
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeId, topicTypeUri, "topic type").getUri();
            List<IndexMode> indexModes = fetchIndexModes(typeId);
            List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
            //
            // create and cache type model
            TopicTypeModelImpl topicType = mf.newTopicTypeModel(typeTopic, dataTypeUri, indexModes,
                assocDefs, null);   // viewConfig=null
            putInTypeCache(topicType);
            //
            // Note: the topic type "View Config" can have view configs itself. In order to avoid endless recursions
            // the topic type "View Config" must be available in type cache *before* the view configs are fetched.
            topicType.setViewConfig(fetchTypeViewConfig(typeTopic));
            fetchAssocDefsViewConfig(assocDefs);
            //
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
            long typeId = typeTopic.getId();
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeId, assocTypeUri, "association type").getUri();
            List<IndexMode> indexModes = fetchIndexModes(typeId);
            List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic);
            //
            // create and cache type model
            AssociationTypeModelImpl assocType = mf.newAssociationTypeModel(typeTopic, dataTypeUri, indexModes,
                assocDefs, null);   // viewConfig=null
            putInTypeCache(assocType);
            //
            // Note: the topic type "View Config" can have view configs itself. In order to avoid endless recursions
            // the topic type "View Config" must be available in type cache *before* the view configs are fetched.
            assocType.setViewConfig(fetchTypeViewConfig(typeTopic));
            fetchAssocDefsViewConfig(assocDefs);
            //
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
        } else if (!typeTopic.getTypeUri().equals("dmx.core.topic_type") &&
                   !typeTopic.getTypeUri().equals("dmx.core.meta_type") &&
                   !typeTopic.getTypeUri().equals("dmx.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dmx.core.topic_type\"");
        }
    }

    private void checkAssociationType(String assocTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals("dmx.core.assoc_type")) {
            throw new RuntimeException("URI \"" + assocTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dmx.core.assoc_type\"");
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
            RelatedTopicModel dataType = pl.fetchTopicRelatedTopic(typeId, "dmx.core.composition", "dmx.core.type",
                "dmx.core.default", "dmx.core.data_type");
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
            pl.createAssociation("dmx.core.composition",
                mf.newTopicRoleModel(typeUri,     "dmx.core.type"),
                mf.newTopicRoleModel(dataTypeUri, "dmx.core.default")
            );
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + typeUri + "\" with data type \"" + dataTypeUri +
                "\" failed", e);
        }
    }



    // === Index Modes ===

    // --- Fetch ---

    private List<IndexMode> fetchIndexModes(long typeId) {
        List<RelatedTopicModelImpl> indexModes = pl.fetchTopicRelatedTopics(typeId, "dmx.core.composition",
            "dmx.core.type", "dmx.core.default", "dmx.core.index_mode");
        return IndexMode.fromTopics(indexModes);
    }

    // --- Store ---

    private void storeIndexModes(String typeUri, List<IndexMode> indexModes) {
        for (IndexMode indexMode : indexModes) {
            storeIndexMode(typeUri, indexMode);
        }
    }

    void storeIndexMode(String typeUri, IndexMode indexMode) {
        pl.createAssociation("dmx.core.composition",
            mf.newTopicRoleModel(typeUri,           "dmx.core.type"),
            mf.newTopicRoleModel(indexMode.toUri(), "dmx.core.default")
        );
    }



    // === Association Definitions ===

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
        return sortAssocDefs(assocDefs, DMXUtils.idList(sequence));
    }

    private Map<Long, AssociationDefinitionModel> fetchAssociationDefinitionsUnsorted(TopicModelImpl typeTopic) {
        Map<Long, AssociationDefinitionModel> assocDefs = new HashMap();
        //
        // 1) fetch child topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered"). ### FIXDOC: this is obsolete
        // Note: the "othersTopicTypeUri" filter is not set here (null). We want match both "dmx.core.topic_type"
        // and "dmx.core.meta_type" (the latter is required e.g. by dmx-mail). ### TODO: add a getRelatedTopics()
        // method that takes a list of topic types.
        List<RelatedTopicModelImpl> childTypes = typeTopic.getRelatedTopics("dmx.core.composition_def",
            "dmx.core.parent_type", "dmx.core.child_type", null);   // othersTopicTypeUri=null
        //
        // 2) create association definitions
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the assoc def's according to the sequence IDs.
        for (RelatedTopicModelImpl childType : childTypes) {
            AssociationDefinitionModel assocDef = fetchAssociationDefinition(childType.getRelatingAssociation(),
                typeTopic.getUri(), childType.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // ---

    /**
     * Part of "Type Editor Support". See TypeModelImpl.
     * Called when the user creates an assoc def interactively.
     * <p>
     * Creates an assoc def model from an association which may or may not have been an assoc def before.
     * This is needed when an association becomes an assoc def through retyping.
     * <p>
     * Note: the assoc is **not** required to identify its players by URI (by ID is OK)
     */
    AssociationDefinitionModelImpl newAssociationDefinition(AssociationModelImpl assoc) {
        // prepareAssocModel(model, parentTypeUri, childTypeUri, defaultCardinality(assoc)); ### drop it
        return mf.newAssociationDefinitionModel(
            setUriRoles(
                // Note: we must not manipulate the assoc model in-place. The Webclient expects by-ID roles.
                mf.newAssociationModel(assoc),
                fetchParentTypeTopic(assoc).getUri(),
                fetchChildTypeTopic(assoc).getUri()
            ), null     // viewConfig=null
        );
    }

    /**
     * Called when an assoc def is loaded from DB.
     * <p>
     * Note: the assoc is **not** required to identify its players by URI (by ID is OK)
     */
    private AssociationDefinitionModel fetchAssociationDefinition(AssociationModelImpl assoc, String parentTypeUri,
                                                                                              String childTypeUri) {
        try {
            prepareAssocModel(assoc, parentTypeUri, childTypeUri, fetchCardinality(assoc));
            return mf.newAssociationDefinitionModel(assoc, null);   // viewConfig=null
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc def failed (parentTypeUri=\"" + parentTypeUri +
                "\", childTypeUri=\"" + childTypeUri + "\", " + assoc + ")", e);
        }
    }

    /**
     * Prepares an assoc model for being used as the base for an assoc def model.
     * <p>
     * The assoc model is manipulated in-place:
     *   - 2 roles (parent/child types, by-URI)
     *   - cardinality
     *   - custom assoc type
     *   - identity-attr flag
     *   - include-in-label flag
     * <p>
     * TODO: can we use model-driven assoc def retrieval? => NO! See below.
     *
     * @param   cardinality     must not null
     */
    private void prepareAssocModel(AssociationModelImpl assoc, String parentTypeUri, String childTypeUri,
                                   RelatedTopicModel cardinality) {
        ChildTopicsModel childTopics = assoc.getChildTopicsModel();
        // 2 roles
        setUriRoles(assoc, parentTypeUri, childTypeUri);
        // cardinality
        childTopics.put("dmx.core.cardinality", cardinality);   // Note: putRef() would not be sufficient.
                                                                // The assoc model must be fully initialized.
                                                                // Otherwise update would fail. ### TODO: revise comment
        // custom assoc type
        RelatedTopicModel customAssocType = fetchCustomAssocType(assoc);
        if (customAssocType != null) {
            childTopics.put("dmx.core.assoc_type#dmx.core.custom_assoc_type", customAssocType);
        }
        // identity-attr flag
        RelatedTopicModel isIdentityAttr = fetchIsIdentityAttr(assoc);
        if (isIdentityAttr != null) {   // ### TODO: should a isIdentityAttr topic always exist?
            childTopics.put("dmx.core.identity_attr", isIdentityAttr);
        }
        // include-in-label flag
        RelatedTopicModel includeInLabel = fetchIncludeInLabel(assoc);
        if (includeInLabel != null) {   // ### TODO: should a includeInLabel topic always exist?
            childTopics.put("dmx.core.include_in_label", includeInLabel);
        }
    }

    private AssociationModel setUriRoles(AssociationModel assoc, String parentTypeUri, String childTypeUri) {
        assoc.setRoleModel1(mf.newTopicRoleModel(parentTypeUri, "dmx.core.parent_type"));
        assoc.setRoleModel2(mf.newTopicRoleModel(childTypeUri,  "dmx.core.child_type"));
        return assoc;
    }

    private RelatedTopicModel fetchCustomAssocType(AssociationModelImpl assoc) {
        // ### TODO: can we use model-driven retrieval? => NO!
        // Fetching assoc type "Composition Definition" would run into an endless recursion while fetching
        // its "Custom Association Type" assoc def.
        return assoc.getRelatedTopic("dmx.core.custom_assoc_type", "dmx.core.parent", "dmx.core.child",
            "dmx.core.assoc_type");
    }

    private RelatedTopicModel fetchIsIdentityAttr(AssociationModelImpl assoc) {
        // ### TODO: can we use model-driven retrieval? => NO! See above.
        return assoc.getRelatedTopic("dmx.core.composition", "dmx.core.parent", "dmx.core.child",
            "dmx.core.identity_attr");
    }

    private RelatedTopicModel fetchIncludeInLabel(AssociationModelImpl assoc) {
        // ### TODO: can we use model-driven retrieval?  => NO! See above.
        return assoc.getRelatedTopic("dmx.core.composition", "dmx.core.parent", "dmx.core.child",
            "dmx.core.include_in_label");
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
            // assignments already. These must be removed before assigning new cardinality. ### TODO?
            // ### removeCardinalityAssignmentIfExists(assocDefId, CHILD_CARDINALITY);
            // ### associateCardinality(assocDefId, CHILD_CARDINALITY,  assocDef.getChildCardinalityUri());
            //
            // 3) view config
            storeViewConfig(newAssocDefRole(assocDefId), assocDef.getViewConfig());
        } catch (Exception e) {
            throw new RuntimeException("Storing assoc def \"" + assocDef.getAssocDefUri() +
                "\" failed (parent type \"" + assocDef.getParentTypeUri() + "\")", e);
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
        TopicModel parentType = (TopicModel) assoc.getPlayer("dmx.core.parent_type");
        // error check
        if (parentType == null) {
            throw new RuntimeException("DB inconsistency: topic role \"dmx.core.parent_type\" is missing in " + assoc);
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
        TopicModel childType = (TopicModel) assoc.getPlayer("dmx.core.child_type");
        // error check
        if (childType == null) {
            throw new RuntimeException("DB inconsistency: topic role \"dmx.core.child_type\" is missing in " + assoc);
        }
        //
        return childType;
    }

    // ---

    TypeModelImpl fetchParentType(AssociationModelImpl assoc) {
        TopicModel type = fetchParentTypeTopic(assoc);
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dmx.core.topic_type")) {
            return getTopicType(type.getUri());
        } else if (typeUri.equals("dmx.core.assoc_type")) {
            return getAssociationType(type.getUri());
        } else {
            throw new RuntimeException("DB inconsistency: the \"dmx.core.parent_type\" player is not a type " +
                "but of type \"" + typeUri + "\" in " + assoc);
        }
    }



    // === Cardinality ===

    private RelatedTopicModelImpl fetchCardinality(AssociationModelImpl assoc) {
        RelatedTopicModelImpl cardinality = fetchCardinalityIfExists(assoc);
        // error check
        if (cardinality == null) {
            throw new RuntimeException("DB inconsistency: assoc def " + assoc.id + " has no cardinality");
        }
        //
        return cardinality;
    }

    private RelatedTopicModelImpl fetchCardinalityIfExists(AssociationModelImpl assoc) {
        // Note: we can't use model-driven retrieval -> Endless recursion while loading type "dmx.core.composition_def"
        return assoc.getRelatedTopic("dmx.core.composition", "dmx.core.parent", "dmx.core.child",
            "dmx.core.cardinality");
    }

    // Note: if the assoc was an assoc def before it has a cardinality assignment already.
    // The assignment is restored. Otherwise "One" is used as default.
    //
    // ### TODO: drop it (dead code)
    private RelatedTopicModel defaultCardinality(AssociationModelImpl assoc) {
        RelatedTopicModel cardinality = fetchCardinalityIfExists(assoc);
        if (cardinality != null) {
            return cardinality;
        } else {
            return mf.newRelatedTopicModel(pl.fetchTopic("uri", new SimpleValue("dmx.core.one")));  // ### FIXME
        }
    }



    // === Sequence ===

    // --- Fetch ---

    // Note: the sequence is fetched in 2 situations:
    // 1) When fetching a type's association definitions.
    //    In this situation we don't have a DMXType object at hand but a sole type topic.
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
        return pl.fetchTopicRelatedAssociation(typeId, "dmx.core.composition", "dmx.core.type",
            "dmx.core.sequence_start", null);   // othersAssocTypeUri=null ### TODO: set dmx.core.composition_def
    }

    private RelatedAssociationModelImpl fetchSuccessor(long assocDefId) {
        return pl.fetchAssociationRelatedAssociation(assocDefId, "dmx.core.sequence", "dmx.core.predecessor",
            "dmx.core.successor", null);        // othersAssocTypeUri=null ### TODO: set dmx.core.composition_def
    }

    private RelatedAssociationModelImpl fetchPredecessor(long assocDefId) {
        return pl.fetchAssociationRelatedAssociation(assocDefId, "dmx.core.sequence", "dmx.core.successor",
            "dmx.core.predecessor", null);      // othersAssocTypeUri=null ### TODO: set dmx.core.composition_def
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
        pl.createAssociation("dmx.core.composition",
            mf.newTopicRoleModel(typeId, "dmx.core.type"),
            mf.newAssociationRoleModel(assocDefId, "dmx.core.sequence_start")
        );
    }

    private void storeSequenceSegment(long predAssocDefId, long succAssocDefId) {
        pl.createAssociation("dmx.core.sequence",
            mf.newAssociationRoleModel(predAssocDefId, "dmx.core.predecessor"),
            mf.newAssociationRoleModel(succAssocDefId, "dmx.core.successor")
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

    private void fetchAssocDefsViewConfig(List<AssociationDefinitionModel> assocDefs) {
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDef.setViewConfig(fetchAssocDefViewConfig(assocDef));
        }
    }

    // ---

    private ViewConfigurationModel fetchTypeViewConfig(TopicModel typeTopic) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            return viewConfigModel(pl.fetchTopicRelatedTopics(typeTopic.getId(), "dmx.core.composition",
                "dmx.core.type", "dmx.core.view_config", null));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private ViewConfigurationModel fetchAssocDefViewConfig(AssociationModel assocDef) {
        try {
            // Note: othersTopicTypeUri=null, the view config's topic type is unknown (it is client-specific)
            return viewConfigModel(pl.fetchAssociationRelatedTopics(assocDef.getId(), "dmx.core.composition",
                "dmx.core.assoc_def", "dmx.core.view_config", null));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for assoc def " + assocDef.getId() + " failed", e);
        }
    }

    // ---

    /**
     * Creates a view config model from a bunch of config topics.
     * Loads the child topics of the given topics and updates them in-place.
     */
    private ViewConfigurationModel viewConfigModel(Iterable<? extends TopicModelImpl> configTopics) {
        loadChildTopics(configTopics);
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
        pl.createAssociation("dmx.core.composition", configurable,
            mf.newTopicRoleModel(configTopic.getId(), "dmx.core.view_config"));
    }

    // --- Helper ---

    private void loadChildTopics(Iterable<? extends DMXObjectModelImpl> objects) {
        for (DMXObjectModelImpl object : objects) {
            object.loadChildTopics(true);   // deep=true
        }
    }

    // ---

    RoleModel newTypeRole(long typeId) {
        return mf.newTopicRoleModel(typeId, "dmx.core.type");
    }

    RoleModel newAssocDefRole(long assocDefId) {
        return mf.newAssociationRoleModel(assocDefId, "dmx.core.assoc_def");
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
