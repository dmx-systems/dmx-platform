package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
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

    private static final String DEFAULT_URI_PREFIX = "domain.project.topic_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, AssociationDefinition> assocDefs;   // Attached object cache
    private AttachedViewConfiguration viewConfig;           // Attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedType(EmbeddedService dms) {
        super(dms);         // Note: the model and the attached object cache are initialized through fetch().
    }

    AttachedType(TypeModel model, EmbeddedService dms) {
        super(model, dms);  // Note: the attached object cache is initialized through store().
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

    // === Label Configuration ===

    @Override
    public List<String> getLabelConfig() {
        return getModel().getLabelConfig();
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



    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected abstract void putInTypeCache();



    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Fetches this type from DB, initializes its model, and puts the type in the type cache.
     * <p>
     * Called from {@link TypeCache#loadTopicType}.
     *
     * @param   model   A type model with only the base-topic parts initialized.
     */
    void fetch(TypeModel model) {
        setModel(model);
        //
        // 1) init data type
        getModel().setDataTypeUri(fetchDataTypeTopic().getUri());
        // 2) init index modes
        getModel().setIndexModes(fetchIndexModes());
        // 3) init association definitions
        initAssociationDefinitions();
        //
        // Note 1: the type's association definitions must be initialized and the type must be
        // put in cache *before* its label configuration is fetched.
        //
        // Note 2: the type must be put in cache *before* its view configuration is fetched.
        // Othewise endless recursion might occur. Consider this case: fetching the topic type "Icon"
        // implies fetching its view configuration which in turn implies fetching the topic type "Icon"!
        // This is because "Icon" *is part of* "View Configuration" and has a view configuration itself
        // (provided by the deepamehta-iconpicker module).
        // We resolve that circle by postponing the view configuration retrieval. This works because Icon's
        // view configuration is actually not required while fetching, but solely its data type is
        // (see AttachedDeepaMehtaObject.fetchComposite()).
        //
        putInTypeCache();   // abstract
        //
        // 4) init label configuration
        getModel().setLabelConfig(fetchLabelConfig());
        // 5) init view configuration
        fetchViewConfig();
        //
        // init attached object cache
        // ### initAssocDefs();    // Note: the assoc defs are already initialized through previous addAssocDefsSorted()
        // ### initViewConfig();   // Note: initialized through fetchViewConfig()
    }

    void store() {
        // 1) store the base-topic parts
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setSimpleValue(getSimpleValue());
        // Note: if no URI is set a default URI is generated
        if (getUri().equals("")) {
            setUri(DEFAULT_URI_PREFIX + getId());
        }
        // init attached object cache
        initAssocDefs();
        initViewConfig();
        // Note: the attached object cache must be initialized *after* storing the base-topic parts because
        // initViewConfig() relies on the type's ID (unknown before stored) or URI (possibly unknown before stored).
        // Note: the attached object cache must be initialized *before* storing the type-specific parts because
        // storeAssocDefs() relies on the association definitions.
        //
        // 2) store the type-specific parts
        dms.associateDataType(getUri(), getDataTypeUri());
        storeIndexModes();
        storeAssocDefs();
        storeLabelConfig();
        getViewConfig().store();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private RelatedTopic fetchDataTypeTopic() {
        try {
            RelatedTopic dataType = getRelatedTopic("dm4.core.aggregation", "dm4.core.type", null,
                "dm4.core.data_type", false, false);     // fetchComposite=false
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to " + className() + " \"" +
                    getUri() + "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for " + className() + " \"" +
                getUri() + "\" failed", e);
        }
    }

    private Set<IndexMode> fetchIndexModes() {
        ResultSet<RelatedTopic> topics = getRelatedTopics("dm4.core.aggregation", "dm4.core.type", null,
            "dm4.core.index_mode", false, false, 0);       // fetchComposite=false
        return IndexMode.fromTopics(topics.getItems());
    }

    // ---

    private void initAssociationDefinitions() {
        Map<Long, AttachedAssociationDefinition> assocDefs = fetchAssociationDefinitions();
        List<RelatedAssociation> sequence = fetchSequence();
        // sanity check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("Graph inconsistency: " + assocDefs.size() + " association " +
                "definitions found but sequence length is " + sequence.size());
        }
        //
        addAssocDefsSorted(assocDefs, sequence);
    }

    private Map<Long, AttachedAssociationDefinition> fetchAssociationDefinitions() {
        Map<Long, AttachedAssociationDefinition> assocDefs = new HashMap();
        //
        // fetch part topic types
        List assocTypeFilter = Arrays.asList("dm4.core.aggregation_def", "dm4.core.composition_def");
        ResultSet<RelatedTopic> partTopicTypes = getRelatedTopics(assocTypeFilter, "dm4.core.whole_type",
            "dm4.core.part_type", "dm4.core.topic_type", false, false, 0);
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

    // ---

    private List<String> fetchLabelConfig() {
        List<String> labelConfig = new ArrayList();
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            SimpleValue includeInLabel = assocDef.getChildTopicValue("dm4.core.include_in_label");
            if (includeInLabel != null && includeInLabel.booleanValue()) {
                labelConfig.add(assocDef.getUri());
            }
        }
        return labelConfig;
    }

    private void fetchViewConfig() {
        try {
            ResultSet<RelatedTopic> topics = getRelatedTopics("dm4.core.aggregation", "dm4.core.type",
                "dm4.core.view_config", null, true, false, 0);    // fetchComposite=true, fetchRelatingComposite=false
            // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
            getModel().setViewConfig(new ViewConfigurationModel(dms.getTopicModels(topics.getItems())));
            initViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for " + className() + " \"" + getUri() +
                "\" failed", e);
        }
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
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getUri(), "dm4.core.type"),
                new TopicRoleModel(indexMode.toUri(), "dm4.core.default"));
        }
    }

    private void storeAssocDefs() {
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            ((AttachedAssociationDefinition) assocDef).store();
        }
        storeSequence();
    }

    private void storeLabelConfig() {
        List<String> labelConfig = getLabelConfig();
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            boolean includeInLabel = labelConfig.contains(assocDef.getUri());
            assocDef.setChildTopicValue("dm4.core.include_in_label", new SimpleValue(includeInLabel));
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
            RelatedAssociation assocDef = getRelatedAssociation("dm4.core.aggregation", "dm4.core.type",
                "dm4.core.sequence_start", null, false, false);   // othersAssocTypeUri=null
            // fetch sequence segments
            if (assocDef != null) {
                sequence.add(assocDef);
                while ((assocDef = assocDef.getRelatedAssociation("dm4.core.sequence",
                    "dm4.core.predecessor", "dm4.core.successor")) != null) {
                    //
                    sequence.add(assocDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for " + className() + " \"" + getUri() + "\" failed", e);
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
        logger.fine("Storing " + count + " sequence segments for " + className() + " \"" + getUri() + "\"");
    }

    private void appendToSequence(AssociationDefinition assocDef, AssociationDefinition predecessor) {
        if (predecessor == null) {
            storeSequenceStart(assocDef.getId());
        } else {
            storeSequenceSegment(predecessor.getId(), assocDef.getId());
        }
    }

    private void storeSequenceStart(long assocDefId) {
        dms.createAssociation("dm4.core.aggregation",
            new TopicRoleModel(getId(), "dm4.core.type"),
            new AssociationRoleModel(assocDefId, "dm4.core.sequence_start"));
    }

    private void storeSequenceSegment(long predAssocDefId, long succAssocDefId) {
        dms.createAssociation("dm4.core.sequence",
            new AssociationRoleModel(predAssocDefId, "dm4.core.predecessor"),
            new AssociationRoleModel(succAssocDefId, "dm4.core.successor"));
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
        logger.info("### Deleting " + count + " sequence segments of " + className() + " \"" + getUri() + "\"");
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
        RoleModel configurable = new TopicRoleModel(getId(), "dm4.core.type");
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }
}
