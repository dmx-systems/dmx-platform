package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.Type;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



abstract class AttachedType extends AttachedTopic implements Type {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, AssociationDefinition> assocDefs;   // Attached object cache
    private ViewConfiguration viewConfig;                   // Attached object cache

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
        dms.typeStorage.storeDataTypeUri(getId(), getUri(), className(), dataTypeUri);
    }

    // --- Index Modes ---

    @Override
    public Set<IndexMode> getIndexModes() {
        return getModel().getIndexModes();
    }

    @Override
    public void setIndexModes(Set<IndexMode> indexModes) {
        // update memory
        getModel().setIndexModes(indexModes);
        // update DB
        dms.typeStorage.storeIndexModes(getUri(), indexModes);
    }

    // --- Association Definitions ---

    @Override
    public Collection<AssociationDefinition> getAssocDefs() {
        return assocDefs.values();
    }

    @Override
    public AssociationDefinition getAssocDef(String childTypeUri) {
        AssociationDefinition assocDef = assocDefs.get(childTypeUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                childTypeUri + "\" not found in " + this);
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
        dms.typeStorage.storeAssociationDefinition(model);
        dms.typeStorage.appendToSequence(getUri(), model, predecessor);
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
    public void removeAssocDef(String childTypeUri) {
        // update memory
        getModel().removeAssocDef(childTypeUri);                                // update model
        AttachedAssociationDefinition assocDef = _removeAssocDef(childTypeUri); // update attached object cache
        // update DB
        dms.typeStorage.rebuildSequence(this);
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
        dms.typeStorage.storeLabelConfig(labelConfig, getModel().getAssocDefs());
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



    // === Updating ===

    @Override
    public void update(TypeModel model, ClientState clientState, Directives directives) {
        boolean uriChanged = hasUriChanged(model.getUri());
        if (uriChanged) {
            removeFromTypeCache();                                                  // abstract
            addDeleteTypeDirective(directives, new JSONWrapper("uri", getUri()));   // abstract
        }
        //
        super.update(model, clientState, directives);
        //
        if (uriChanged) {
            putInTypeCache();   // abstract
        }
        //
        updateDataTypeUri(model.getDataTypeUri());
        updateAssocDefs(model.getAssocDefs(), clientState, directives);
        updateSequence(model.getAssocDefs());
        updateLabelConfig(model.getLabelConfig());
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract void putInTypeCache();

    abstract void removeFromTypeCache();

    // ---

    abstract void addDeleteTypeDirective(Directives directives, JSONEnabled arg);

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

    // ---

    private void updateAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs, ClientState clientState,
                                                                                      Directives directives) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            getAssocDef(assocDef.getChildTypeUri()).update(assocDef, clientState, directives);
        }
    }

    // ---

    private void updateSequence(Collection<AssociationDefinitionModel> newAssocDefs) {
        if (!hasSequenceChanged(newAssocDefs)) {
            return;
        }
        logger.info("### Changing assoc def sequence");
        // update memory
        getModel().removeAllAssocDefs();
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            getModel().addAssocDef(assocDef);
        }
        initAssocDefs();    // attached object cache
        // update DB
        dms.typeStorage.rebuildSequence(this);
    }

    private boolean hasSequenceChanged(Collection<AssociationDefinitionModel> newAssocDefs) {
        Collection<AssociationDefinition> assocDefs = getAssocDefs();
        if (assocDefs.size() != newAssocDefs.size()) {
            throw new RuntimeException("adding/removing of assoc defs not yet supported via updateTopicType() call");
        }
        //
        Iterator<AssociationDefinitionModel> i = newAssocDefs.iterator();
        for (AssociationDefinition assocDef : assocDefs) {
            AssociationDefinitionModel newAssocDef = i.next();
            if (!assocDef.getChildTypeUri().equals(newAssocDef.getChildTypeUri())) {
                return true;
            }
        }
        //
        return false;
    }

    // ---

    private void updateLabelConfig(List<String> newLabelConfig) {
        if (!getLabelConfig().equals(newLabelConfig)) {
            logger.info("### Changing label configuration");
            setLabelConfig(newLabelConfig);
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
        for (AssociationDefinitionModel assocDef : getModel().getAssocDefs()) {
            lastAssocDef = assocDef;
        }
        return lastAssocDef;
    }

    // --- Attached Object Cache ---

    // ### FIXME: make it private
    protected void initAssocDefs() {
        this.assocDefs = new LinkedHashMap();
        for (AssociationDefinitionModel model : getModel().getAssocDefs()) {
            _addAssocDef(model);
        }
    }

    /**
     * @param   model   the new association definition.
     *                  Note: all fields must be initialized.
     */
    private void _addAssocDef(AssociationDefinitionModel model) {
        AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(model, dms);
        assocDefs.put(assocDef.getChildTypeUri(), assocDef);
    }

    private AttachedAssociationDefinition _removeAssocDef(String childTypeUri) {
        // error check
        getAssocDef(childTypeUri);
        //
        return (AttachedAssociationDefinition) assocDefs.remove(childTypeUri);
    }

    // ---

    private void initViewConfig() {
        RoleModel configurable = dms.typeStorage.createConfigurableType(getId());   // ### type ID is uninitialized
        this.viewConfig = new AttachedViewConfiguration(configurable, getModel().getViewConfigModel(), dms);
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
