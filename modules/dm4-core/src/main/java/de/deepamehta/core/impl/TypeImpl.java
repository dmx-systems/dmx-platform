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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



abstract class TypeImpl extends TopicImpl implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private SequencedHashMap<String, AssociationDefinition> assocDefs;  // Attached object cache ### TODO: drop this
    private ViewConfiguration viewConfig;                               // Attached object cache ### TODO: drop this

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeImpl(TypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
        // init attached object cache
        initAssocDefs();
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** DeepaMehtaObjectImpl Overrides ***
    // **************************************



    // ### TODO: refactoring. Move update logic to TypeModel.
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
        // update memory
        getModel().setDataTypeUri(dataTypeUri);
        // update DB
        storeDataTypeUri(dataTypeUri);
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
        return assocDefs.values();
    }

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        return getAssocDefOrThrow(assocDefUri);
    }

    @Override
    public boolean hasAssocDef(String assocDefUri) {
        return _getAssocDef(assocDefUri) != null;
    }

    @Override
    public Type addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    @Override
    public Type addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        try {
            long lastAssocDefId = lastAssocDefId();             // must be determined *before* the memory is updated
            //
            // 1) update memory (model)
            // Note: the assoc def's custom association type is stored as a child topic. The meta model extension that
            // adds "Association Type" as a child to the "Composition Definition" and "Aggregation Definition"
            // association types has itself a custom association type (named "Custom Association Type"), see migration
            // 5. It would not be stored as storage is model driven and the (meta) model doesn't know about custom
            // associations as this very concept is introduced only by the assoc def being added here. So, the model
            // must be updated (in-memory) *before* the assoc def is stored.
            getModel().addAssocDefBefore(assocDef, beforeAssocDefUri);
            //
            // 2) update DB
            pl.typeStorage.storeAssociationDefinition(assocDef);
            long beforeAssocDefId = beforeAssocDefUri != null ? getAssocDef(beforeAssocDefUri).getId() : -1;
            long firstAssocDefId = firstAssocDef().getId();     // must be determined *after* the memory is updated
            pl.typeStorage.addAssocDefToSequence(getId(), assocDef.getId(), beforeAssocDefId, firstAssocDefId,
                                                                                               lastAssocDefId);
            // 3) update memory (attached object cache)
            // Note: attaching an assoc def involves attaching the 2 roles of the association that assigns the assoc
            // def's custom association type (the "relating association"). These roles are only initialized while
            // storing (see ValueStorage#associateChildTopic()). (The AssociationDefinitionModel constructors leave
            // the relating association uninitialized, see AssociationDefinitionModel#childTopics()). So, the assoc
            // def must be attached *after* the assoc def is stored.
            // ### TODO: attach before store. Refactoring needed. See comment in TypeCache#put methods.
            _addAssocDefBefore(new AssociationDefinitionImpl((AssociationDefinitionModelImpl) assocDef, this, pl),
                beforeAssocDefUri);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding an association definition to type \"" + getUri() + "\" before \"" +
                beforeAssocDefUri + "\" failed" + assocDef, e);
        }
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

    // ---

    @Override
    public void _addAssocDef(Association assoc) {
        addAssocDef(pl.typeStorage.createAssociationDefinition(assoc));
    }

    @Override
    public void _updateAssocDef(Association assoc) {
        // Note: if the assoc def's custom association type is changed the assoc def URI changes as well.
        // So we must identify the assoc def to update **by ID** and rehash (that is remove + add).
        String[] assocDefUris = getModel().findAssocDefUris(assoc.getId());
        AssociationDefinition oldAssocDef = getAssocDef(assocDefUris[0]);
        if (assoc == oldAssocDef) {
            // edited via type topic -- abort
            return;
        }
        // Note: we must not manipulate the assoc model in-place. The Webclient expects by-ID roles.
        AssociationModel newAssocModel = mf.newAssociationModel(assoc.getModel());
        AssociationModel oldAssocModel = oldAssocDef.getModel();
        // Note: an assoc def expects by-URI roles.
        newAssocModel.setRoleModel1(oldAssocModel.getRoleModel1());
        newAssocModel.setRoleModel2(oldAssocModel.getRoleModel2());
        //
        AssociationDefinition newAssocDef = new AssociationDefinitionImpl(
            mf.newAssociationDefinitionModel(newAssocModel,
                oldAssocDef.getParentCardinalityUri(),
                oldAssocDef.getChildCardinalityUri(), oldAssocDef.getViewConfig().getModel()
            ),
            this, pl
        );
        String oldAssocDefUri = oldAssocDef.getAssocDefUri();
        String newAssocDefUri = newAssocDef.getAssocDefUri();
        if (oldAssocDefUri.equals(newAssocDefUri)) {
            replaceAssocDef(newAssocDef);
        } else {
            replaceAssocDef(newAssocDef, oldAssocDefUri, assocDefUris[1]);
            //
            // Note: if the custom association type has changed and the assoc def is part the label config
            // we must replace the assoc def URI in the label config
            getModel().replaceInLabelConfig(newAssocDefUri, oldAssocDefUri);
        }
    }

    @Override
    public void _removeAssocDefFromMemoryAndRebuildSequence(Association assoc) {
        String[] assocDefUris = getModel().findAssocDefUris(assoc.getId());
        String assocDefUri = getAssocDef(assocDefUris[0]).getAssocDefUri();
        // update memory
        getModel().removeAssocDef(assocDefUri);             // update model
        getModel().removeFromLabelConfig(assocDefUri);      // update model
        _removeAssocDef(assocDefUri);                       // update attached object cache
        // update DB
        pl.typeStorage.rebuildSequence(getModel());
    }

    // --- Label Configuration ---

    @Override
    public List<String> getLabelConfig() {
        return getModel().getLabelConfig();
    }

    @Override
    public void setLabelConfig(List<String> labelConfig) {
        // update memory
        getModel().setLabelConfig(labelConfig);
        // update DB
        pl.typeStorage.updateLabelConfig(labelConfig, getModel().getAssocDefs());
    }

    // --- View Configuration ---

    @Override
    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // ---

    @Override
    public void update(TypeModel newModel) {
        model.update(newModel);
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract List<? extends DeepaMehtaObject> getAllInstances();

    // ---

    void rehashAssocDef(String assocDefUri, String beforeAssocDefUri) {
        getModel().rehashAssocDef(assocDefUri, beforeAssocDefUri);      // update model
        _rehashAssocDef(assocDefUri, beforeAssocDefUri);                // update attached object cache
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void storeDataTypeUri(String dataTypeUri) {
        // remove current assignment
        getRelatedTopic("dm4.core.aggregation", "dm4.core.type", "dm4.core.default", "dm4.core.data_type")
            .getRelatingAssociation().delete();
        // create new assignment
        pl.typeStorage.storeDataType(getUri(), dataTypeUri);
    }

    // ---

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



    // === Association Definitions ===

    /**
     * Returns the ID of the last association definition of this type or
     * <code>-1</code> if there are no association definitions.
     *
     * ### TODO: move to class TypeModel?
     */
    private long lastAssocDefId() {
        long lastAssocDefId = -1;
        for (AssociationDefinitionModel assocDef : getModel().getAssocDefs()) {
            lastAssocDefId = assocDef.getId();
        }
        return lastAssocDefId;
    }

    private AssociationDefinitionModel firstAssocDef() {
        return getModel().getAssocDefs().iterator().next();
    }

    // ---

    private void replaceAssocDef(AssociationDefinition assocDef) {
        getModel().replaceAssocDef(assocDef.getModel());    // update model
        _replaceAssocDef(assocDef);         // update attached object cache
    }

    private void replaceAssocDef(AssociationDefinition assocDef, String oldAssocDefUri, String beforeAssocDefUri) {
        getModel().replaceAssocDef(assocDef.getModel(), oldAssocDefUri, beforeAssocDefUri);     // update model
        _replaceAssocDef(assocDef, oldAssocDefUri, beforeAssocDefUri);          // update attached object cache
    }

    // --- Attached Object Cache ---

    private void initAssocDefs() {
        this.assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : getModel().getAssocDefs()) {
            _addAssocDef(new AssociationDefinitionImpl((AssociationDefinitionModelImpl) assocDef, this, pl));
        }
    }

    private AssociationDefinition getAssocDefOrThrow(String assocDefUri) {
        AssociationDefinition assocDef = _getAssocDef(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found in " +
                assocDefs.keySet());
        }
        return assocDef;
    }

    private AssociationDefinition _getAssocDef(String assocDefUri) {
        return assocDefs.get(assocDefUri);
    }

    private void _addAssocDef(AssociationDefinition assocDef) {
        _addAssocDefBefore(assocDef, null);     // beforeAssocDefUri=null
    }

    /**
     * @param   beforeAssocDefUri   the assoc def <i>before</i> the assoc def is inserted into the sequence.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    private void _addAssocDefBefore(AssociationDefinition assocDef, String beforeAssocDefUri) {
        assocDefs.putBefore(assocDef.getAssocDefUri(), assocDef, beforeAssocDefUri);
    }

    private AssociationDefinition _removeAssocDef(String assocDefUri) {
        try {
            AssociationDefinition assocDef = assocDefs.remove(assocDefUri);
            if (assocDef == null) {
                throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found in " +
                    assocDefs.keySet());
            }
            return assocDef;
        } catch (Exception e) {
            throw new RuntimeException("Removing association definition \"" + assocDefUri + "\" from type \"" +
                getUri() + "\" failed", e);
        }
    }

    private void _rehashAssocDef(String assocDefUri, String beforeAssocDefUri) {
        _addAssocDefBefore(_removeAssocDef(assocDefUri), beforeAssocDefUri);
    }

    private void _rehashAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            _rehashAssocDef(assocDef.getAssocDefUri(), null);
        }
    }

    // ---

    private void _replaceAssocDef(AssociationDefinition assocDef) {
        _replaceAssocDef(assocDef, assocDef.getAssocDefUri(), null);
    }

    private void _replaceAssocDef(AssociationDefinition assocDef, String oldAssocDefUri, String beforeAssocDefUri) {
        _removeAssocDef(oldAssocDefUri);
        _addAssocDefBefore(assocDef, beforeAssocDefUri);
    }

    // ---

    private void initViewConfig() {
        RoleModel configurable = pl.typeStorage.createConfigurableType(getId());   // ### type ID is uninitialized
        this.viewConfig = new ViewConfigurationImpl(configurable, getModel().getViewConfigModel(), pl);
    }
}
