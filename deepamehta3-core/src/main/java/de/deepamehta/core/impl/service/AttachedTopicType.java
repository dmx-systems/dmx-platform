package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TopicValue;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Attached object cache.
     */
    private Map<String, AssociationDefinition> assocDefs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(EmbeddedService dms) {
        super(dms);     // The model and the attached object cache remain uninitialized.
                        // They are initialized later on through fetch().
    }

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
        initAssocDefs();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicType Implementation ===

    @Override
    public String getDataTypeUri() {
        return getModel().getDataTypeUri();
    }

    @Override
    public void setDataTypeUri(String dataTypeUri) {
        // update memory
        getModel().setDataTypeUri(dataTypeUri);
        // update DB
        storeDataTypeUri();
    }

    // ---

    @Override
    public Set<IndexMode> getIndexModes() {
        return getModel().getIndexModes();
    }

    @Override
    public void setIndexModes(Set<IndexMode> indexModes) {
        // update memory
        getModel().setIndexModes(indexModes);
        // update DB
        storeIndexModes();
    }

    // ---

    @Override
    public Map<String, AssociationDefinition> getAssocDefs() {
        return assocDefs;
    }

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        AssociationDefinition assocDef = assocDefs.get(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                assocDefUri + "\" not found in " + this);
        }
        return assocDef;
    }

    @Override
    public void addAssocDef(AssociationDefinitionModel model) {
        // Note: the predecessor must be determines *before* the memory is updated
        AssociationDefinition predecessor = findLastAssocDef();
        // 1) update memory
        getModel().addAssocDefModel(model);                             // update model
        AttachedAssociationDefinition assocDef = initAssocDef(model);   // update attached object cache
        // 2) update DB
        assocDef.store(predecessor);
    }

    @Override
    public void updateAssocDef(AssociationDefinitionModel model) {
        // 1) update memory
        getModel().updateAssocDefModel(model);                          // update model
        initAssocDef(model);                                            // update attached object cache
        // 2) update DB
        // ### Note: nothing to do for the moment
        // (in case of interactive assoc type change the association is already updated in DB)
    }



    // === AttachedTopic Overrides ===

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(String topicTypeUri) {
        AttachedTopic typeTopic = dms.getTopic("uri", new TopicValue(topicTypeUri), false);     // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        Map<Long, AttachedAssociationDefinition> assocDefs = fetchAssociationDefinitions(typeTopic);
        //
        List<Long> sequenceIds = fetchSequenceIds(typeTopic);
        // sanity check
        if (assocDefs.size() != sequenceIds.size()) {
            throw new RuntimeException("Graph inconsistency: " + assocDefs.size() + " association " +
                "definitions found but sequence length is " + sequenceIds.size());
        }
        // build model
        TopicTypeModel model = new TopicTypeModel(typeTopic.getModel(), fetchDataTypeTopic(typeTopic).getUri(),
                                                                        fetchIndexModes(typeTopic),
                                                                        fetchViewConfig(typeTopic));
        addAssocDefsToModel(model, assocDefs, sequenceIds);
        // set model of this topic type
        setModel(model);
        // ### initAssocDefs(); // Note: the assoc defs are already initialized through previous addAssocDefsToModel()
        initViewConfig();   // defined in superclass
    }

    void store() {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setValue(getValue());
        //
        dms.associateDataType(getUri(), getDataTypeUri());
        storeIndexModes();
        storeAssocDefs();
        getViewConfig().store();
    }

    void update(TopicTypeModel model) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        String uri = model.getUri();
        TopicValue value = model.getValue();
        String dataTypeUri = model.getDataTypeUri();
        //
        boolean uriChanged = !getUri().equals(uri);
        boolean valueChanged = !getValue().equals(value);
        boolean dataTypeChanged = !getDataTypeUri().equals(dataTypeUri);
        //
        if (uriChanged) {
            logger.info("Changing URI from \"" + getUri() + "\" -> \"" + uri + "\"");
            dms.typeCache.invalidate(getUri());
            super.update(model);
            dms.typeCache.put(this);
        }
        if (valueChanged) {
            logger.info("Changing name from \"" + getValue() + "\" -> \"" + value + "\"");
            super.update(model);
        }
        if (dataTypeChanged) {
            logger.info("Changing data type from \"" + getDataTypeUri() + "\" -> \"" + dataTypeUri + "\"");
            setDataTypeUri(dataTypeUri);
        }
        //
        if (!uriChanged && !valueChanged && !dataTypeChanged) {
            logger.info("Updating topic type \"" + getUri() + "\" ABORTED -- no changes made by user");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private Map<Long, AttachedAssociationDefinition> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AttachedAssociationDefinition> assocDefs = new HashMap();
        for (Association assoc : typeTopic.getAssociations("dm3.core.whole_topic_type")) {
            AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(dms);
            assocDef.fetch(assoc, typeTopic.getUri());
            // Note: the returned map is an intermediate, hashed by ID. The actual type model is
            // subsequently build from it by sorting the assoc def's according to the sequence IDs.
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    private List<Long> fetchSequenceIds(Topic typeTopic) {
        try {
            // FIXME: don't make storage low-level calls here
            // TODO: extend Topic       interface by getRelatedAssociation
            // TODO: extend Association interface by getRelatedAssociation
            List<Long> sequenceIds = new ArrayList();
            AssociationModel assocDef = dms.storage.getTopicRelatedAssociation(typeTopic.getId(),
                "dm3.core.association", "dm3.core.topic_type", "dm3.core.first_assoc_def");
            if (assocDef != null) {
                sequenceIds.add(assocDef.getId());
                while ((assocDef = dms.storage.getAssociationRelatedAssociation(assocDef.getId(), "dm3.core.sequence",
                                                               "dm3.core.predecessor", "dm3.core.successor")) != null) {
                    sequenceIds.add(assocDef.getId());
                }
            }
            return sequenceIds;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence IDs for topic type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private void addAssocDefsToModel(TopicTypeModel model, Map<Long, AttachedAssociationDefinition> assocDefs,
                                                           List<Long> sequenceIds) {
        this.assocDefs = new LinkedHashMap();
        for (long assocDefId : sequenceIds) {
            AttachedAssociationDefinition assocDef = assocDefs.get(assocDefId);
            // sanity check
            if (assocDef == null) {
                throw new RuntimeException("Graph inconsistency: ID " + assocDefId +
                    " is in sequence but association definition is not found");
            }
            // Note: the model and the attached object cache is updated together.
            model.addAssocDefModel(assocDef.getModel());        // update model
            this.assocDefs.put(assocDef.getUri(), assocDef);    // update attached object cache
        }
    }

    // ---

    private RelatedTopic fetchDataTypeTopic(Topic typeTopic) {
        try {
            RelatedTopic dataType = typeTopic.getRelatedTopic("dm3.core.association", "dm3.core.topic_type",
                "dm3.core.data_type", "dm3.core.data_type", false);     // fetchComposite=false
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to topic type \"" + typeTopic.getUri() +
                    "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for topic type \"" + typeTopic.getUri() +
                "\" failed", e);
        }
    }

    private Set<IndexMode> fetchIndexModes(Topic typeTopic) {
        Set<RelatedTopic> topics = typeTopic.getRelatedTopics("dm3.core.association", "dm3.core.topic_type",
            "dm3.core.index_mode", "dm3.core.index_mode", false);       // fetchComposite=false
        return IndexMode.fromTopics(topics);
    }



    // === Store ===

    private void storeDataTypeUri() {
        // remove current assignment
        long assocId = fetchDataTypeTopic(this).getAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientContext=null
        // create new assignment
        dms.associateDataType(getUri(), getDataTypeUri());
    }

    private void storeIndexModes() {
        for (IndexMode indexMode : getIndexModes()) {
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getUri(), "dm3.core.topic_type"));
            assocModel.setRoleModel2(new TopicRoleModel(indexMode.toUri(), "dm3.core.index_mode"));
            dms.createAssociation(assocModel, null);         // FIXME: clientContext=null
        }
    }

    private void storeAssocDefs() {
        AssociationDefinition predecessor = null;
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            ((AttachedAssociationDefinition) assocDef).store(predecessor);
            predecessor = assocDef;
        }
    }



    // === Helper ===

    private AssociationDefinition findLastAssocDef() {
        AssociationDefinition lastAssocDef = null;
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            lastAssocDef = assocDef;
        }
        return lastAssocDef;
    }

    // --- Attached Object Cache ---

    private void initAssocDefs() {
        this.assocDefs = new LinkedHashMap();
        for (AssociationDefinitionModel model : getModel().getAssocDefModels().values()) {
            initAssocDef(model);
        }
    }

    private AttachedAssociationDefinition initAssocDef(AssociationDefinitionModel model) {
        AttachedAssociationDefinition assocDef = new AttachedAssociationDefinition(model, dms);
        assocDefs.put(assocDef.getUri(), assocDef);
        return assocDef;
    }
}
