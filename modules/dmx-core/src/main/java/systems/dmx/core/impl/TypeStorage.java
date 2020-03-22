package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.util.DMXUtils;

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

    private AccessLayer al;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeStorage(AccessLayer al) {
        this.al = al;
        this.mf = al.mf;
    }

    // --------------------------------------------------------------------------------------------------------- Methods



    // === Type Cache ===

    TopicTypeModelImpl getTopicType(String topicTypeUri) {
        TopicTypeModelImpl topicType = (TopicTypeModelImpl) getTypeIfExists(topicTypeUri);
        return topicType != null ? topicType : fetchTopicType(topicTypeUri);
    }

    AssocTypeModelImpl getAssocType(String assocTypeUri) {
        AssocTypeModelImpl assocType = (AssocTypeModelImpl) getTypeIfExists(assocTypeUri);
        return assocType != null ? assocType : fetchAssocType(assocTypeUri);
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

    TypeModelImpl getType(String typeUri) {
        TypeModelImpl type = getTypeIfExists(typeUri);
        if (type == null) {
            throw new RuntimeException("Type \"" + typeUri + "\" not found in type cache");
        }
        return type;
    }

    private TypeModelImpl getTypeIfExists(String typeUri) {
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
            TopicModelImpl typeTopic = al.db.fetchTopic("uri", topicTypeUri);
            checkTopicType(topicTypeUri, typeTopic);
            long typeId = typeTopic.getId();
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeId, topicTypeUri, "topic type").getUri();
            List<CompDefModel> compDefs = fetchCompDefs(typeTopic);
            //
            // create and cache type model
            TopicTypeModelImpl topicType = mf.newTopicTypeModel(typeTopic, dataTypeUri, compDefs, null);
            putInTypeCache(topicType);                                                            // viewConfig=null
            //
            // Note: the topic type "View Config" can have view configs itself. In order to avoid endless recursions
            // the topic type "View Config" must be available in type cache *before* the view configs are fetched.
            topicType.setViewConfig(fetchViewConfigOfType(typeTopic));
            fetchViewConfigOfCompDefs(compDefs);
            //
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic type \"" + topicTypeUri + "\" failed", e);
        } finally {
            endlessRecursionDetection.reset(topicTypeUri);
        }
    }

    private AssocTypeModelImpl fetchAssocType(String assocTypeUri) {
        try {
            logger.info("Fetching association type \"" + assocTypeUri + "\"");
            endlessRecursionDetection.check(assocTypeUri);
            //
            // fetch generic topic
            TopicModelImpl typeTopic = al.db.fetchTopic("uri", assocTypeUri);
            checkAssocType(assocTypeUri, typeTopic);
            long typeId = typeTopic.getId();
            //
            // fetch type-specific parts
            String dataTypeUri = fetchDataTypeTopic(typeId, assocTypeUri, "association type").getUri();
            List<CompDefModel> compDefs = fetchCompDefs(typeTopic);
            //
            // create and cache type model
            AssocTypeModelImpl assocType = mf.newAssocTypeModel(typeTopic, dataTypeUri, compDefs, null);
            putInTypeCache(assocType);                                                            // viewConfig=null
            //
            // Note: the topic type "View Config" can have view configs itself. In order to avoid endless recursions
            // the topic type "View Config" must be available in type cache *before* the view configs are fetched.
            assocType.setViewConfig(fetchViewConfigOfType(typeTopic));
            fetchViewConfigOfCompDefs(compDefs);
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
        } else if (!typeTopic.getTypeUri().equals(TOPIC_TYPE) &&
                   !typeTopic.getTypeUri().equals(META_TYPE) &&
                   !typeTopic.getTypeUri().equals("dmx.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dmx.core.topic_type\"");
        }
    }

    private void checkAssocType(String assocTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Assoc type \"" + assocTypeUri + "\" not found in DB");
        } else if (!typeTopic.getTypeUri().equals(ASSOC_TYPE)) {
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
        // 1) put in type cache
        // Note: an association type must be put in type cache *before* storing its comp defs. Consider creation
        // of association type "Composition Definition": it has a composition definition itself.
        putInTypeCache(type);
        //
        // 2) store type-specific parts
        storeDataType(type.getUri(), type.getDataTypeUri());
        storeCompDefs(type.getId(), type.getCompDefs());
        storeViewConfig(type);
    }



    // === Data Type ===

    // --- Fetch ---

    private RelatedTopicModel fetchDataTypeTopic(long typeId, String typeUri, String className) {
        try {
            RelatedTopicModel dataType = al.sd.fetchTopicRelatedTopic(typeId, COMPOSITION, PARENT, CHILD, DATA_TYPE);
            if (dataType == null) {
                throw new RuntimeException("No data type topic associated to " + className + " \"" + typeUri + "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching data type topic of " + className + " \"" + typeUri + "\" failed", e);
        }
    }

    // --- Store ---

    // ### TODO: compare to low-level method CoreServiceImpl._associateDataType(). Remove structural similarity.
    void storeDataType(String typeUri, String dataTypeUri) {
        try {
            al.createAssoc(COMPOSITION,
                mf.newTopicPlayerModel(typeUri,     PARENT),
                mf.newTopicPlayerModel(dataTypeUri, CHILD)
            );
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + typeUri + "\" with data type \"" + dataTypeUri +
                "\" failed", e);
        }
    }



    // === Composition Definitions ===

    // --- Fetch ---

    private List<CompDefModel> fetchCompDefs(TopicModelImpl typeTopic) {
        Map<Long, CompDefModel> compDefs = fetchCompDefsUnsorted(typeTopic);
        List<RelatedAssocModelImpl> sequence = fetchSequence(typeTopic);
        // error check
        if (compDefs.size() != sequence.size()) {
            throw new RuntimeException("DB inconsistency: type \"" + typeTopic.getUri() + "\" has " + compDefs.size() +
                " comp defs but in sequence are " + sequence.size());
        }
        //
        return sortCompDefs(compDefs, DMXUtils.idList(sequence));
    }

    private Map<Long, CompDefModel> fetchCompDefsUnsorted(TopicModelImpl typeTopic) {
        Map<Long, CompDefModel> compDefs = new HashMap();
        //
        // 1) fetch child topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered"). ### FIXDOC: this is obsolete
        // Note: the "othersTopicTypeUri" filter is not set here (null). We want match both "dmx.core.topic_type"
        // and "dmx.core.meta_type" (the latter is required e.g. by dmx-mail). ### TODO: add a getRelatedTopics()
        // method that takes a list of topic types.
        List<RelatedTopicModelImpl> childTypes = typeTopic.getRelatedTopics(COMPOSITION_DEF,
            PARENT_TYPE, CHILD_TYPE, null);   // othersTopicTypeUri=null
        //
        // 2) create comp defs
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the comp def's according to the sequence IDs.
        for (RelatedTopicModelImpl childType : childTypes) {
            CompDefModel compDef = fetchCompDef(childType.getRelatingAssoc(), typeTopic.getUri(), childType.getUri());
            compDefs.put(compDef.getId(), compDef);
        }
        return compDefs;
    }

    // ---

    /**
     * Creates an comp def model from an assoc model. Determines the parent/child type URIs and adds them in-place.
     * Note: the assoc may or may not have been an comp def before.
     *
     * Part of "Type Editor Support". See TypeModelImpl.
     * Called when the user creates an comp def interactively.
     *
     * @param   assoc   an assoc whose players are ref'd by-ID
     */
    CompDefModelImpl newCompDefModel(AssocModelImpl assoc) {
        return mf.newCompDefModel(
            addPlayerUris(assoc, fetchParentTypeTopic(assoc).uri, fetchChildTypeTopic(assoc).uri),
            mf.newViewConfigurationModel().addConfigTopic(
                // FIXME: the Core must not know about the Webclient
                // FIXME: the view config topic label is not set
                mf.newTopicModel("dmx.webclient.view_config", new SimpleValue("View Configuration"))
            )
        );
    }

    /**
     * Creates an comp def model from an assoc model as retrieved from DB.
     *
     * In-place sets the given type URIs as the player URIs.
     *
     * Fetches the comp def's child topics and manipulates the given assoc model in-place:
     *   - cardinality
     *   - custom assoc type
     *   - identity-attr flag
     *   - include-in-label flag
     *
     * Called when a type is loaded from DB.
     *
     * Note: we can't use model-driven comp def retrieval. Fetching assoc type "Composition Definition" would run into
     * an endless recursion while fetching its "Custom Association Type" comp def.
     *
     * @param   assoc   the underlying assoc as retrieved from DB, that is
     *                  1) players are ref'd by-ID
     *                  2) children are not retrieved
     */
    private CompDefModel fetchCompDef(AssocModelImpl assoc, String parentTypeUri, String childTypeUri) {
        try {
            // 2 players
            addPlayerUris(assoc, parentTypeUri, childTypeUri);
            // cardinality (must exist in DB)
            ChildTopicsModel childTopics = assoc.getChildTopicsModel();
            childTopics.put(CARDINALITY, fetchCardinality(assoc));
                        // Note: putRef() would not be sufficient. The assoc model must be fully initialized.
                        // Otherwise update would fail. ### TODO: revise comment
            // custom assoc type
            RelatedTopicModel customAssocType = fetchCustomAssocType(assoc);
            if (customAssocType != null) {
                childTopics.put("dmx.core.assoc_type#dmx.core.custom_assoc_type", customAssocType);
            }
            // identity-attr flag
            RelatedTopicModel isIdentityAttr = fetchIsIdentityAttr(assoc);
            if (isIdentityAttr != null) {   // ### TODO: should a isIdentityAttr topic always exist?
                childTopics.put(IDENTITY_ATTR, isIdentityAttr);
            }
            // include-in-label flag
            RelatedTopicModel includeInLabel = fetchIncludeInLabel(assoc);
            if (includeInLabel != null) {   // ### TODO: should a includeInLabel topic always exist?
                childTopics.put(INCLUDE_IN_LABEL, includeInLabel);
            }
            return mf.newCompDefModel(assoc, null);   // viewConfig=null
        } catch (Exception e) {
            throw new RuntimeException("Fetching comp def failed (parentTypeUri=\"" + parentTypeUri +
                "\", childTypeUri=\"" + childTypeUri + "\", " + assoc + ")", e);
        }
    }

    private RelatedTopicModel fetchCustomAssocType(AssocModelImpl assoc) {
        // Note: we can't use model-driven retrieval. See comment above.
        return assoc.getRelatedTopic(CUSTOM_ASSOC_TYPE, PARENT, CHILD, ASSOC_TYPE);
    }

    private RelatedTopicModel fetchIsIdentityAttr(AssocModelImpl assoc) {
        // Note: we can't use model-driven retrieval. See comment above.
        return assoc.getRelatedTopic(COMPOSITION, PARENT, CHILD, IDENTITY_ATTR);
    }

    private RelatedTopicModel fetchIncludeInLabel(AssocModelImpl assoc) {
        // Note: we can't use model-driven retrieval. See comment above.
        return assoc.getRelatedTopic(COMPOSITION, PARENT, CHILD, INCLUDE_IN_LABEL);
    }

    // ---

    private AssocModel addPlayerUris(AssocModel assoc, String parentTypeUri, String childTypeUri) {
        ((TopicPlayerModelImpl) assoc.getPlayerByRole(PARENT_TYPE)).topicUri = parentTypeUri;
        ((TopicPlayerModelImpl) assoc.getPlayerByRole(CHILD_TYPE)).topicUri  = childTypeUri;
        return assoc;
    }

    private CompDefModelImpl addPlayerIds(CompDefModelImpl compDef) {
        compDef.getPlayerByRole(PARENT_TYPE).id = compDef.getParentType().id;
        compDef.getPlayerByRole(CHILD_TYPE).id  = compDef.getChildType().id;
        return compDef;
    }

    // ---

    private List<CompDefModel> sortCompDefs(Map<Long, CompDefModel> compDefs, List<Long> sequence) {
        List<CompDefModel> sortedCompDefs = new ArrayList();
        for (long compDefId : sequence) {
            CompDefModel compDef = compDefs.get(compDefId);
            // error check
            if (compDef == null) {
                throw new RuntimeException("DB inconsistency: ID " + compDefId +
                    " is in sequence but not in the type's comp defs");
            }
            sortedCompDefs.add(compDef);
        }
        return sortedCompDefs;
    }

    // --- Store ---

    private void storeCompDefs(long typeId, Collection<CompDefModelImpl> compDefs) {
        for (CompDefModelImpl compDef : compDefs) {
            storeCompDef(compDef);
        }
        storeSequence(typeId, compDefs);
    }

    void storeCompDef(CompDefModelImpl compDef) {
        try {
            // Note: the passed "compDef" is what is stored in type cache. The value integrator (as called by
            // createAssoc()) does not update that model in-place.
            AssocModelImpl assoc = al.createAssoc(addPlayerIds(compDef));
            // We must explicitly update the type cache with the integration result (as returned by createAssoc()).
            // Both values are needed, the simple one (as being displayed in webclient's type editor), and the composite
            // one (as its relating assocs are needed when revealing a comp def child topic in webclient).
            // TODO: remove this side effect
            compDef.value       = assoc.value;
            compDef.childTopics = assoc.childTopics;
            //
            storeViewConfig(compDef);
        } catch (Exception e) {
            throw new RuntimeException("Storing comp def \"" + compDef.getCompDefUri() + "\" failed (parent type \"" +
                compDef.getParentTypeUri() + "\")", e);
        }
    }



    // === Parent Type / Child Type ===

    // --- Fetch ---

    /**
     * @param   assoc   an association representing a comp def
     *
     * @return  the parent type topic.
     *          A topic representing either a topic type or an association type.
     */
    private TopicModelImpl fetchParentTypeTopic(AssocModelImpl assoc) {
        TopicModelImpl parentType = (TopicModelImpl) assoc.getDMXObjectByRole(PARENT_TYPE);
        // error check
        if (parentType == null) {
            throw new RuntimeException("DB inconsistency: player \"dmx.core.parent_type\" is missing in " + assoc);
        }
        //
        return parentType;
    }

    /**
     * @param   assoc   an association representing a comp def
     *
     * @return  the child type topic.
     *          A topic representing a topic type.
     */
    private TopicModelImpl fetchChildTypeTopic(AssocModelImpl assoc) {
        TopicModelImpl childType = (TopicModelImpl) assoc.getDMXObjectByRole(CHILD_TYPE);
        // error check
        if (childType == null) {
            throw new RuntimeException("DB inconsistency: player \"dmx.core.child_type\" is missing in " + assoc);
        }
        //
        return childType;
    }

    // ---

    TypeModelImpl fetchParentType(AssocModelImpl assoc) {
        TopicModelImpl type = fetchParentTypeTopic(assoc);
        if (type.typeUri.equals(TOPIC_TYPE)) {
            return getTopicType(type.uri);
        } else if (type.typeUri.equals(ASSOC_TYPE)) {
            return getAssocType(type.uri);
        } else {
            throw new RuntimeException("DB inconsistency: the \"dmx.core.parent_type\" player is not a type " +
                "but of type \"" + type.typeUri + "\" in " + assoc);
        }
    }



    // === Cardinality ===

    private RelatedTopicModelImpl fetchCardinality(AssocModelImpl assoc) {
        RelatedTopicModelImpl cardinality = fetchCardinalityIfExists(assoc);
        // error check
        if (cardinality == null) {
            throw new RuntimeException("DB inconsistency: comp def " + assoc.id + " has no cardinality");
        }
        //
        return cardinality;
    }

    private RelatedTopicModelImpl fetchCardinalityIfExists(AssocModelImpl assoc) {
        // Note: we can't use model-driven retrieval -> Endless recursion while loading type "dmx.core.composition_def"
        return assoc.getRelatedTopic(COMPOSITION, PARENT, CHILD, CARDINALITY);
    }

    // Note: if the assoc was an comp def before it has a cardinality assignment already.
    // The assignment is restored. Otherwise "One" is used as default.
    //
    // ### TODO: drop it (dead code)
    private RelatedTopicModel defaultCardinality(AssocModelImpl assoc) {
        RelatedTopicModel cardinality = fetchCardinalityIfExists(assoc);
        if (cardinality != null) {
            return cardinality;
        } else {
            return mf.newRelatedTopicModel(al.db.fetchTopic("uri", ONE));    // ### FIXME
        }
    }



    // === Sequence ===

    // --- Fetch ---

    // Note: the sequence is fetched in 2 situations:
    // 1) When fetching a type's comp defs.
    //    In this situation we don't have a DMXType object at hand but a sole type topic.
    // 2) When deleting a sequence in order to rebuild it.
    private List<RelatedAssocModelImpl> fetchSequence(TopicModel typeTopic) {
        try {
            List<RelatedAssocModelImpl> sequence = new ArrayList();
            //
            RelatedAssocModelImpl compDef = fetchSequenceStart(typeTopic.getId());
            if (compDef != null) {
                sequence.add(compDef);
                while ((compDef = fetchSuccessor(compDef.getId())) != null) {
                    sequence.add(compDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    // ---

    private RelatedAssocModelImpl fetchSequenceStart(long typeId) {
        return al.sd.fetchTopicRelatedAssoc(typeId, COMPOSITION, TYPE, SEQUENCE_START, COMPOSITION_DEF);
    }

    private RelatedAssocModelImpl fetchSuccessor(long compDefId) {
        return al.sd.fetchAssocRelatedAssoc(compDefId, SEQUENCE, PREDECESSOR, SUCCESSOR, COMPOSITION_DEF);
    }

    private RelatedAssocModelImpl fetchPredecessor(long compDefId) {
        return al.sd.fetchAssocRelatedAssoc(compDefId, SEQUENCE, SUCCESSOR, PREDECESSOR, COMPOSITION_DEF);
    }

    // --- Store ---

    private void storeSequence(long typeId, Collection<CompDefModelImpl> compDefs) {
        logger.fine("### Storing " + compDefs.size() + " sequence segments for type " + typeId);
        long predCompDefId = -1;
        for (CompDefModel compDef : compDefs) {
            addCompDefToSequence(typeId, compDef.getId(), -1, -1, predCompDefId);
            predCompDefId = compDef.getId();
        }
    }

    /**
     * Adds an comp def to the sequence. Depending on the last 3 arguments either appends it at end, inserts it at
     * start, or inserts it in the middle.
     *
     * @param   beforeCompDefId     the ID of the comp def <i>before</i> the comp def is added
     *                              If <code>-1</code> the comp def is <b>appended at end</b>.
     *                              In this case <code>lastCompDefId</code> must identify the end.
     *                              (<code>firstCompDefId</code> is not relevant in this case.)
     * @param   firstCompDefId      Identifies the first comp def. If this equals the ID of the comp def to add
     *                              the comp def is <b>inserted at start</b>.
     */
    void addCompDefToSequence(long typeId, long compDefId, long beforeCompDefId, long firstCompDefId,
                                                                                 long lastCompDefId) {
        if (beforeCompDefId == -1) {
            // append at end
            appendToSequence(typeId, compDefId, lastCompDefId);
        } else if (firstCompDefId == compDefId) {
            // insert at start
            insertAtSequenceStart(typeId, compDefId);
        } else {
            // insert in the middle
            insertIntoSequence(compDefId, beforeCompDefId);
        }
    }

    private void appendToSequence(long typeId, long compDefId, long predCompDefId) {
        if (predCompDefId == -1) {
            storeSequenceStart(typeId, compDefId);
        } else {
            storeSequenceSegment(predCompDefId, compDefId);
        }
    }

    private void insertAtSequenceStart(long typeId, long compDefId) {
        // delete sequence start
        RelatedAssocModelImpl compDef = fetchSequenceStart(typeId);
        compDef.getRelatingAssoc().delete();
        // reconnect
        storeSequenceStart(typeId, compDefId);
        storeSequenceSegment(compDefId, compDef.getId());
    }

    private void insertIntoSequence(long compDefId, long beforeCompDefId) {
        // delete sequence segment
        RelatedAssocModelImpl compDef = fetchPredecessor(beforeCompDefId);
        compDef.getRelatingAssoc().delete();
        // reconnect
        storeSequenceSegment(compDef.getId(), compDefId);
        storeSequenceSegment(compDefId, beforeCompDefId);
    }

    // ---

    private void storeSequenceStart(long typeId, long compDefId) {
        al.createAssoc(COMPOSITION,
            mf.newTopicPlayerModel(typeId, TYPE),
            mf.newAssocPlayerModel(compDefId, SEQUENCE_START)
        );
    }

    private void storeSequenceSegment(long predCompDefId, long succCompDefId) {
        al.createAssoc(SEQUENCE,
            mf.newAssocPlayerModel(predCompDefId, PREDECESSOR),
            mf.newAssocPlayerModel(succCompDefId, SUCCESSOR)
        );
    }

    // ---

    void rebuildSequence(TypeModelImpl type) {
        deleteSequence(type);
        storeSequence(type.getId(), type.getCompDefs());
    }

    private void deleteSequence(TopicModel typeTopic) {
        List<RelatedAssocModelImpl> sequence = fetchSequence(typeTopic);
        logger.info("### Deleting " + sequence.size() + " sequence segments of type \"" + typeTopic.getUri() + "\"");
        for (RelatedAssocModelImpl assoc : sequence) {
            assoc.getRelatingAssoc().delete();
        }
    }



    // === View Configurations ===

    // --- Fetch ---

    private void fetchViewConfigOfCompDefs(List<CompDefModel> compDefs) {
        for (CompDefModel compDef : compDefs) {
            compDef.setViewConfig(fetchViewConfigOfCompDef(compDef));
        }
    }

    // ---

    private ViewConfigurationModel fetchViewConfigOfType(TopicModel typeTopic) {
        try {
            return viewConfigModel(al.db.fetchTopicRelatedTopics(typeTopic.getId(), COMPOSITION,
                PARENT, CHILD, "dmx.webclient.view_config"));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view config of type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    private ViewConfigurationModel fetchViewConfigOfCompDef(AssocModel compDef) {
        try {
            return viewConfigModel(al.db.fetchAssocRelatedTopics(compDef.getId(), COMPOSITION,
                PARENT, CHILD, "dmx.webclient.view_config"));
        } catch (Exception e) {
            throw new RuntimeException("Fetching view config of comp def " + compDef.getId() + " failed", e);
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

    private void storeViewConfig(TypeModelImpl type) {
        ViewConfigurationModelImpl viewConfig = type.viewConfig;
        TopicModel configTopic = _storeViewConfig(newTypePlayer(type.id), viewConfig);
        // Note: cached view config must be overridden with the "real thing". Otherwise the child assocs
        // would be missing on a cold start. Subsequent migrations operating on them would fail.
        if (configTopic != null) {
            viewConfig.updateConfigTopic(configTopic);
        }
    }

    void storeViewConfig(CompDefModelImpl compDef) {
        ViewConfigurationModelImpl viewConfig = compDef.viewConfig;
        TopicModel configTopic = _storeViewConfig(newCompDefPlayer(compDef.id), viewConfig);
        // Note: cached view config must be overridden with the "real thing". Otherwise the child assocs
        // would be missing on a cold start. Subsequent migrations operating on them would fail.
        if (configTopic != null) {
            viewConfig.updateConfigTopic(configTopic);
        }
    }

    /**
     * @return      may be null
     */
    private TopicModel _storeViewConfig(PlayerModel configurable, ViewConfigurationModelImpl viewConfig) {
        try {
            TopicModel topic = null;
            for (TopicModelImpl configTopic : viewConfig.getConfigTopics()) {
                if (topic != null) {
                    throw new RuntimeException("DM5 does not support more than one view config topic per configurable");
                }
                topic = storeViewConfigTopic(configurable, configTopic);
            }
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Storing view configuration failed (configurable=" + configurable +
                ", viewConfig=" + viewConfig + ")", e);
        }
    }

    TopicModel storeViewConfigTopic(PlayerModel configurable, TopicModelImpl configTopic) {
        TopicModel topic = al.createTopic(configTopic);
        al.createAssoc(COMPOSITION,
            configurable,
            mf.newTopicPlayerModel(configTopic.id, CHILD)
        );
        return topic;
    }

    // --- Helper ---

    private void loadChildTopics(Iterable<? extends DMXObjectModelImpl> objects) {
        for (DMXObjectModelImpl object : objects) {
            object.loadChildTopics(true);   // deep=true
        }
    }

    // ---

    PlayerModel newTypePlayer(long typeId) {
        return mf.newTopicPlayerModel(typeId, PARENT);
    }

    PlayerModel newCompDefPlayer(long compDefId) {
        return mf.newAssocPlayerModel(compDefId, PARENT);
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
