package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.util.JSONHelper;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Directives;

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

    void update(TopicTypeModel model, ClientContext clientContext, Directives directives) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        String uri = model.getUri();
        SimpleValue value = model.getSimpleValue();
        String dataTypeUri = model.getDataTypeUri();
        //
        boolean uriChanged = !getUri().equals(uri);
        boolean valueChanged = !getSimpleValue().equals(value);
        boolean dataTypeChanged = !getDataTypeUri().equals(dataTypeUri);
        //
        if (uriChanged || valueChanged) {
            if (uriChanged) {
                logger.info("Changing URI from \"" + getUri() + "\" -> \"" + uri + "\"");
            }
            if (valueChanged) {
                logger.info("Changing name from \"" + getSimpleValue() + "\" -> \"" + value + "\"");
            }
            if (uriChanged) {
                dms.typeCache.invalidate(getUri());
                super.update(model, clientContext, directives);
                dms.typeCache.put(this);
            } else {
                super.update(model, clientContext, directives);
            }
        }
        if (dataTypeChanged) {
            logger.info("Changing data type from \"" + getDataTypeUri() + "\" -> \"" + dataTypeUri + "\"");
            setDataTypeUri(dataTypeUri);
        }
        //
        updateSequence(model.getAssocDefs().values());
        //
        updateLabelConfig(model.getLabelConfig());
        //
        if (!uriChanged && !valueChanged && !dataTypeChanged) {
            logger.info("Updating topic type \"" + getUri() + "\" ABORTED -- no changes made by user");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Association Definitions ===

    private void updateSequence(Collection<AssociationDefinitionModel> newAssocDefs) {
        if (hasSequenceChanged(newAssocDefs)) {
            logger.info("### Changing assoc def sequence");
            // update memory
            addAssocDefsSorted(hashAssocDefsById(), JSONHelper.idList(newAssocDefs));
            // update DB
            rebuildSequence();
        } else {
            logger.info("### Updating assoc def sequence ABORTED -- no changes made by user");
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

    private Map<Long, AttachedAssociationDefinition> hashAssocDefsById() {
        Map assocDefs = new HashMap();
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    // === Label Configuration ===

    private void updateLabelConfig(List<String> newLabelConfig) {
        if (!getLabelConfig().equals(newLabelConfig)) {
            logger.info("### Changing label configuration");
            setLabelConfig(newLabelConfig);
        } else {
            logger.info("### Updating label configuration ABORTED -- no changes made by user");
        }
    }
}
