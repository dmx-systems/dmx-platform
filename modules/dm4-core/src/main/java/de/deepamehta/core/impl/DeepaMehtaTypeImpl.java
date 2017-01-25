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
    public final String getDataTypeUri() {
        return getModel().getDataTypeUri();
    }

    @Override
    public final DeepaMehtaType setDataTypeUri(String dataTypeUri) {
        _getModel().updateDataTypeUri(dataTypeUri);
        return this;
    }

    // --- Index Modes ---

    @Override
    public final List<IndexMode> getIndexModes() {
        return getModel().getIndexModes();
    }

    @Override
    public final DeepaMehtaType addIndexMode(IndexMode indexMode) {
        _getModel()._addIndexMode(indexMode);
        return this;
    }

    // --- Association Definitions ---

    @Override
    public final Collection<AssociationDefinition> getAssocDefs() {
        return pl.instantiate(getModel().getAssocDefs());
    }

    @Override
    public final AssociationDefinition getAssocDef(String assocDefUri) {
        return getModel().getAssocDef(assocDefUri).instantiate();
    }

    @Override
    public final boolean hasAssocDef(String assocDefUri) {
        return getModel().hasAssocDef(assocDefUri);
    }

    @Override
    public final DeepaMehtaType addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    @Override
    public final DeepaMehtaType addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        _getModel()._addAssocDefBefore((AssociationDefinitionModelImpl) assocDef, beforeAssocDefUri);
        return this;
    }

    @Override
    public final DeepaMehtaType removeAssocDef(String assocDefUri) {
        _getModel()._removeAssocDef(assocDefUri);
        return this;
    }

    // --- View Configuration ---

    @Override
    public final ViewConfiguration getViewConfig() {
        RoleModel configurable = pl.typeStorage.newTypeRole(getId());   // ### type ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), pl);
    }

    @Override
    public final Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // ---

    @Override
    public void update(TypeModel newModel) {
        _getModel().update((TypeModelImpl) newModel);   // ### FIXME: call through pl for access control
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) model;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Returns the <i>internal</i> (= <b>kernel</b>) model underlying this type.
     * <p>
     * Note: type updates must be performed on the internal type model, not the userland's type model (as returned by
     * <code>getModel()</code>). Performing an update on the <b>userland</b>'s type model would have no effect, as it
     * is transient. The userland's type model is always a <i>cloned</i> and filtered (= "projected") version of a
     * kernel type model which is created on-the-fly each time a specific user requests it.
     */
    abstract TypeModelImpl _getModel();

    // --- Label Configuration ---

    final List<String> getLabelConfig() {
        return getModel().getLabelConfig();
    }
}
