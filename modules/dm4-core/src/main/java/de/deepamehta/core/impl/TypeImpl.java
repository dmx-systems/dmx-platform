package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



abstract class TypeImpl extends TopicImpl implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeImpl(TypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************
    // *** Type Implementation ***
    // ***************************



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
        return getModel().instantiateAssocDefs();
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
    public Type addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    @Override
    public Type addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        getModel()._addAssocDefBefore(assocDef, beforeAssocDefUri);
        return this;
    }

    @Override
    public Type removeAssocDef(String assocDefUri) {
        getModel()._removeAssocDef(assocDefUri);
        return this;
    }

    // --- 3 Type Editor helpers ---

    @Override
    public void _addAssocDef(AssociationModel assoc) {
        getModel()._addAssocDef(assoc);
    }

    @Override
    public void _updateAssocDef(AssociationModel assoc) {
        getModel()._updateAssocDef(assoc);
    }

    @Override
    public void _removeAssocDefFromMemoryAndRebuildSequence(AssociationModel assoc) {
        getModel()._removeAssocDefFromMemoryAndRebuildSequence(assoc);
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
        RoleModel configurable = pl.typeStorage.createConfigurableType(getId());   // ### type ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), pl);
    }

    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // ---

    @Override
    public void update(TypeModel newModel) {
        getModel().update(newModel);
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) model;
    }
}
