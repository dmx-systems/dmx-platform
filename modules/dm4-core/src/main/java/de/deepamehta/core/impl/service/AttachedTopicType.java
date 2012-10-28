package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === AttachedType Overrides ===

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }

    @Override
    public void update(TopicTypeModel model, ClientState clientState, Directives directives) {
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
        updateAssocDefs(model.getAssocDefs(), clientState, directives);
        updateSequence(model.getAssocDefs());
        updateLabelConfig(model.getLabelConfig());
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // === AttachedTopic Overrides ===

    @Override
    protected String className() {
        return "topic type";
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
        dms.objectFactory.rebuildSequence(getId(), getUri(), className(), getModel().getAssocDefs());
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
            if (!assocDef.getUri().equals(newAssocDef.getUri())) {
                return true;
            }
        }
        //
        return false;
    }

    private Map<Long, AssociationDefinition> hashAssocDefsById() {
        Map assocDefs = new HashMap();
        for (AssociationDefinition assocDef : getAssocDefs()) {
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
