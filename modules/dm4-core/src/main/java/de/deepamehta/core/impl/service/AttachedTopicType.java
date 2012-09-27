package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(EmbeddedService dms) {
        super(dms);     // The model and the attached object cache remain uninitialized.
                        // They are initialized later on through fetch().
    }

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === AttachedType Overrides ===

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // === AttachedType Overrides ===

    @Override
    protected void putInTypeCache() {
        dms.typeCache.put(this);
    }

    // === AttachedTopic Overrides ===

    @Override
    protected String className() {
        return "topic type";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    // ### FIXME: add to interface?
    void update(TopicTypeModel model, ClientState clientState, Directives directives) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        // Note: the UPDATE_TOPIC_TYPE directive must be added *before* a possible UPDATE_TOPIC directive (added
        // by super.update()). In case of a changed type URI the webclient's type cache must be updated *before*
        // the TopictypeRenderer can render the type.
        directives.add(Directive.UPDATE_TOPIC_TYPE, this);
        //
        boolean uriChanged = hasUriChanged(model.getUri());
        if (uriChanged) {
            dms.typeCache.invalidate(getUri());
            directives.add(Directive.DELETE_TOPIC_TYPE, new JSONWrapper("uri", getUri()));
        }
        //
        super.update(model, clientState, directives);
        //
        if (uriChanged) {
            dms.typeCache.put(this);
        }
        //
        updateDataTypeUri(model.getDataTypeUri());
        updateAssocDefs(model.getAssocDefs().values(), clientState, directives);
        updateSequence(model.getAssocDefs().values());
        updateLabelConfig(model.getLabelConfig());
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

    // ---

    private void updateAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs, ClientState clientState,
                                                                                      Directives directives) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            getAssocDef(assocDef.getUri()).update(assocDef, clientState, directives);
        }
    }

    // ---

    private void updateSequence(Collection<AssociationDefinitionModel> newAssocDefs) {
        if (hasSequenceChanged(newAssocDefs)) {
            logger.info("### Changing assoc def sequence");
            // update memory
            addAssocDefsSorted(hashAssocDefsById(), DeepaMehtaUtils.idList(newAssocDefs));
            // update DB
            rebuildSequence();
        }
    }

    private boolean hasSequenceChanged(Collection<AssociationDefinitionModel> newAssocDefs) {
        Collection<AssociationDefinition> assocDefs = getAssocDefs().values();
        if (assocDefs.size() != newAssocDefs.size()) {
            throw new RuntimeException("adding/removing of assoc defs not yet supported via updateTopicType() call");
        }
        //
        Iterator<AssociationDefinitionModel> i = newAssocDefs.iterator();
        for (AssociationDefinition assocDef : assocDefs) {
            AssociationDefinitionModel newAssocDef = i.next();
            if (!assocDef.getUri().equals(newAssocDef.getUri())) {
                return true;
            }
        }
        //
        return false;
    }

    private Map<Long, AssociationDefinition> hashAssocDefsById() {
        Map<Long, AssociationDefinition> assocDefs = new HashMap<Long, AssociationDefinition>();
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // ---

    private void updateLabelConfig(List<String> newLabelConfig) {
        if (!getLabelConfig().equals(newLabelConfig)) {
            logger.info("### Changing label configuration");
            setLabelConfig(newLabelConfig);
        }
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
