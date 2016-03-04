package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
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



    // **************************************
    // *** DeepaMehtaObjectImpl Overrides ***
    // **************************************



    // ### TODO: refactoring. Move logic to model.
    @Override
    public void delete() {
        String operation = "Deleting " + className() + " \"" + getUri() + "\" (named \"" + getSimpleValue() + "\")";
        try {
            logger.info(operation);
            //
            int size = getAllInstances().size();
            if (size > 0) {
                throw new RuntimeException(size + " \"" + getSimpleValue() + "\" instances still exist");
            }
            //
            super.delete();   // delete type topic
            //
            getModel()._removeFromTypeCache();
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }



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
        // update memory
        getModel().addIndexMode(indexMode);
        // update DB
        pl.typeStorage.storeIndexMode(getUri(), indexMode);
        indexAllInstances(indexMode);
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
        // We trigger deleting an association definition by deleting the underlying association. This mimics deleting an
        // association definition interactively in the webclient. Updating this type definition's memory and DB sequence
        // is triggered then by the Type Editor plugin's preDeleteAssociation() hook.
        // This way deleting an association definition works for both cases: 1) interactive deletion (when the user
        // deletes an association), and 2) programmatical deletion (e.g. from a migration).
        getAssocDef(assocDefUri).delete();
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract List<? extends DeepaMehtaObject> getAllInstances();

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void indexAllInstances(IndexMode indexMode) {
        List<? extends DeepaMehtaObject> objects = getAllInstances();
        //
        String str = "\"" + getSimpleValue() + "\" (" + getUri() + ") instances";
        if (getIndexModes().size() > 0) {
            if (objects.size() > 0) {
                logger.info("### Indexing " + objects.size() + " " + str + " (indexMode=" + indexMode + ")");
            } else {
                logger.info("### Indexing " + str + " ABORTED -- no instances in DB");
            }
        } else {
            logger.info("### Indexing " + str + " ABORTED -- no index mode set");
        }
        //
        for (DeepaMehtaObject obj : objects) {
            ((DeepaMehtaObjectModelImpl) obj.getModel()).indexSimpleValue(indexMode);
        }
    }
}
