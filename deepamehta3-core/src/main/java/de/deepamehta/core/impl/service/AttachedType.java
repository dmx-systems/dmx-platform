package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



abstract class AttachedType extends AttachedTopic implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, AssociationDefinition> assocDefs;   // Attached object cache
    private AttachedViewConfiguration viewConfig;           // Attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedType(EmbeddedService dms) {
        super((TopicModel) null, dms);  // The model (and thus the attached object cache) remain uninitialized.
                                        // They are initialized later on through fetch().
    }

    AttachedType(TypeModel model, EmbeddedService dms) {
        super(model, dms);
        initAssocDefs();
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************
    // *** Type Implementation ***
    // ***************************



    // === Data Type ===

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

    // === Index Modes ===

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

    // === Association Definitions ===

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

    // === View Configuration ===

    @Override
    public AttachedViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // FIXME: to be dropped
    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }



    // *******************************
    // *** AttachedTopic Overrides ***
    // *******************************



    @Override
    public TypeModel getModel() {
        return (TypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(TypeModel model) {
        setModel(model);
        //
        Map<Long, AttachedAssociationDefinition> assocDefs = fetchAssociationDefinitions();
        List<RelatedAssociation> sequence = fetchSequence();
        // sanity check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("Graph inconsistency: " + assocDefs.size() + " association " +
                "definitions found but sequence length is " + sequence.size());
        }
        // init model
        getModel().setDataTypeUri(fetchDataTypeTopic().getUri());
        getModel().setIndexModes(fetchIndexModes());
        getModel().setViewConfig(fetchViewConfig());
        addAssocDefsSorted(assocDefs, sequence);
        //
        // init attached object cache
        // ### initAssocDefs(); // Note: the assoc defs are already initialized through previous addAssocDefsSorted()
        initViewConfig();       // defined in superclass
    }

    void store() {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setSimpleValue(getSimpleValue());
        //
        dms.associateDataType(getUri(), getDataTypeUri());
        storeIndexModes();
        storeAssocDefs();
        storeSequence();
        getViewConfig().store();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private RelatedTopic fetchDataTypeTopic() {
        try {
            // ### FIXME: revise "topic type" wording in exception messages
            RelatedTopic dataType = getRelatedTopic("dm3.core.aggregation", "dm3.core.type",
                "dm3.core.data_type", "dm3.core.data_type", false, false);     // fetchComposite=false
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to topic type \"" + getUri() +
                    "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for topic type \"" + getUri() +
                "\" failed", e);
        }
    }

    private Set<IndexMode> fetchIndexModes() {
        Set<RelatedTopic> topics = getRelatedTopics("dm3.core.aggregation", "dm3.core.type",
            "dm3.core.index_mode", "dm3.core.index_mode", false, false);       // fetchComposite=false
        return IndexMode.fromTopics(topics);
    }

    private Map<Long, AttachedAssociationDefinition> fetchAssociationDefinitions() {
        Map<Long, AttachedAssociationDefinition> assocDefs = new HashMap();
        //
        // fetch part topic types ### TODO: revise "whole_topic_type" role
        List assocTypeFilter = Arrays.asList("dm3.core.aggregation_def", "dm3.core.composition_def");
        Set<RelatedTopic> partTopicTypes = getRelatedTopics(assocTypeFilter, "dm3.core.whole_type",
            "dm3.core.part_type", "dm3.core.topic_type", false, false);
        //
        for (RelatedTopic partTopicType : partTopicTypes) {
            AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(dms);
            // FIXME: pass more info of the reltopic to the fetch() method to avoid double-fetching
            assocDef.fetch(partTopicType.getAssociation(), getUri());
            // Note: the returned map is an intermediate, hashed by ID. The actual type model is
            // subsequently build from it by sorting the assoc def's according to the sequence IDs.
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    private void addAssocDefsSorted(Map<Long, AttachedAssociationDefinition> assocDefs,
                                    List<RelatedAssociation> sequence) {
        getModel().setAssocDefs(new LinkedHashMap());           // init model
        this.assocDefs = new LinkedHashMap();                   // init attached object cache
        for (RelatedAssociation relAssoc : sequence) {
            long assocDefId = relAssoc.getId();
            AttachedAssociationDefinition assocDef = assocDefs.get(assocDefId);
            // sanity check
            if (assocDef == null) {
                throw new RuntimeException("Graph inconsistency: ID " + assocDefId +
                    " is in sequence but association definition is not found");
            }
            // Note: the model and the attached object cache is updated together
            getModel().addAssocDef(assocDef.getModel());        // update model
            this.assocDefs.put(assocDef.getUri(), assocDef);    // update attached object cache
        }
    }

    private ViewConfigurationModel fetchViewConfig() {
        Set<RelatedTopic> topics = getRelatedTopics("dm3.core.aggregation", "dm3.core.type", "dm3.core.view_config",
            null, true, false);    // fetchComposite=true, fetchRelatingComposite=false
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(dms.getTopicModels(topics));
    }



    // === Store ===

    private void storeDataTypeUri() {
        // remove current assignment
        long assocId = fetchDataTypeTopic().getAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientContext=null
        // create new assignment
        dms.associateDataType(getUri(), getDataTypeUri());
    }

    private void storeIndexModes() {
        for (IndexMode indexMode : getIndexModes()) {
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getUri(), "dm3.core.type"),
                new TopicRoleModel(indexMode.toUri(), "dm3.core.index_mode"));
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

    private List<RelatedAssociation> fetchSequence() {
        try {
            List<RelatedAssociation> sequence = new ArrayList();
            // find sequence start
            RelatedAssociation assocDef = getRelatedAssociation("dm3.core.aggregation", "dm3.core.type",
                "dm3.core.sequence_start", null, false, false);   // othersAssocTypeUri=null
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
            throw new RuntimeException("Fetching sequence for topic type \"" + getUri() + "\" failed", e);
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
        logger.fine("Storing " + count + " sequence segments for topic type \"" + getUri() + "\"");
    }

    private void appendToSequence(AssociationDefinition assocDef, AssociationDefinition predecessor) {
        if (predecessor == null) {
            storeSequenceStart(assocDef.getId());
        } else {
            storeSequenceSegment(predecessor.getId(), assocDef.getId());
        }
    }

    private void storeSequenceStart(long assocDefId) {
        dms.createAssociation("dm3.core.aggregation",
            new TopicRoleModel(getId(), "dm3.core.type"),
            new AssociationRoleModel(assocDefId, "dm3.core.sequence_start"));
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
        for (RelatedAssociation relAssoc : fetchSequence()) {
            Association assoc = relAssoc.getRelatingAssociation();
            dms.deleteAssociation(assoc.getId(), null);    // clientContext=null
            count++;
        }
        logger.info("### Deleting " + count + " sequence segments of topic type \"" + getUri() + "\"");
    }



    // === Attached Object Cache ===

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

    // ---

    private void initViewConfig() {
        // Note: this type must be identified by its URI. Types being created have no ID yet.
        RoleModel configurable = new TopicRoleModel(getUri(), "dm3.core.type");
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
