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
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.util.DeepaMehtaUtils;

import java.util.ArrayList;
import static java.util.Arrays.asList;
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

    AttachedType(TypeModel model, EmbeddedService dms) {
        super(model, dms);
        // init attached object cache
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
        // update memory
        getModel().addAssocDef(model);                                          // update model
        AttachedAssociationDefinition assocDef = _addAssocDef(model);           // update attached object cache
        // update DB
        assocDef.store();
        appendToSequence(assocDef, predecessor);
    }

    @Override
    public void updateAssocDef(AssociationDefinitionModel model) {
        // update memory
        getModel().updateAssocDef(model);                                       // update model
        _addAssocDef(model);                                                    // update attached object cache
        // update DB
        // ### Note: the DB is not updated here! In case of interactive assoc type change the association is
        // already updated in DB. => See interface comment.
    }

    @Override
    public void removeAssocDef(String assocDefUri) {
        // update memory
        getModel().removeAssocDef(assocDefUri);                                 // update model
        AttachedAssociationDefinition assocDef = _removeAssocDef(assocDefUri);  // update attached object cache
        // update DB
        rebuildSequence();
    }

    // === Label Configuration ===

    @Override
    public List<String> getLabelConfig() {
        return getModel().getLabelConfig();
    }

    @Override
    public void setLabelConfig(List<String> labelConfig) {
        // update memory
        getModel().setLabelConfig(labelConfig);
        // update DB
        storeLabelConfig();
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

    void store(ClientState clientState) {
        // 1) store the base-topic parts ### FIXME: call super.store() instead?
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setSimpleValue(getSimpleValue());
        // Note: if no URI is set a default URI is generated
        if (getUri().equals("")) {
            setUri(DEFAULT_URI_PREFIX + getId());
        }
        // Note: the attached object cache must be initialized *after* storing the base-topic parts because
        // initViewConfig() relies on the type's ID (unknown before stored) or URI (possibly unknown before stored).
        // ### FIXME: this requirment must be dropped. Storage must be performed outside of this object.
        //
        // Note: the attached object cache must be initialized *before* storing the type-specific parts because
        // storeAssocDefs() relies on the association definitions.
        // ### FIXME: this requirment must be dropped. Storage must rely solely on the model.
        //
        // 2) store the type-specific parts
        associateDataType();
        storeIndexModes();
        storeAssocDefs();
        storeLabelConfig();
        getViewConfig().store(clientState);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Store ===

    private void storeDataTypeUri() {
        // remove current assignment
        long assocId = dms.objectFactory.fetchDataTypeTopic(getId(), getUri(), className())
            .getAssociationModel().getId();
        dms.deleteAssociation(assocId, null);   // clientState=null
        // create new assignment
        associateDataType();
    }

    private void associateDataType() {
        try {
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getUri(),         "dm4.core.type"),
                new TopicRoleModel(getDataTypeUri(), "dm4.core.default"));
        } catch (Exception e) {
            throw new RuntimeException("Associating type \"" + getUri() + "\" with data type \"" +
                getDataTypeUri() + "\" failed", e);
        }
    }

    private void storeIndexModes() {
        for (IndexMode indexMode : getIndexModes()) {
            dms.createAssociation("dm4.core.aggregation",
                new TopicRoleModel(getUri(),          "dm4.core.type"),
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

    // ### FIXME: should be private
    protected void rebuildSequence() {
        deleteSequence();
        storeSequence();
    }

    private void deleteSequence() {
        int count = 0;
        List<RelatedAssociationModel> sequence = dms.objectFactory.fetchSequence(getId(), getUri(), className());
        for (RelatedAssociationModel assoc : sequence) {
            long assocId = assoc.getRelatingAssociationModel().getId();
            dms.deleteAssociation(assocId, null);   // clientState=null
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

    /**
     * @param   model   the new association definition.
     *                  Note: all fields must be initialized.
     */
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
