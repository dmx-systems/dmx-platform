package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(EmbeddedService dms) {
        super(dms);     // the model and viewConfig remain uninitialized.
                        // They are initialued later on through fetch().
    }

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
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
        return getModel().getAssocDefs();
    }

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        return getModel().getAssocDef(assocDefUri);
    }

    @Override
    public void addAssocDef(AssociationDefinition assocDef) {
        AssociationDefinition predAssocDef = findLastAssocDef();
        // update memory
        getModel().addAssocDef(assocDef);
        // update DB
        storeAssocDef(assocDef, predAssocDef);
    }



    // === TopicBase Overrides ===

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(String topicTypeUri) {
        Topic typeTopic = dms.getTopic("uri", new TopicValue(topicTypeUri), false);     // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        //
        Map<Long, AssociationDefinition> assocDefs = fetchAssociationDefinitions(typeTopic);
        //
        List<Long> sequenceIds = fetchSequenceIds(typeTopic);
        // sanity check
        if (assocDefs.size() != sequenceIds.size()) {
            throw new RuntimeException("Graph inconsistency: " + assocDefs.size() + " association " +
                "definitions found but sequence length is " + sequenceIds.size());
        }
        // build type model
        TopicTypeModel model = new TopicTypeModel(typeTopic, fetchDataTypeTopic(typeTopic).getUri(),
                                                             fetchIndexModes(typeTopic),
                                                             fetchViewConfig(typeTopic));
        addAssocDefs(model, assocDefs, sequenceIds);
        //
        setModel(model);
        initViewConfig();
    }

    void store() {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(this);
        setValue(getValue());
        //
        dms.associateDataType(getUri(), getDataTypeUri());
        storeIndexModes();
        storeAssocDefs();
        getViewConfig().store();
    }

    void update(TopicTypeModel topicTypeModel) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + topicTypeModel + ")");
        String uri = topicTypeModel.getUri();
        TopicValue value = topicTypeModel.getValue();
        String dataTypeUri = topicTypeModel.getDataTypeUri();
        //
        boolean uriChanged = !getUri().equals(uri);
        boolean valueChanged = !getValue().equals(value);
        boolean dataTypeChanged = !getDataTypeUri().equals(dataTypeUri);
        //
        if (uriChanged || valueChanged) {
            if (uriChanged) {
                logger.info("Changing URI from \"" + getUri() + "\" -> \"" + uri + "\"");
            }
            if (valueChanged) {
                logger.info("Changing name from \"" + getValue() + "\" -> \"" + value + "\"");
            }
            super.update(topicTypeModel);
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

    private Map<Long, AssociationDefinition> fetchAssociationDefinitions(Topic typeTopic) {
        Map<Long, AssociationDefinition> assocDefs = new HashMap();
        for (Association assoc : dms.getAssociations(typeTopic.getId(), "dm3.core.topic_type_1")) {
            AssociationDefinition assocDef = fetchAssociationDefinition(assoc, typeTopic.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    /**
     * @param   topicTypeUri    only used for sanity check
     */
    private AssociationDefinition fetchAssociationDefinition(Association assoc, String topicTypeUri) {
        try {
            TopicTypes topicTypes = fetchTopicTypes(assoc);
            // ### RoleTypes roleTypes = fetchRoleTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            // sanity check
            if (!topicTypes.topicTypeUri1.equals(topicTypeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            AssociationDefinition assocDef = new AssociationDefinition(assoc.getId(),
                topicTypes.topicTypeUri1, topicTypes.topicTypeUri2
                /* ###, roleTypes.roleTypeUri1, roleTypes.roleTypeUri2 */);
            assocDef.setCardinalityUri1(cardinality.cardinalityUri1);
            assocDef.setCardinalityUri2(cardinality.cardinalityUri2);
            assocDef.setAssocTypeUri(assoc.getTypeUri());
            assocDef.setViewConfigModel(fetchViewConfig(assoc));
            return assocDef;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition for topic type \"" + topicTypeUri +
                "\" failed (" + assoc + ")", e);
        }
    }

    // ---

    private List<Long> fetchSequenceIds(Topic typeTopic) {
        try {
            // FIXME: don't make storage low-level calls here
            // TODO: extend Topic       interface by getRelatedAssociation
            // TODO: extend Association interface by getRelatedAssociation
            List<Long> sequenceIds = new ArrayList();
            Association assocDef = dms.storage.getTopicRelatedAssociation(typeTopic.getId(), "dm3.core.association",
                                                               "dm3.core.topic_type", "dm3.core.first_assoc_def");
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

    private void addAssocDefs(TopicTypeModel topicTypeModel, Map<Long, AssociationDefinition> assocDefs,
                                                             List<Long> sequenceIds) {
        for (long assocDefId : sequenceIds) {
            AssociationDefinition assocDef = assocDefs.get(assocDefId);
            // sanity check
            if (assocDef == null) {
                throw new RuntimeException("Graph inconsistency: ID " + assocDefId +
                    " is in sequence but association definition is not found");
            }
            //
            topicTypeModel.addAssocDef(assocDef);
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

    // ---

    private ViewConfigurationModel fetchViewConfig(Association assoc) {
        // ### should we use "dm3.core.association" instead of "dm3.core.aggregation"?
        Set<RelatedTopic> topics = assoc.getRelatedTopics("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.view_config", null, true);    // fetchComposite=true
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(topics);
    }

    // ---

    private TopicTypes fetchTopicTypes(Association assoc) {
        String topicTypeUri1 = assoc.getTopic("dm3.core.topic_type_1").getUri();
        String topicTypeUri2 = assoc.getTopic("dm3.core.topic_type_2").getUri();
        return new TopicTypes(topicTypeUri1, topicTypeUri2);
    }

    /* ### private RoleTypes fetchRoleTypes(Association assoc) {
        Topic roleType1 = assoc.getTopic("dm3.core.role_type_1");
        Topic roleType2 = assoc.getTopic("dm3.core.role_type_2");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (roleType1 != null) {
            roleTypes.setRoleTypeUri1(roleType1.getUri());
        }
        if (roleType2 != null) {
            roleTypes.setRoleTypeUri2(roleType2.getUri());
        }
        return roleTypes;
    } */

    private Cardinality fetchCardinality(Association assoc) {
        Topic cardinality1 = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.cardinality_1", "dm3.core.cardinality", false);    // fetchComposite=false
        Topic cardinality2 = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.cardinality_2", "dm3.core.cardinality", false);    // fetchComposite=false
        Cardinality cardinality = new Cardinality();
        if (cardinality1 != null) {
            cardinality.setCardinalityUri1(cardinality1.getUri());
        }
        if (cardinality2 != null) {
            cardinality.setCardinalityUri2(cardinality2.getUri());
        } else {
            throw new RuntimeException("Missing cardinality of position 2 in association definition");
        }
        return cardinality;
    }

    // --- Inner Classes ---

    private class TopicTypes {

        private String topicTypeUri1;
        private String topicTypeUri2;

        private TopicTypes(String topicTypeUri1, String topicTypeUri2) {
            this.topicTypeUri1 = topicTypeUri1;
            this.topicTypeUri2 = topicTypeUri2;
        }
    }

    /* ### private class RoleTypes {

        private String roleTypeUri1;
        private String roleTypeUri2;

        private void setRoleTypeUri1(String roleTypeUri1) {
            this.roleTypeUri1 = roleTypeUri1;
        }

        private void setRoleTypeUri2(String roleTypeUri2) {
            this.roleTypeUri2 = roleTypeUri2;
        }
    } */

    private class Cardinality {

        private String cardinalityUri1;
        private String cardinalityUri2;

        private void setCardinalityUri1(String cardinalityUri1) {
            this.cardinalityUri1 = cardinalityUri1;
        }

        private void setCardinalityUri2(String cardinalityUri2) {
            this.cardinalityUri2 = cardinalityUri2;
        }
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
        AssociationDefinition predAssocDef = null;
        for (AssociationDefinition assocDef : getAssocDefs().values()) {
            storeAssocDef(assocDef, predAssocDef);
            predAssocDef = assocDef;
        }
    }

    /**
     * @param   predAssocDef    The predecessor of the new assocdef. The new assocdef is added after this one.
     *                          <code>null</code> indicates the sequence start. 
     */
    private void storeAssocDef(AssociationDefinition assocDef, AssociationDefinition predAssocDef) {
        try {
            // topic types
            Association assoc = dms.createAssociation(assocDef.getAssocTypeUri(),
                new TopicRoleModel(assocDef.getTopicTypeUri1(), "dm3.core.topic_type_1"),
                new TopicRoleModel(assocDef.getTopicTypeUri2(), "dm3.core.topic_type_2"));
            assocDef.setId(assoc.getId());
            // role types
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(assocDef.getRoleTypeUri1(), "dm3.core.role_type_1"),
                new AssociationRoleModel(assoc.getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(assocDef.getRoleTypeUri2(), "dm3.core.role_type_2"),
                new AssociationRoleModel(assoc.getId(), "dm3.core.assoc_def"));
            // cardinality
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(assocDef.getCardinalityUri1(), "dm3.core.cardinality_1"),
                new AssociationRoleModel(assoc.getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(assocDef.getCardinalityUri2(), "dm3.core.cardinality_2"),
                new AssociationRoleModel(assoc.getId(), "dm3.core.assoc_def"));
            //
            putInSequence(assocDef, predAssocDef);
            //
            storeViewConfig(assocDef);
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + assocDef.getUri() +
                "\" of topic type \"" + getUri() + "\" failed", e);
        }
    }

    private void putInSequence(AssociationDefinition assocDef, AssociationDefinition predAssocDef) {
        if (predAssocDef == null) {
            // start sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getUri(), "dm3.core.topic_type"));
            assocModel.setRoleModel2(new AssociationRoleModel(assocDef.getId(), "dm3.core.first_assoc_def"));
            dms.createAssociation(assocModel, null);                     // FIXME: clientContext=null
        } else {
            // continue sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.sequence");
            assocModel.setRoleModel1(new AssociationRoleModel(predAssocDef.getId(), "dm3.core.predecessor"));
            assocModel.setRoleModel2(new AssociationRoleModel(assocDef.getId(),     "dm3.core.successor"));
            dms.createAssociation(assocModel, null);                     // FIXME: clientContext=null
        }
    }

    // ---

    // FIXME: move to an AttachedAssociationDefinition class
    private void storeViewConfig(AssociationDefinition assocDef) {
        for (TopicModel configTopic : assocDef.getViewConfigModel().getConfigTopics()) {
            Topic topic = dms.createTopic(configTopic, null);           // FIXME: clientContext=null
            dms.createAssociation("dm3.core.aggregation",
                new AssociationRoleModel(assocDef.getId(), "dm3.core.assoc_def"),
                new TopicRoleModel(topic.getId(), "dm3.core.view_config"));
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
}
