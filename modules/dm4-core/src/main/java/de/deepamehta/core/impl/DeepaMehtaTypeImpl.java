package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TypeModel;

import java.util.Collection;
import java.util.List;



abstract class DeepaMehtaTypeImpl extends TopicImpl implements DeepaMehtaType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    DeepaMehtaTypeImpl(TypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************************
    // *** DeepaMehtaType Implementation ***
    // *************************************



    // --- Data Type ---

    @Override
    public String getDataTypeUri() {
        return getModel().getDataTypeUri();
    }

    @Override
    public void setDataTypeUri(String dataTypeUri) {
        getModel().updateDataTypeUri(dataTypeUri);
    }

    // --- Index Modes ---

    @Override
    public List<IndexMode> getIndexModes() {
        return getModel().getIndexModes();
    }

    @Override
    public void addIndexMode(IndexMode indexMode) {
        getModel()._addIndexMode(indexMode);
    }

    // --- Association Definitions ---

    @Override
    public Collection<AssociationDefinition> getAssocDefs() {
        return pl.instantiate(getModel().getAssocDefs());
    }

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        return getModel().getAssocDef(assocDefUri).instantiate();
    }

    @Override
    public boolean hasAssocDef(String assocDefUri) {
        return getModel().hasAssocDef(assocDefUri);
    }

    @Override
    public DeepaMehtaType addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    @Override
    public DeepaMehtaType addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        getModel()._addAssocDefBefore(assocDef, beforeAssocDefUri);
        return this;
    }

    @Override
    public DeepaMehtaType removeAssocDef(String assocDefUri) {
        getModel()._removeAssocDef(assocDefUri);
        return this;
    }

    // --- Label Configuration ---

    @Override
    public List<String> getLabelConfig() {
        return getModel().getLabelConfig();
    }

    @Override
    public void setLabelConfig(List<String> labelConfig) {
        getModel().updateLabelConfig(labelConfig);
    }

    // --- View Configuration ---

    @Override
    public ViewConfiguration getViewConfig() {
        RoleModel configurable = pl.typeStorage.newTypeRole(getId());   // ### type ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), pl);
    }

    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // ---

    @Override
    public void update(TypeModel newModel) {
        getModel().update(newModel);    // ### FIXME: call through pl for access control
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) model;
    }
}
