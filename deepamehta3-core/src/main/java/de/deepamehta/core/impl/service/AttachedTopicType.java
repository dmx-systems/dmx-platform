package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TopicValue;

import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Attached object cache.
     */
    private Map<String, AssociationDefinition> assocDefs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(EmbeddedService dms) {
        super(dms);     // The model and the attached object cache remain uninitialized.
                        // They are initialized later on through fetch().
    }

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
        initAssocDefs();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicType Implementation ===

    // --- Data Type ---

    @Override
    public String getDataTypeUri() {
        return getModel().getDataTypeUri();
    }

    @Override
    public void setDataTypeUri(String dataTypeUri) {
        // update memory
        getModel().setDataTypeUri(dataTypeUri);
        // update DB
        storeDataTypeUri();
    }

    // --- Index Modes ---

    @Override
    public Set<IndexMode> getIndexModes() {
        return getModel().getIndexModes();
    }

    @Override
    public void setIndexModes(Set<IndexMode> indexModes) {
        // update memory
        getModel().setIndexModes(indexModes);
        // update DB
        storeIndexModes();
    }

    // --- Association Definitions ---

    @Override
    public Map<String, AssociationDefinition> getAssocDefs() {
        return assocDefs;
    }

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        AssociationDefinition assocDef = assocDefs.get(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                assocDefUri + "\" not found in " + this);
        }
        return assocDef;
    }

    @Override
    public void addAssocDef(AssociationDefinitionModel model) {
        // Note: the predecessor must be determined *before* the memory is updated
        AssociationDefinition predecessor = lastAssocDef();
        // 1) update memory
        getModel().addAssocDef(model);                                          // update model
        AttachedAssociationDefinition assocDef = _addAssocDef(model);           // update attached object cache
        // 2) update DB
        assocDef.store();
        appendToSequence(assocDef, predecessor);
    }

    @Override
    public void updateAssocDef(AssociationDefinitionModel model) {
        // 1) update memory
        getModel().updateAssocDef(model);                                       // update model
        _addAssocDef(model);                                                    // update attached object cache
        // 2) update DB
        // ### Note: nothing to do for the moment
        // (in case of interactive assoc type change the association is already updated in DB)
    }

    @Override
    public void removeAssocDef(String assocDefUri) {
        // 1) update memory
        getModel().removeAssocDef(assocDefUri);                                 // update model
        AttachedAssociationDefinition assocDef = _removeAssocDef(assocDefUri);  // update attached object cache
        // 2) update DB
        rebuildSequence();
    }



    // === AttachedTopic Overrides ===

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(String topicTypeUri) {
        AttachedTopic typeTopic = dms.getTopic("uri", new TopicValue(topicTypeUri), false);     // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        Map<Long, AttachedAssociationDefinition> assocDefs = fetchAssociationDefinitions(typeTopic);
        //
        List<RelatedAssociation> sequence = fetchSequence(typeTopic);
        // sanity check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("Graph inconsistency: " + assocDefs.size() + " association " +
                "definitions found but sequence length is " + sequence.size());
        }
        // build model
        TopicTypeModel model = new TopicTypeModel(typeTopic.getModel(), fetchDataTypeTopic(typeTopic).getUri(),
                                                                        fetchIndexModes(typeTopic),
                                                                        fetchViewConfig(typeTopic));
        addAssocDefsToModel(model, assocDefs, sequence);
        // set model of this topic type
        setModel(model);
        // ### initAssocDefs(); // Note: the assoc defs are already initialized through previous addAssocDefsToModel()
        initViewConfig();       // defined in superclass
    }

    void store() {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setValue(getValue());
        //
        dms.associateDataType(getUri(), getDataTypeUri());
        storeIndexModes();
        storeAssocDefs();
        storeSequence();
        getViewConfig().store();
    }

    void update(TopicTypeModel model) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        String uri = model.getUri();
        TopicValue value = model.getValue();
        String dataTypeUri = model.getDataTypeUri();
        //
        boolean uriChanged = !getUri().equals(uri);
        boolean valueChanged = !getValue().equals(value);
        boolean dataTypeChanged = !getDataTypeUri().equals(dataTypeUri);
        //
        if (uriChanged || valueChanged) {
            if (uriChanged) {
                logger.info("Changing URI from \"" + getUri() + "\" -> \"" + uri + "\"");
            }
            if (valueChanged) {
                logger.info("Changing name from \"" + getValue() + "\" -> \"" + value + "\"");
            }
            if (uriChanged) {
                dms.typeCache.invalidate(getUri());
                super.update(model);
                dms.typeCache.put(this);
            } else {
                super.update(model);
            }
        }
        if (dataTypeChanged) {
            logger.info("Changing data type from \"" + getDataTypeUri() + "\" -> \"" + dataTypeUri + "\"");
            setDataTypeUri(dataTypeUri);
        }
        //
        if (!uriChanged && !valueChanged && !dataTypeChanged) {
            logger.info("Updating topic type \"" + getUri() + "\" ABORTED -- no changes made by user");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private Map<Long, AttachedAssociationDefinition> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AttachedAssociationDefinition> assocDefs = new HashMap();
        //
        // fetch part topic types
        List assocTypeFilter = Arrays.asList("dm3.core.aggregation_def", "dm3.core.composition_def");
        Set<RelatedTopic> partTopicTypes = typeTopic.getRelatedTopics(assocTypeFilter,
            "dm3.core.whole_topic_type", "dm3.core.part_topic_type", "dm3.core.topic_type", false);
        //
        for (RelatedTopic partTopicType : partTopicTypes) {
            AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(dms);
            // FIXME: pass more info of the reltopic to the fetch() method to avoid double-fetching
            assocDef.fetch(partTopicType.getAssociation(), typeTopic.getUri());
            // Note: the returned map is an intermediate, hashed by ID. The actual type model is
            // subsequently build from it by sorting the assoc def's according to the sequence IDs.
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    private void addAssocDefsToModel(TopicTypeModel model, Map<Long, AttachedAssociationDefinition> assocDefs,
                                                           List<RelatedAssociation> sequence) {
        this.assocDefs = new LinkedHashMap();
        for (RelatedAssociation relAssoc : sequence) {
            long assocDefId = relAssoc.getId();
            AttachedAssociationDefinition assocDef = assocDefs.get(assocDefId);
            // sanity check
            if (assocDef == null) {
                throw new RuntimeException("Graph inconsistency: ID " + assocDefId +
                    " is in sequence but association definition is not found");
            }
            // Note: the model and the attached object cache is updated together
            model.addAssocDef(assocDef.getModel());             // update model
            this.assocDefs.put(assocDef.getUri(), assocDef);    // update attached object cache
        }
    }

    // ---

    private RelatedTopic fetchDataTypeTopic(Topic typeTopic) {
        try {
            RelatedTopic dataType = typeTopic.getRelatedTopic("dm3.core.association", "dm3.core.topic_type",
                "dm3.core.data_type", "dm3.core.data_type", false);     // fetchComposite=false
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
        Set<RelatedTopic> topics = typeTopic.getRelatedTopics("dm3.core.association", "dm3.core.topic_type",
            "dm3.core.index_mode", "dm3.core.index_mode", false);       // fetchComposite=false
        return IndexMode.fromTopics(topics);
    }



    // === Store ===

    private void storeDataTypeUri() {
        // remove current assignment
        long assocId = fetchDataTypeTopic(this).getAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientContext=null
        // create new assignment
        dms.associateDataType(getUri(), getDataTypeUri());
    }

    private void storeIndexModes() {
        for (IndexMode indexMode : getIndexModes()) {
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getUri(), "dm3.core.topic_type"));
            assocModel.setRoleModel2(new TopicRoleModel(indexMode.toUri(), "dm3.core.index_mode"));
            dms.createAssociation(assocModel, null);         // FIXME: clientContext=null
        }
    }

    private void storeAssocDefs() {
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            ((AttachedAssociationDefinition) assocDef).store();
        }
    }



    // === Helper ===

    /**
     * Returns the last association definition of this type or
     * <code>null</code> if there are no association definitions.
     */
    private AssociationDefinition lastAssocDef() {
        AssociationDefinition lastAssocDef = null;
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            lastAssocDef = assocDef;
        }
        return lastAssocDef;
    }

    // --- Sequence ---

    private List<RelatedAssociation> fetchSequence(Topic typeTopic) {
        try {
            List<RelatedAssociation> sequence = new ArrayList();
            // find sequence start
            RelatedAssociation assocDef = typeTopic.getRelatedAssociation("dm3.core.association",
                "dm3.core.topic_type", "dm3.core.first_assoc_def");
            // fetch sequence segments
            if (assocDef != null) {
                sequence.add(assocDef);
                while ((assocDef = assocDef.getRelatedAssociation("dm3.core.sequence",
                    "dm3.core.predecessor", "dm3.core.successor")) != null) {
                    //
                    sequence.add(assocDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for topic type \"" + typeTopic.getUri() + "\" failed", e);
        }
    }

    private void storeSequence() {
        AssociationDefinition predecessor = null;
        int count = 0;
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            appendToSequence(assocDef, predecessor);
            predecessor = assocDef;
            count++;
        }
        logger.info("### Storing " + count + " sequence segments for topic type \"" + getUri() + "\"");
    }

    private void appendToSequence(AssociationDefinition assocDef, AssociationDefinition predecessor) {
        if (predecessor == null) {
            storeSequenceStart(assocDef.getId());
        } else {
            storeSequenceSegment(predecessor.getId(), assocDef.getId());
        }
    }

    private void storeSequenceStart(long assocDefId) {
        dms.createAssociation("dm3.core.association",
            new TopicRoleModel(getId(), "dm3.core.topic_type"),
            new AssociationRoleModel(assocDefId, "dm3.core.first_assoc_def"));
    }

    private void storeSequenceSegment(long predAssocDefId, long succAssocDefId) {
        dms.createAssociation("dm3.core.sequence",
            new AssociationRoleModel(predAssocDefId, "dm3.core.predecessor"),
            new AssociationRoleModel(succAssocDefId, "dm3.core.successor"));
    }

    private void rebuildSequence() {
        deleteSequence();
        storeSequence();
    }

    private void deleteSequence() {
        int count = 0;
        for (RelatedAssociation relAssoc : fetchSequence(this)) {
            Association assoc = relAssoc.getRelatingAssociation();
            dms.deleteAssociation(assoc.getId(), null);    // clientContext=null
            count++;
        }
        logger.info("### Deleting " + count + " sequence segments of topic type \"" + getUri() + "\"");
    }

    // --- Attached Object Cache ---

    private void initAssocDefs() {
        this.assocDefs = new LinkedHashMap();
        for (AssociationDefinitionModel model : getModel().getAssocDefs().values()) {
            _addAssocDef(model);
        }
    }

    private AttachedAssociationDefinition _addAssocDef(AssociationDefinitionModel model) {
        AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(model, dms);
        assocDefs.put(assocDef.getUri(), assocDef);
        return assocDef;
    }

    private AttachedAssociationDefinition _removeAssocDef(String assocDefUri) {
        // error check
        getAssocDef(assocDefUri);
        //
        return (AttachedAssociationDefinition) assocDefs.remove(assocDefUri);
    }
}
