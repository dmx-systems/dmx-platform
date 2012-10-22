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

    // ### to be dropped
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
        dms.objectFactory.storeDataTypeUri(getId(), getUri(), className(), dataTypeUri);
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
        dms.objectFactory.storeIndexModes(getUri(), indexModes);
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
        AssociationDefinitionModel predecessor = lastAssocDef();
        // update memory
        getModel().addAssocDef(model);                                          // update model
        _addAssocDef(model);                                                    // update attached object cache
        // update DB
        dms.objectFactory.storeAssociationDefinition(model);
        dms.objectFactory.appendToSequence(getUri(), model, predecessor);
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
        dms.objectFactory.rebuildSequence(getId(), getUri(), className(), getModel().getAssocDefs().values());
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

    // ### to be dropped
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
        // init attached object cache ### FIXME: to be dropped
        initAssocDefs();
        initViewConfig();
        //
        // Note: the attached object cache must be initialized *before* storing the type-specific parts because
        // storeAssocDefs() relies on the association definitions.
        // ### FIXME: this requirment must be dropped. Storage must rely solely on the model.
        //
        // 2) store the type-specific parts
        // ### associateDataType();
        // ### storeIndexModes();
        // ### storeAssocDefs();
        storeLabelConfig();
        getViewConfig().store(clientState);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Store ===

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
     *
     * ### TODO: move to class TypeModel?
     */
    private AssociationDefinitionModel lastAssocDef() {
        AssociationDefinitionModel lastAssocDef = null;
        for (AssociationDefinitionModel assocDef : getModel().getAssocDefs().values()) {
            lastAssocDef = assocDef;
        }
        return lastAssocDef;
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
    private void _addAssocDef(AssociationDefinitionModel model) {
        AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(model, dms);
        assocDefs.put(assocDef.getUri(), assocDef);
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
