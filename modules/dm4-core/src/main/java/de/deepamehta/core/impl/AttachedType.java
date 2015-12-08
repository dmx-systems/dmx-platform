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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



abstract class AttachedType extends AttachedTopic implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private SequencedHashMap<String, AssociationDefinition> assocDefs;  // Attached object cache
    private ViewConfiguration viewConfig;                               // Attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedType(TypeModel model, EmbeddedService dms) {
        super(model, dms);
        // init attached object cache
        initAssocDefs();
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************************
    // *** AttachedDeepaMehtaObject Overrides ***
    // ******************************************



    // === Updating ===

    @Override
    public void update(TypeModel model) {
        boolean uriChanged = hasUriChanged(model.getUri());
        if (uriChanged) {
            _removeFromTypeCache();
        }
        //
        super.update(model);
        //
        if (uriChanged) {
            putInTypeCache();   // abstract
        }
        //
        updateDataTypeUri(model.getDataTypeUri());
        updateAssocDefs(model.getAssocDefs());
        updateSequence(model.getAssocDefs());
        updateLabelConfig(model.getLabelConfig());
    }



    // === Deletion ===

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
            _removeFromTypeCache();
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed", e);
        }
    }



    // ***************************
    // *** Type Implementation ***
    // ***************************



    // === Model ===

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
        dms.typeStorage.storeIndexMode(getUri(), indexMode);
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
            // Note: the last assoc def must be determined *before* the memory is updated
            long lastAssocDefId = lastAssocDefId();
            // 1) update memory
            getModel().addAssocDefBefore(assocDef, beforeAssocDefUri);                       // update model
            _addAssocDefBefore(new AttachedAssociationDefinition(assocDef, this, dms), beforeAssocDefUri);  // update ..
            // 2) update DB                                                                  // .. attached object cache
            dms.typeStorage.storeAssociationDefinition(assocDef);
            // update sequence
            long assocDefId = assocDef.getId();
            if (beforeAssocDefUri == null) {
                // append at end
                dms.typeStorage.appendToSequence(getId(), assocDefId, lastAssocDefId);
            } else if (firstAssocDef().getId() == assocDefId) {
                // insert at start
                dms.typeStorage.insertAtSequenceStart(getId(), assocDefId);
            } else {
                // insert in the middle
                long beforeAssocDefId = getAssocDef(beforeAssocDefUri).getId();
                dms.typeStorage.insertIntoSequence(assocDefId, beforeAssocDefId);
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding an association definition to type \"" + getUri() + "\" before \"" +
                beforeAssocDefUri + "\" failed" + assocDef, e);
        }
    }

    // Note: the assoc is required to identify its players by URI (not by ID).
    // ### TODO: actually this is not fulfilled at the moment. As a consequence you can't
    // call getParentTypeUri(), getChildTypeUri(), or getAssocDefUri() on "newAssocDef".
    //
    // Note: only memory updates here, mostly type model and little attached object cache (rehashing).
    // The DB is already updated.
    //
    // ### TODO: refactor this method in smaller ones
    @Override
    public void updateAssocDef(AssociationModel assoc) {
        // Note: if the assoc def's custom association type is changed the assoc def URI changes as well.
        // So we must identify the assoc def to update **by ID** and rehash (that is remove + add).
        String[] assocDefUris = getModel().findAssocDefUris(assoc.getId());
        AssociationDefinitionModel oldAssocDef = getModel().getAssocDef(assocDefUris[0]);
        AssociationDefinitionModel newAssocDef = new AssociationDefinitionModel(assoc);
        //
        // 1) type URI
        String oldTypeUri = oldAssocDef.getTypeUri();
        String newTypeUri = newAssocDef.getTypeUri();
        if (!oldTypeUri.equals(newTypeUri)) {
            logger.info("### Changing type URI of assoc def \"" + oldAssocDef.getAssocDefUri() + "\" from \"" +
                oldTypeUri + "\" -> \"" + newTypeUri + "\"");
            oldAssocDef.setTypeUri(newTypeUri);
        }
        //
        // 2) custom assoc type
        RelatedTopicModel oldCustomAssocType = oldAssocDef.getCustomAssocType();
        RelatedTopicModel newCustomAssocType = newAssocDef.getCustomAssocType();
        boolean oldIsSet = oldCustomAssocType != null;
        boolean newIsSet = newCustomAssocType != null;
        boolean customAssocTypeChanged = false;
        if (newIsSet && (!oldIsSet || !oldCustomAssocType.equals(newCustomAssocType))) {
            logger.info("### Changing custom association type of assoc def \"" + oldAssocDef.getAssocDefUri() +
                "\" from " + (oldIsSet ? "\"" + oldCustomAssocType.getUri() + "\"" : "<unset>") +
                " -> \"" + newCustomAssocType.getUri() + "\"");
            oldAssocDef.getChildTopicsModel().put("dm4.core.assoc_type#dm4.core.custom_assoc_type",
                newCustomAssocType);
            customAssocTypeChanged = true;
        } else if (oldIsSet && !newIsSet) {
            logger.info("### Changing custom association type of assoc def \"" + oldAssocDef.getAssocDefUri() +
                "\" from \"" + oldCustomAssocType.getUri() + "\" -> <unset>");
            oldAssocDef.getChildTopicsModel().remove("dm4.core.assoc_type#dm4.core.custom_assoc_type");
            customAssocTypeChanged = true;
        }
        //
        // ### TODO: include in label flag
        //
        // 3) label config
        if (customAssocTypeChanged) {
            // Note: if the custom association type has changed and the assoc def is part the label config
            // we must replace the assoc def URI in the label config
            List<String> labelConfig = getLabelConfig();
            String oldAssocDefUri = assocDefUris[0];
            int i = labelConfig.indexOf(oldAssocDefUri);
            if (i != -1) {
                String newAssocDefUri = oldAssocDef.getAssocDefUri();   // ### see method comment
                logger.info("### Label config: replacing \"" + oldAssocDefUri + "\" -> \"" + newAssocDefUri +
                    "\" (position " + i + ")");
                labelConfig.set(i, newAssocDefUri);
            }
        }
        //
        // 4) rehash
        if (customAssocTypeChanged) {
            // Note: if the custom association type has changed we must rehash (remove + add)
            rehashAssocDef(assocDefUris[0], assocDefUris[1]);
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
        dms.typeStorage.updateLabelConfig(labelConfig, getModel().getAssocDefs());
    }

    // --- View Configuration ---

    @Override
    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // FIXME: to be dropped
    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return getModel().getViewConfig(typeUri, settingUri);
    }

    // ---

    @Override
    public TypeModel getModel() {
        return (TypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract void putInTypeCache();

    abstract void removeFromTypeCache();

    // ---

    abstract Directive getDeleteTypeDirective();

    abstract List<? extends DeepaMehtaObject> getAllInstances();

    // ---

    // ### TODO: check if actually an assocDefUri is passed (not a childTypeUri)
    void removeAssocDefFromMemoryAndRebuildSequence(String assocDefUri) {
        // update memory
        getModel().removeAssocDef(assocDefUri);     // update model
        _removeAssocDef(assocDefUri);               // update attached object cache
        // update DB
        dms.typeStorage.rebuildSequence(this);
    }

    void rehashAssocDef(String assocDefUri, String beforeAssocDefUri) {
        getModel().rehashAssocDef(assocDefUri, beforeAssocDefUri);      // update model
        _rehashAssocDef(assocDefUri, beforeAssocDefUri);                // update attached object cache
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private boolean hasUriChanged(String newUri) {
        return newUri != null && !getUri().equals(newUri);
    }

    // ---

    private void updateDataTypeUri(String newDataTypeUri) {
        if (newDataTypeUri != null) {
            String dataTypeUri = getDataTypeUri();
            if (!dataTypeUri.equals(newDataTypeUri)) {
                logger.info("### Changing data type URI from \"" + dataTypeUri + "\" -> \"" + newDataTypeUri + "\"");
                setDataTypeUri(newDataTypeUri);
            }
        }
    }

    private void storeDataTypeUri(String dataTypeUri) {
        // remove current assignment
        getRelatedTopic("dm4.core.aggregation", "dm4.core.type", "dm4.core.default", "dm4.core.data_type")
            .getRelatingAssociation().delete();
        // create new assignment
        dms.typeStorage.storeDataType(getUri(), dataTypeUri);
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
            dms.valueStorage.indexSimpleValue(obj.getModel(), indexMode);
        }
    }

    // ---

    private void updateAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            // Note: if the assoc def's custom association type was changed the assoc def URI changes as well.
            // So we must identify the assoc def to update **by ID**.
            // ### TODO: drop updateAssocDef() and rehash here (that is remove + add).
            String[] assocDefUris = getModel().findAssocDefUris(assocDef.getId());
            getAssocDef(assocDefUris[0]).update(assocDef);
        }
    }

    private void updateSequence(Collection<AssociationDefinitionModel> newAssocDefs) {
        if (getAssocDefs().size() != newAssocDefs.size()) {
            throw new RuntimeException("adding/removing of assoc defs not yet supported via type update");
        }
        if (getModel().hasSameAssocDefSequence(newAssocDefs)) {
            return;
        }
        // update memory
        logger.info("### Changing assoc def sequence (" + getAssocDefs().size() + " items)");
        getModel().removeAllAssocDefs();    // ### TODO: reorder existing instances instead of creating new ones!
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            getModel().addAssocDef(assocDef);
        }
        initAssocDefs();    // attached object cache
        // update DB
        dms.typeStorage.rebuildSequence(this);
    }

    // ---

    private void updateLabelConfig(List<String> newLabelConfig) {
        try {
            if (!getLabelConfig().equals(newLabelConfig)) {
                logger.info("### Changing label configuration from " + getLabelConfig() + " -> " + newLabelConfig);
                setLabelConfig(newLabelConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating label configuration of type \"" + getUri() + "\" failed", e);
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

    // --- Attached Object Cache ---

    // ### FIXME: make it private
    protected void initAssocDefs() {
        this.assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : getModel().getAssocDefs()) {
            _addAssocDefBefore(new AttachedAssociationDefinition(assocDef, this, dms), null);  // beforeAssocDefUri=null
        }
    }

    private AssociationDefinition getAssocDefOrThrow(String assocDefUri) {
        AssociationDefinition assocDef = _getAssocDef(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" + assocDefUri +
                "\" not found in " + assocDefs.keySet());
        }
        return assocDef;
    }

    private AssociationDefinition _getAssocDef(String assocDefUri) {
        return assocDefs.get(assocDefUri);
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
                throw new RuntimeException("Schema violation: association definition \"" + assocDefUri +
                    "\" not found in " + assocDefs.keySet());
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

    // ---

    private void initViewConfig() {
        RoleModel configurable = dms.typeStorage.createConfigurableType(getId());   // ### type ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
    }



    // ===

    /**
     * Removes this type from type cache and adds a DELETE TYPE directive to the given set of directives.
     */
    private void _removeFromTypeCache() {
        removeFromTypeCache();                      // abstract
        addDeleteTypeDirective();
    }

    private void addDeleteTypeDirective() {
        Directive dir = getDeleteTypeDirective();   // abstract
        Directives.get().add(dir, new JSONWrapper("uri", getUri()));
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class JSONWrapper implements JSONEnabled {

        private JSONObject wrapped;

        private JSONWrapper(String key, Object value) {
            try {
                wrapped = new JSONObject();
                wrapped.put(key, value);
            } catch (Exception e) {
                throw new RuntimeException("Constructing a JSONWrapper failed", e);
            }
        }

        @Override
        public JSONObject toJSON() {
            return wrapped;
        }
    }
}
