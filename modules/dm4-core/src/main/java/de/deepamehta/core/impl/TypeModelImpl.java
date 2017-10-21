package de.deepamehta.core.impl;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



class TypeModelImpl extends TopicModelImpl implements TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;     // may be null in models used for an update operation
    private boolean isValueType;    // true for value types, false for identity types
    private List<IndexMode> indexModes;
    private SequencedHashMap<String, AssociationDefinitionModelImpl> assocDefs; // is never null, may be empty
    private ViewConfigurationModelImpl viewConfig;                              // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeModelImpl(TopicModelImpl typeTopic, String dataTypeUri, boolean isValueType, List<IndexMode> indexModes,
                  List<AssociationDefinitionModel> assocDefs, ViewConfigurationModelImpl viewConfig) {
        super(typeTopic);
        this.dataTypeUri  = dataTypeUri;
        this.isValueType  = isValueType;
        this.indexModes   = indexModes;
        this.assocDefs    = toMap(assocDefs);
        this.viewConfig   = viewConfig;
    }

    TypeModelImpl(TypeModelImpl type) {
        super(type);
        this.dataTypeUri  = type.getDataTypeUri();
        this.isValueType  = type.isValueType();
        this.indexModes   = type.getIndexModes();
        this.assocDefs    = toMap(type.getAssocDefs());
        this.viewConfig   = type.getViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Data Type ===

    @Override
    public String getDataTypeUri() {
        return dataTypeUri;
    }

    @Override
    public void setDataTypeUri(String dataTypeUri) {
        this.dataTypeUri = dataTypeUri;
    }



    // === Value Type ===

    @Override
    public boolean isValueType() {
        return isValueType;
    }

    @Override
    public void setValueType(boolean isValueType) {
        this.isValueType = isValueType;
    }



    // === Index Modes ===

    @Override
    public List<IndexMode> getIndexModes() {
        return indexModes;
    }

    @Override
    public void addIndexMode(IndexMode indexMode) {
        indexModes.add(indexMode);
    }



    // === Association Definitions ===

    @Override
    public Collection<AssociationDefinitionModelImpl> getAssocDefs() {
        return assocDefs.values();
    }

    @Override
    public AssociationDefinitionModelImpl getAssocDef(String assocDefUri) {
        return getAssocDefOrThrow(assocDefUri);
    }

    @Override
    public boolean hasAssocDef(String assocDefUri) {
        return _getAssocDef(assocDefUri) != null;
    }

    /**
     * @param   assocDef    the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    @Override
    public TypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    /**
     * @param   assocDef            the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    @Override
    public TypeModel addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        try {
            // error check
            String assocDefUri = assocDef.getAssocDefUri();
            AssociationDefinitionModel existing = _getAssocDef(assocDefUri);
            if (existing != null) {
                throw new RuntimeException("Ambiguity: type \"" + uri + "\" has more than one \"" + assocDefUri +
                    "\" assoc defs");
            }
            //
            assocDefs.putBefore(assocDefUri, (AssociationDefinitionModelImpl) assocDef, beforeAssocDefUri);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding an assoc def to type \"" + uri + "\" before \"" + beforeAssocDefUri +
                "\" failed" + assocDef, e);
        }
    }

    @Override
    public AssociationDefinitionModel removeAssocDef(String assocDefUri) {
        try {
            AssociationDefinitionModel assocDef = assocDefs.remove(assocDefUri);
            if (assocDef == null) {
                throw new RuntimeException("Assoc def \"" + assocDefUri + "\" not found in " + assocDefs.keySet());
            }
            return assocDef;
        } catch (Exception e) {
            throw new RuntimeException("Removing assoc def \"" + assocDefUri + "\" from type \"" + uri + "\" failed",
                e);
        }
    }



    // === View Configuration ===

    @Override
    public ViewConfigurationModelImpl getViewConfig() {
        return viewConfig;
    }

    @Override
    public Object getViewConfigValue(String configTypeUri, String childTypeUri) {
        return viewConfig.getConfigValue(configTypeUri, childTypeUri);
    }

    @Override
    public void setViewConfig(ViewConfigurationModel viewConfig) {
        this.viewConfig = (ViewConfigurationModelImpl) viewConfig;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this TypeModel's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return assocDefs.keySet().iterator();
    }



    // ****************************
    // *** TopicModel Overrides ***
    // ****************************



    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("dataTypeUri", dataTypeUri)
                .put("isValueType", isValueType)
                .put("indexModeUris", toJSONArray(indexModes))
                .put("assocDefs", toJSONArray(assocDefs.values()))
                .put("viewConfigTopics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public TypeModelImpl clone() {
        try {
            TypeModelImpl model = (TypeModelImpl) super.clone();
            model.assocDefs = (SequencedHashMap) model.assocDefs.clone();
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TypeModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", value=\"" + value + "\", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + getDataTypeUri() + "\", isValueType=" + isValueType +
            ", indexModes=" + getIndexModes() + ", assocDefs=" + getAssocDefs() + ", " + getViewConfig();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Abstract Methods ===

    DeepaMehtaTypeImpl instantiate() {
        throw new UnsupportedOperationException();
    }

    List<? extends DeepaMehtaObjectModelImpl> getAllInstances() {
        throw new UnsupportedOperationException();
    }

    // ---

    Directive getUpdateTypeDirective() {
        throw new UnsupportedOperationException();
    }

    Directive getDeleteTypeDirective() {
        throw new UnsupportedOperationException();
    }



    // === Core Internal Hooks ===

    @Override
    void preUpdate(DeepaMehtaObjectModel updateModel) {
        // ### TODO: is it sufficient if we rehash (remove + add) at post-time?
        if (uriChange(updateModel.getUri(), uri)) {
            removeFromTypeCache();
        }
    }

    @Override
    void postUpdate(DeepaMehtaObjectModel updateModel, DeepaMehtaObjectModel oldObject) {
        if (uriChange(updateModel.getUri(), oldObject.getUri())) {
            putInTypeCache();
        }
        //
        updateType((TypeModelImpl) updateModel);
        //
        // Note: the UPDATE_TOPIC_TYPE/UPDATE_ASSOCIATION_TYPE directive must be added *before* a possible UPDATE_TOPIC
        // directive (added by DeepaMehtaObjectModelImpl.update()). In case of a changed type URI the webclient's type
        // cache must be updated *before* the TopicTypeRenderer/AssociationTypeRenderer can render the type.
        addUpdateTypeDirective();
    }

    // ---

    @Override
    void preDelete() {
        // 1) check pre-condition
        int size = getAllInstances().size();
        if (size > 0) {
            throw new RuntimeException(size + " \"" + value + "\" instances still exist");
        }
        // 2) delete all assoc defs
        //
        // Note 1: we use the preDelete() hook to delete the assoc defs *before* they would get deleted by the generic
        // deletion logic (see "delete direct associations" in DeepaMehtaObjectModelImpl.delete()). The generic deletion
        // logic deletes the direct associations in an *arbitrary* order. The "Sequence Start" association might get
        // deleted *before* any other assoc def. When subsequently deleting an assoc def the sequence can't be rebuild
        // as it is corrupted.
        // Note 2: iterating with a for-loop here would cause ConcurrentModificationException. Deleting an assoc def
        // implies rebuilding the sequence and that iterates with a for-loop already. Instead we must create a new
        // iterator for every single assoc def.
        String assocDefUri;
        while ((assocDefUri = getFirstAssocDefUri()) != null) {
            _getAssocDef(assocDefUri).delete();
        }
    }

    @Override
    void postDelete() {
        super.postDelete();                     // ### TODO: needed?
        //
        removeFromTypeCache();
    }



    // === Update (memory + DB) ===

    final void updateDataTypeUri(String dataTypeUri) {
        setDataTypeUri(dataTypeUri);    // update memory
        storeDataTypeUri();             // update DB
    }

    final void _addIndexMode(IndexMode indexMode) {
        // update memory
        addIndexMode(indexMode);
        // update DB
        pl.typeStorage.storeIndexMode(uri, indexMode);
        indexAllInstances(indexMode);
    }

    // ---

    final void _addAssocDefBefore(AssociationDefinitionModelImpl assocDef, String beforeAssocDefUri) {
        try {
            long lastAssocDefId = lastAssocDefId();     // must be determined *before* memory is updated
            //
            // 1) update memory
            // Note: the assoc def's custom association type is stored as a child topic. The meta model extension that
            // adds "Association Type" as a child to the "Composition Definition" and "Aggregation Definition"
            // association types has itself a custom association type (named "Custom Association Type"), see migration
            // 5. It would not be stored as storage is model driven and the (meta) model doesn't know about custom
            // associations as this very concept is introduced only by the assoc def being added here. So, the model
            // must be updated (in-memory) *before* the assoc def is stored.
            addAssocDefBefore(assocDef, beforeAssocDefUri);
            //
            // 2) update DB
            pl.typeStorage.storeAssociationDefinition(assocDef);
            long beforeAssocDefId = beforeAssocDefUri != null ? getAssocDef(beforeAssocDefUri).getId() : -1;
            long firstAssocDefId = firstAssocDefId();   // must be determined *after* memory is updated
            pl.typeStorage.addAssocDefToSequence(getId(), assocDef.getId(), beforeAssocDefId, firstAssocDefId,
                lastAssocDefId);
        } catch (Exception e) {
            throw new RuntimeException("Adding an assoc def to type \"" + uri + "\" before \"" + beforeAssocDefUri +
                "\" failed" + assocDef, e);
        }
    }

    final void _removeAssocDef(String assocDefUri) {
        // We trigger deleting an association definition by deleting the underlying association. This mimics deleting an
        // association definition interactively in the webclient. Updating this type definition's memory and DB sequence
        // is triggered then by the Type Editor plugin's preDeleteAssociation() hook. ### FIXDOC
        // This way deleting an association definition works for both cases: 1) interactive deletion (when the user
        // deletes an association), and 2) programmatical deletion (e.g. from a migration).
        getAssocDef(assocDefUri).delete();
    }



    // === Type Editor Support ===

    final void _addAssocDef(AssociationModelImpl assoc) {
        _addAssocDefBefore(pl.typeStorage.newAssociationDefinition(assoc), null);    // beforeAssocDefUri=null
        //
        addUpdateTypeDirective();
    }

    /**
     * Adjusts the type cache on post-update-assoc.
     * Called from ApplicationModel's core internal postUpdate() hook, in 2 situations:
     *   - An assoc def has been updated.
     *   - An assoc def's underlying assoc has been updated.
     *
     * @param   assoc       the updated generic association. ### FIXDOC: might be an assoc def as well
     */
    final void _updateAssocDef(AssociationModel assoc, AssociationModel oldAssoc) {
        String[] assocDefUris = findAssocDefUris(assoc.getId());
        AssociationDefinitionModel assocDef = getAssocDef(assocDefUris[0]);
        String oldAssocDefUri;
        if (assoc == assocDef) {
            // updated "via assoc def"
            oldAssocDefUri = ((AssociationDefinitionModel) oldAssoc).getAssocDefUri();
        } else {
            // updated "via assoc"
            oldAssocDefUri = assocDef.getAssocDefUri();
            // transfer assoc model to assoc def model
            assocDef.setTypeUri(assoc.getTypeUri());
            assocDef.setSimpleValue(assoc.getSimpleValue());
            assocDef.setChildTopicsModel(assoc.getChildTopicsModel());
        }
        // rehash if assoc def URI has changed (due to changed custom assoc type)
        if (!assocDef.getAssocDefUri().equals(oldAssocDefUri)) {
            rehashAssocDef(oldAssocDefUri, assocDefUris[1]);
        }
        //
        addUpdateTypeDirective();
    }

    /**
     * Removes an association from memory and rebuilds the sequence in DB. Note: the underlying
     * association is *not* removed from DB.
     * This method is called (by the Type Editor plugin's preDeleteAssociation() hook) when the
     * deletion of an association that represents an association definition is imminent. ### FIXDOC
     */
    final void _removeAssocDefFromMemoryAndRebuildSequence(AssociationModel assoc) {
        String[] assocDefUris = findAssocDefUris(assoc.getId());
        String assocDefUri = getAssocDef(assocDefUris[0]).getAssocDefUri();
        // update memory
        removeAssocDef(assocDefUri);
        // update DB
        pl.typeStorage.rebuildSequence(this);
        //
        addUpdateTypeDirective();
    }



    // === Label Configuration ===

    final List<String> getLabelConfig() {
        try {
            List<String> labelConfig = new ArrayList();
            for (String assocDefUri : this) {
                if (getAssocDef(assocDefUri).includeInLabel()) {
                    labelConfig.add(assocDefUri);
                }
            }
            return labelConfig;
        } catch (Exception e) {
            throw new RuntimeException("Calculating the label configuration for type \"" + uri + "\" failed", e);
        }
    }



    // === Access Control ===

    final <M extends TypeModelImpl> M filterReadableAssocDefs() {
        try {
            Iterator<String> i = iterator();
            while (i.hasNext()) {
                String assocDefUri = i.next();
                if (!_getAssocDef(assocDefUri).isReadable()) {
                    i.remove();
                }
            }
            return (M) this;
        } catch (Exception e) {
            throw new RuntimeException("Filtering readable assoc defs of type \"" + uri + "\" failed", e);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addUpdateTypeDirective() {
        Directives.get().add(getUpdateTypeDirective(), instantiate());
    }



    // === Update (memory + DB) ===

    private void updateType(TypeModelImpl updateModel) {
        _updateDataTypeUri(updateModel.getDataTypeUri());
        _updateAssocDefs(updateModel.getAssocDefs());
        _updateSequence(updateModel.getAssocDefs());
    }

    // ---

    private void _updateDataTypeUri(String newDataTypeUri) {
        if (newDataTypeUri != null) {
            String dataTypeUri = getDataTypeUri();
            if (!dataTypeUri.equals(newDataTypeUri)) {
                logger.info("### Changing data type URI from \"" + dataTypeUri + "\" -> \"" + newDataTypeUri + "\"");
                updateDataTypeUri(newDataTypeUri);
            }
        }
    }

    private void _updateAssocDefs(Collection<AssociationDefinitionModelImpl> newAssocDefs) {
        for (AssociationDefinitionModelImpl assocDef : newAssocDefs) {
            // Note: if the assoc def's custom association type was changed the assoc def URI changes as well.
            // So we must identify the assoc def to update **by ID**.
            // ### TODO: drop updateAssocDef() and rehash here (that is remove + add).
            String[] assocDefUris = findAssocDefUris(assocDef.getId());
            getAssocDef(assocDefUris[0]).update(assocDef);
        }
    }

    private void _updateSequence(Collection<AssociationDefinitionModelImpl> newAssocDefs) {
        try {
            // ### TODO: only update sequence if there is a change request
            //
            // update memory
            logger.info("##### Updating sequence (" + newAssocDefs.size() + "/" + getAssocDefs().size() +
                " assoc defs)");
            rehashAssocDefs(newAssocDefs);
            // update DB
            pl.typeStorage.rebuildSequence(this);
        } catch (Exception e) {
            throw new RuntimeException("Updating the assoc def sequence failed", e);
        }
    }



    // === Store (DB only) ===

    private void storeDataTypeUri() {
        // remove current assignment
        getRelatedTopic("dm4.core.aggregation", "dm4.core.type", "dm4.core.default", "dm4.core.data_type")
            .getRelatingAssociation().delete();
        // create new assignment
        pl.typeStorage.storeDataType(uri, dataTypeUri);
    }

    private void indexAllInstances(IndexMode indexMode) {
        List<? extends DeepaMehtaObjectModelImpl> objects = getAllInstances();
        //
        String str = "\"" + value + "\" (" + uri + ") instances";
        if (indexModes.size() > 0) {
            if (objects.size() > 0) {
                logger.info("### Indexing " + objects.size() + " " + str + " (indexMode=" + indexMode + ")");
            } else {
                logger.info("### Indexing " + str + " SKIPPED -- no instances in DB");
            }
        } else {
            logger.info("### Indexing " + str + " SKIPPED -- no index mode set");
        }
        //
        for (DeepaMehtaObjectModelImpl obj : objects) {
            obj.indexSimpleValue(indexMode);
        }
    }



    // === Association Definitions (memory access) ===

    /**
     * Finds an assoc def by ID and returns its URI (at index 0). Returns the URI of the next-in-sequence
     * assoc def as well (at index 1), or null if the found assoc def is the last one.
     */
    private String[] findAssocDefUris(long assocDefId) {
        if (assocDefId == -1) {
            throw new IllegalArgumentException("findAssocDefUris() called with assocDefId=-1");
        }
        String[] assocDefUris = new String[2];
        Iterator<String> i = iterator();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            long _assocDefId = checkAssocDefId(_getAssocDef(assocDefUri));
            if (_assocDefId == assocDefId) {
                assocDefUris[0] = assocDefUri;
                if (i.hasNext()) {
                    assocDefUris[1] = i.next();
                }
                break;
            }
        }
        if (assocDefUris[0] == null) {
            throw new RuntimeException("Assoc def with ID " + assocDefId + " not found in assoc defs of type \"" + uri +
                "\" (" + assocDefs.keySet() + ")");
        }
        return assocDefUris;
    }

    // ### TODO: not called
    private boolean hasSameAssocDefSequence(Collection<? extends AssociationDefinitionModel> assocDefs) {
        Collection<? extends AssociationDefinitionModel> _assocDefs = getAssocDefs();
        if (assocDefs.size() != _assocDefs.size()) {
            return false;
        }
        //
        Iterator<? extends AssociationDefinitionModel> i = assocDefs.iterator();
        for (AssociationDefinitionModel _assocDef : _assocDefs) {
            AssociationDefinitionModel assocDef = i.next();
            // Note: if the assoc def's custom association type changed the assoc def URI changes as well.
            // So we must identify the assoc defs to compare **by ID**.
            long assocDefId  = checkAssocDefId(assocDef);
            long _assocDefId = checkAssocDefId(_assocDef);
            if (assocDefId != _assocDefId) {
                return false;
            }
        }
        //
        return true;
    }

    // ---

    private void rehashAssocDefs(Collection<AssociationDefinitionModelImpl> newAssocDefs) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            rehashAssocDef(assocDef.getAssocDefUri(), null);
        }
    }

    private void rehashAssocDef(String assocDefUri, String beforeAssocDefUri) {
        AssociationDefinitionModel assocDef = removeAssocDef(assocDefUri);
        logger.info("### Rehashing assoc def \"" + assocDefUri + "\" -> \"" + assocDef.getAssocDefUri() +
            "\" (put " + (beforeAssocDefUri != null ? "before \"" + beforeAssocDefUri + "\"" : "at end") + ")");
        addAssocDefBefore(assocDef, beforeAssocDefUri);
    }

    // ---

    private AssociationDefinitionModelImpl getAssocDefOrThrow(String assocDefUri) {
        AssociationDefinitionModelImpl assocDef = _getAssocDef(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Assoc def \"" + assocDefUri + "\" not found in " + assocDefs.keySet());
        }
        return assocDef;
    }

    private AssociationDefinitionModelImpl _getAssocDef(String assocDefUri) {
        return assocDefs.get(assocDefUri);
    }

    // ---

    /**
     * Returns the ID of the last association definition of this type or
     * <code>-1</code> if there are no association definitions.
     */
    private long lastAssocDefId() {
        long lastAssocDefId = -1;
        for (AssociationDefinitionModel assocDef : getAssocDefs()) {
            lastAssocDefId = assocDef.getId();
        }
        return lastAssocDefId;
    }

    private long firstAssocDefId() {
        return getAssocDefs().iterator().next().getId();
    }

    private String getFirstAssocDefUri() {
        Iterator<String> i = iterator();
        return i.hasNext() ? i.next() : null;
    }

    // ---

    private long checkAssocDefId(AssociationDefinitionModel assocDef) {
        long assocDefId = assocDef.getId();
        if (assocDefId == -1) {
            throw new RuntimeException("The assoc def ID is uninitialized (-1): " + assocDef);
        }
        return assocDefId;
    }

    // ---

    private SequencedHashMap<String, AssociationDefinitionModelImpl> toMap(
                                                           Collection<? extends AssociationDefinitionModel> assocDefs) {
        SequencedHashMap<String, AssociationDefinitionModelImpl> _assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            _assocDefs.put(assocDef.getAssocDefUri(), (AssociationDefinitionModelImpl) assocDef);
        }
        return _assocDefs;
    }



    // === Type Cache (memory access) ===

    private void putInTypeCache() {
        pl.typeStorage.putInTypeCache(this);
    }

    /**
     * Removes this type from type cache and adds a DELETE TYPE directive to the given set of directives.
     */
    private void removeFromTypeCache() {
        pl.typeStorage.removeFromTypeCache(uri);
        //
        Directive dir = getDeleteTypeDirective();   // abstract
        Directives.get().add(dir, new JSONWrapper("uri", uri));
    }



    // === Serialization ===

    private JSONArray toJSONArray(List<IndexMode> indexModes) {
        JSONArray indexModeUris = new JSONArray();
        for (IndexMode indexMode : indexModes) {
            indexModeUris.put(indexMode.toUri());
        }
        return indexModeUris;
    }

    private JSONArray toJSONArray(Collection<? extends AssociationDefinitionModel> assocDefs) {
        JSONArray _assocDefs = new JSONArray();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            _assocDefs.put(assocDef.toJSON());
        }
        return _assocDefs;
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private static final class JSONWrapper implements JSONEnabled {

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
