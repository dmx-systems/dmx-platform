package systems.dmx.core.impl;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.service.Directive;
import systems.dmx.core.service.Directives;
import systems.dmx.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



class TypeModelImpl extends TopicModelImpl implements TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    String dataTypeUri;     // may be null in models used for an update operation
    SequencedHashMap<String, CompDefModelImpl> assocDefs;       // is never null, may be empty
    ViewConfigurationModelImpl viewConfig;                      // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeModelImpl(TopicModelImpl typeTopic, String dataTypeUri, List<CompDefModel> assocDefs,
                                                                ViewConfigurationModelImpl viewConfig) {
        super(typeTopic);
        this.dataTypeUri  = dataTypeUri;
        this.assocDefs    = toMap(assocDefs);
        this.viewConfig   = viewConfig;
    }

    TypeModelImpl(TypeModelImpl type) {
        super(type);
        this.dataTypeUri  = type.getDataTypeUri();
        this.assocDefs    = toMap(type.getCompDefs());
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



    // === Association Definitions ===

    @Override
    public Collection<CompDefModelImpl> getCompDefs() {
        return assocDefs.values();
    }

    @Override
    public CompDefModelImpl getCompDef(String compDefUri) {
        return getCompDefOrThrow(compDefUri);
    }

    @Override
    public boolean hasCompDef(String compDefUri) {
        return _getCompDef(compDefUri) != null;
    }

    /**
     * @param   compDef     the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    @Override
    public TypeModel addCompDef(CompDefModel compDef) {
        return addCompDefBefore(compDef, null);   // beforeCompDefUri=null
    }

    /**
     * @param   compDef             the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeCompDefUri    the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    @Override
    public TypeModel addCompDefBefore(CompDefModel compDef, String beforeCompDefUri) {
        try {
            String compDefUri = compDef.getCompDefUri();
            // error check
            CompDefModel existing = _getCompDef(compDefUri);
            if (existing != null) {
                throw new RuntimeException("Type \"" + uri + "\" has a \"" + compDefUri + "\" assoc def already");
            }
            //
            assocDefs.putBefore(compDefUri, (CompDefModelImpl) compDef, beforeCompDefUri);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding assoc def \"" + compDef.getCompDefUri() + "\" to type \"" + uri +
                "\" failed (compDef=" + compDef + ", beforeCompDefUri=\"" + beforeCompDefUri + "\")", e);
        }
    }

    @Override
    public CompDefModel removeCompDef(String compDefUri) {
        try {
            CompDefModel compDef = assocDefs.remove(compDefUri);
            if (compDef == null) {
                throw new RuntimeException("Assoc def \"" + compDefUri + "\" not found in " + assocDefs.keySet());
            }
            return compDef;
        } catch (Exception e) {
            throw new RuntimeException("Removing assoc def \"" + compDefUri + "\" from type \"" + uri + "\" failed", e);
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
                .put("assocDefs", toJSONArray(assocDefs.values()))
                .put("viewConfigTopics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
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

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Abstract Methods ===

    DMXTypeImpl instantiate() {
        throw new UnsupportedOperationException();
    }

    List<? extends DMXObjectModelImpl> getAllInstances() {
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
    void preUpdate(DMXObjectModel updateModel) {
        // ### TODO: is it sufficient if we rehash (remove + add) at post-time?
        if (uriChange(updateModel.getUri(), uri)) {
            removeFromTypeCache();
        }
    }

    @Override
    void postUpdate(DMXObjectModel updateModel, DMXObjectModel oldObject) {
        if (uriChange(updateModel.getUri(), oldObject.getUri())) {
            putInTypeCache();
        }
        //
        updateType((TypeModelImpl) updateModel);
        //
        // Note: the UPDATE_TOPIC_TYPE/UPDATE_ASSOCIATION_TYPE directive must be added *before* a possible UPDATE_TOPIC
        // directive (added by DMXObjectModelImpl.update()). In case of a changed type URI the webclient's type
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
        // deletion logic (see "delete direct associations" in DMXObjectModelImpl.delete()). The generic deletion
        // logic deletes the direct associations in an *arbitrary* order. The "Sequence Start" association might get
        // deleted *before* any other assoc def. When subsequently deleting an assoc def the sequence can't be rebuild
        // as it is corrupted.
        // Note 2: iterating with a for-loop here would cause ConcurrentModificationException. Deleting an assoc def
        // implies rebuilding the sequence and that iterates with a for-loop already. Instead we must create a new
        // iterator for every single assoc def.
        String compDefUri;
        while ((compDefUri = getFirstCompDefUri()) != null) {
            _getCompDef(compDefUri).delete();
        }
    }

    @Override
    void postDelete() {
        super.postDelete();     // ### TODO: needed?
        removeFromTypeCache();
    }



    // === Update (memory + DB) ===

    final void updateDataTypeUri(String dataTypeUri) {
        setDataTypeUri(dataTypeUri);    // update memory
        storeDataTypeUri();             // update DB
    }

    // ---

    /**
     * Adds an assoc def to this type model and to the DB sequence.
     *
     * Preconditions:
     * - The underlying assoc exists in DB already
     * - The assoc def's view config is stored in DB already
     */
    final void _addCompDefBefore(CompDefModelImpl compDef, String beforeCompDefUri) {
        try {
            long lastCompDefId = lastCompDefId();     // must be determined *before* memory is updated
            //
            // 1) update memory
            // Note: the assoc def's custom association type is stored as a child topic. The meta model extension that
            // adds "Association Type" as a child to the "Composition Definition" association type has itself a custom
            // association type (named "Custom Association Type"), see migration 5. It would not be stored as storage is
            // model driven and the (meta) model doesn't know about custom associations as this very concept is
            // introduced only by the assoc def being added here. So, the model must be updated (in-memory) *before* the
            // assoc def is stored.
            addCompDefBefore(compDef, beforeCompDefUri);
            //
            // 2) update DB
            long beforeCompDefId = beforeCompDefUri != null ? getCompDef(beforeCompDefUri).getId() : -1;
            long firstCompDefId = firstCompDefId();   // must be determined *after* memory is updated
            pl.typeStorage.addCompDefToSequence(id, compDef.id, beforeCompDefId, firstCompDefId, lastCompDefId);
        } catch (Exception e) {
            throw new RuntimeException("Adding assoc def \"" + compDef.getCompDefUri() + "\" to type \"" + uri +
                "\" failed (compDef=" + compDef + ", beforeCompDefUri=\"" + beforeCompDefUri + "\")", e);
        }
    }

    final void _removeCompDef(String compDefUri) {
        // We trigger deleting an association definition by deleting the underlying association. This mimics deleting an
        // association definition interactively in the webclient. Updating this type definition's memory and DB sequence
        // is triggered then by the Type Editor plugin's preDeleteAssociation() hook. ### FIXDOC
        // This way deleting an association definition works for both cases: 1) interactive deletion (when the user
        // deletes an association), and 2) programmatical deletion (e.g. from a migration).
        getCompDef(compDefUri).delete();
    }



    // === Type Editor Support ===

    // 3 methods to bridge between assoc and assoc def

    final void _addCompDef(AssociationModelImpl assoc) {
        CompDefModelImpl compDef = pl.typeStorage.newCompDefModel(assoc);
        pl.typeStorage.storeViewConfig(compDef);
        _addCompDefBefore(compDef, null);      // beforeCompDefUri=null
        // FIXME: move addUpdateTypeDirective() call to _addCompDefBefore()? At the moment when adding ab assoc def via
        // TypeImpl no directives are added.
        // (TypeImpl addCompDef() and addCompDefBefore() calls _addCompDefBefore() directly.)
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
    final void _updateCompDef(AssociationModel assoc, AssociationModel oldAssoc) {
        String[] assocDefUris = findCompDefUris(assoc.getId());
        CompDefModel compDef = getCompDef(assocDefUris[0]);
        String oldCompDefUri;
        if (assoc == compDef) {
            // updated "via assoc def"
            oldCompDefUri = ((CompDefModel) oldAssoc).getCompDefUri();
        } else {
            // updated "via assoc"
            oldCompDefUri = compDef.getCompDefUri();
            // transfer assoc model to assoc def model
            compDef.setTypeUri(assoc.getTypeUri());
            compDef.setSimpleValue(assoc.getSimpleValue());
            compDef.setChildTopicsModel(assoc.getChildTopicsModel());
        }
        // rehash if assoc def URI has changed (due to changed custom assoc type)
        if (!compDef.getCompDefUri().equals(oldCompDefUri)) {
            rehashCompDef(oldCompDefUri, assocDefUris[1]);
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
    final void _removeCompDefFromMemoryAndRebuildSequence(AssociationModel assoc) {
        String[] assocDefUris = findCompDefUris(assoc.getId());
        String compDefUri = getCompDef(assocDefUris[0]).getCompDefUri();
        // update memory
        removeCompDef(compDefUri);
        // update DB
        pl.typeStorage.rebuildSequence(this);
        //
        addUpdateTypeDirective();
    }



    // === Identity Configuration ===

    final List<String> getIdentityAttrs() {
        try {
            List<String> identityAttrs = new ArrayList();
            for (String compDefUri : this) {
                if (getCompDef(compDefUri).isIdentityAttr()) {
                    identityAttrs.add(compDefUri);
                }
            }
            return identityAttrs;
        } catch (Exception e) {
            throw new RuntimeException("Calculating the identity configuration for type \"" + uri + "\" failed", e);
        }
    }



    // === Label Configuration ===

    /**
     * Prerequisite: this is a composite type.
     */
    final List<String> getLabelCompDefUris() {
        List<String> labelConfig = getLabelConfig();
        if (labelConfig.size() > 0) {
            return labelConfig;
        } else {
            List<String> assocDefUris = new ArrayList();
            Iterator<? extends CompDefModel> i = getCompDefs().iterator();
            // Note: types just created might have no child types yet
            if (i.hasNext()) {
                assocDefUris.add(i.next().getCompDefUri());
            }
            return assocDefUris;
        }
    }

    final List<String> getLabelConfig() {
        try {
            List<String> labelConfig = new ArrayList();
            for (String compDefUri : this) {
                if (getCompDef(compDefUri).includeInLabel()) {
                    labelConfig.add(compDefUri);
                }
            }
            return labelConfig;
        } catch (Exception e) {
            throw new RuntimeException("Calculating the label configuration for type \"" + uri + "\" failed", e);
        }
    }



    // === Access Control ===

    final <M extends TypeModelImpl> M filterReadableCompDefs() {
        try {
            Iterator<String> i = iterator();
            while (i.hasNext()) {
                String compDefUri = i.next();
                if (!_getCompDef(compDefUri).isReadable()) {
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
        _updateCompDefs(updateModel.getCompDefs());
        _updateSequence(updateModel.getCompDefs());
    }

    // ---

    // TODO: the public object API setters should call these _update() methods (instead update())

    private void _updateDataTypeUri(String newDataTypeUri) {
        if (newDataTypeUri != null) {
            String dataTypeUri = getDataTypeUri();
            if (!dataTypeUri.equals(newDataTypeUri)) {
                logger.info("### Changing data type URI: \"" + dataTypeUri + "\" -> \"" + newDataTypeUri + "\"");
                updateDataTypeUri(newDataTypeUri);
            }
        }
    }

    private void _updateCompDefs(Collection<CompDefModelImpl> newCompDefs) {
        for (CompDefModelImpl compDef : newCompDefs) {
            // Note: if the assoc def's custom association type was changed the assoc def URI changes as well.
            // So we must identify the assoc def to update **by ID**.
            // ### TODO: drop updateCompDef() and rehash here (that is remove + add).
            String[] assocDefUris = findCompDefUris(compDef.getId());
            getCompDef(assocDefUris[0]).update(compDef);
        }
    }

    private void _updateSequence(Collection<CompDefModelImpl> newCompDefs) {
        try {
            // ### TODO: only update sequence if there is a change request
            //
            // update memory
            logger.info("##### Updating sequence (" + newCompDefs.size() + "/" + getCompDefs().size() + " assoc defs)");
            rehashCompDefs(newCompDefs);
            // update DB
            pl.typeStorage.rebuildSequence(this);
        } catch (Exception e) {
            throw new RuntimeException("Updating the assoc def sequence failed", e);
        }
    }



    // === Store (DB only) ===

    private void storeDataTypeUri() {
        // remove current assignment
        getRelatedTopic("dmx.core.composition", "dmx.core.type", "dmx.core.default", "dmx.core.data_type")
            .getRelatingAssociation().delete();
        // create new assignment
        pl.typeStorage.storeDataType(uri, dataTypeUri);
    }



    // === Association Definitions (memory access) ===

    /**
     * Finds an assoc def by ID and returns its URI (at index 0). Returns the URI of the next-in-sequence
     * assoc def as well (at index 1), or null if the found assoc def is the last one.
     */
    private String[] findCompDefUris(long assocDefId) {
        if (assocDefId == -1) {
            throw new IllegalArgumentException("findCompDefUris() called with assocDefId=-1");
        }
        String[] assocDefUris = new String[2];
        Iterator<String> i = iterator();
        while (i.hasNext()) {
            String compDefUri = i.next();
            long _assocDefId = checkCompDefId(_getCompDef(compDefUri));
            if (_assocDefId == assocDefId) {
                assocDefUris[0] = compDefUri;
                if (i.hasNext()) {
                    assocDefUris[1] = i.next();
                }
                break;
            }
        }
        if (assocDefUris[0] == null) {
            throw new RuntimeException("Assoc def " + assocDefId + " not found in assoc defs of type \"" + uri +
                "\" (" + assocDefs.keySet() + ")");
        }
        return assocDefUris;
    }

    // ### TODO: not called
    private boolean hasSameCompDefSequence(Collection<? extends CompDefModel> assocDefs) {
        Collection<? extends CompDefModel> _assocDefs = getCompDefs();
        if (assocDefs.size() != _assocDefs.size()) {
            return false;
        }
        //
        Iterator<? extends CompDefModel> i = assocDefs.iterator();
        for (CompDefModel _assocDef : _assocDefs) {
            CompDefModel compDef = i.next();
            // Note: if the assoc def's custom association type changed the assoc def URI changes as well.
            // So we must identify the assoc defs to compare **by ID**.
            long assocDefId  = checkCompDefId(compDef);
            long _assocDefId = checkCompDefId(_assocDef);
            if (assocDefId != _assocDefId) {
                return false;
            }
        }
        //
        return true;
    }

    // ---

    private void rehashCompDefs(Collection<CompDefModelImpl> newCompDefs) {
        for (CompDefModel compDef : newCompDefs) {
            rehashCompDef(compDef.getCompDefUri(), null);
        }
    }

    private void rehashCompDef(String compDefUri, String beforeCompDefUri) {
        CompDefModel compDef = removeCompDef(compDefUri);
        logger.info("Rehashing assoc def \"" + compDefUri + "\" -> \"" + compDef.getCompDefUri() + "\" (put " +
            (beforeCompDefUri != null ? "before \"" + beforeCompDefUri + "\"" : "at end") + ")");
        addCompDefBefore(compDef, beforeCompDefUri);
    }

    // ---

    private CompDefModelImpl getCompDefOrThrow(String compDefUri) {
        CompDefModelImpl compDef = _getCompDef(compDefUri);
        if (compDef == null) {
            throw new RuntimeException("Assoc def \"" + compDefUri + "\" not found in " + assocDefs.keySet());
        }
        return compDef;
    }

    private CompDefModelImpl _getCompDef(String compDefUri) {
        return assocDefs.get(compDefUri);
    }

    // ---

    /**
     * Returns the ID of the last association definition of this type or
     * <code>-1</code> if there are no association definitions.
     */
    private long lastCompDefId() {
        long lastCompDefId = -1;
        for (CompDefModel compDef : getCompDefs()) {
            lastCompDefId = compDef.getId();
        }
        return lastCompDefId;
    }

    private long firstCompDefId() {
        return getCompDefs().iterator().next().getId();
    }

    private String getFirstCompDefUri() {
        Iterator<String> i = iterator();
        return i.hasNext() ? i.next() : null;
    }

    // ---

    private long checkCompDefId(CompDefModel compDef) {
        long assocDefId = compDef.getId();
        if (assocDefId == -1) {
            throw new RuntimeException("The assoc def ID is uninitialized (-1): " + compDef);
        }
        return assocDefId;
    }

    // ---

    private SequencedHashMap<String, CompDefModelImpl> toMap(
                                                           Collection<? extends CompDefModel> assocDefs) {
        SequencedHashMap<String, CompDefModelImpl> _assocDefs = new SequencedHashMap();
        for (CompDefModel compDef : assocDefs) {
            _assocDefs.put(compDef.getCompDefUri(), (CompDefModelImpl) compDef);
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

    private JSONArray toJSONArray(Collection<? extends CompDefModel> assocDefs) {
        JSONArray _assocDefs = new JSONArray();
        for (CompDefModel compDef : assocDefs) {
            _assocDefs.put(compDef.toJSON());
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
