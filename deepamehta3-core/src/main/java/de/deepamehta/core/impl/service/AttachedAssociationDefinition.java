package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;
import java.util.logging.Logger;



/**
 * An association definition that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationDefinition implements AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationDefinitionModel model;
    private AttachedViewConfiguration viewConfig;
    private final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationDefinition(EmbeddedService dms) {
        this.dms = dms;     // The model and viewConfig remain uninitialized.
                            // They are initialized later on through fetch().
    }

    AttachedAssociationDefinition(AssociationDefinitionModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
        initViewConfig();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssociationDefinition Implementation ===

    @Override
    public long getId() {
        return model.getId();
    }

    @Override
    public String getUri() {
        return model.getUri();
    }

    @Override
    public String getAssocTypeUri() {
        return model.getAssocTypeUri();
    }

    @Override
    public String getInstanceLevelAssocTypeUri() {
        return model.getInstanceLevelAssocTypeUri();
    }

    @Override
    public String getTopicTypeUri1() {
        return model.getTopicTypeUri1();
    }

    @Override
    public String getTopicTypeUri2() {
        return model.getTopicTypeUri2();
    }

    @Override
    public String getRoleTypeUri1() {
        return model.getRoleTypeUri1();
    }

    @Override
    public String getRoleTypeUri2() {
        return model.getRoleTypeUri2();
    }

    @Override
    public String getCardinalityUri1() {
        return model.getCardinalityUri1();
    }

    @Override
    public String getCardinalityUri2() {
        return model.getCardinalityUri2();
    }

    @Override
    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    @Override
    public void setId(long id) {
        model.setId(id);
    }

    @Override
    public void setAssocTypeUri(String assocTypeUri) {
        model.setAssocTypeUri(assocTypeUri);
    }

    @Override
    public void setCardinalityUri1(String cardinalityUri1) {
        model.setCardinalityUri1(cardinalityUri1);
    }

    @Override
    public void setCardinalityUri2(String cardinalityUri2) {
        model.setCardinalityUri2(cardinalityUri2);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * @param   topicTypeUri    only used for sanity check
     */
    void fetch(Association assoc, String topicTypeUri) {
        try {
            TopicTypes topicTypes = fetchTopicTypes(assoc);
            // ### RoleTypes roleTypes = fetchRoleTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            // sanity check
            if (!topicTypes.topicTypeUri1.equals(topicTypeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(),
                topicTypes.topicTypeUri1, topicTypes.topicTypeUri2
                /* ###, roleTypes.roleTypeUri1, roleTypes.roleTypeUri2 */);
            model.setCardinalityUri1(cardinality.cardinalityUri1);
            model.setCardinalityUri2(cardinality.cardinalityUri2);
            model.setAssocTypeUri(assoc.getTypeUri());
            model.setViewConfigModel(fetchViewConfig(assoc));
            //
            setModel(model);
            initViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition for topic type \"" + topicTypeUri +
                "\" failed (" + assoc + ")", e);
        }
    }

    /**
     * @param   predecessor     The predecessor of the new assocdef. The new assocdef is added after this one.
     *                          <code>null</code> indicates the sequence start. 
     */
    void store(AssociationDefinition predecessor) {
        try {
            // Note: creating the underlying association is conditional. It exists already for
            // an interactively created association definition. Its ID is already set.
            if (getId() == -1) {
                Association assoc = dms.createAssociation(getAssocTypeUri(),
                    new TopicRoleModel(getTopicTypeUri1(), "dm3.core.topic_type_1"),
                    new TopicRoleModel(getTopicTypeUri2(), "dm3.core.topic_type_2"));
                setId(assoc.getId());
            } else {
                logger.info("########## Association for assoc def \"" + getUri() + "\" exists already");
            }
            // role types
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getRoleTypeUri1(), "dm3.core.role_type_1"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getRoleTypeUri2(), "dm3.core.role_type_2"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            // cardinality
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getCardinalityUri1(), "dm3.core.cardinality_1"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getCardinalityUri2(), "dm3.core.cardinality_2"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            //
            putInSequence(predecessor);
            //
            storeViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + getUri() +
                "\" of topic type \"" + getTopicTypeUri1() + "\" failed", e);
        }
    }

    // ---

    AssociationDefinitionModel getModel() {
        return model;
    }

    private void setModel(AssociationDefinitionModel model) {
        this.model = model;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

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
            throw new RuntimeException("Missing cardinality of position 2");
        }
        return cardinality;
    }

    private ViewConfigurationModel fetchViewConfig(Association assoc) {
        // ### should we use "dm3.core.association" instead of "dm3.core.aggregation"?
        Set<RelatedTopic> topics = assoc.getRelatedTopics("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.view_config", null, true);    // fetchComposite=true
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(topics);
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

    private void putInSequence(AssociationDefinition predecessor) {
        if (predecessor == null) {
            // start sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getTopicTypeUri1(), "dm3.core.topic_type"));
            assocModel.setRoleModel2(new AssociationRoleModel(getId(), "dm3.core.first_assoc_def"));
            dms.createAssociation(assocModel, null);            // FIXME: clientContext=null
        } else {
            // continue sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.sequence");
            assocModel.setRoleModel1(new AssociationRoleModel(predecessor.getId(), "dm3.core.predecessor"));
            assocModel.setRoleModel2(new AssociationRoleModel(getId(), "dm3.core.successor"));
            dms.createAssociation(assocModel, null);            // FIXME: clientContext=null
        }
    }

    private void storeViewConfig() {
        for (TopicModel configTopic : model.getViewConfigModel().getConfigTopics()) {
            Topic topic = dms.createTopic(configTopic, null);   // FIXME: clientContext=null
            dms.createAssociation("dm3.core.aggregation",
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"),
                new TopicRoleModel(topic.getId(), "dm3.core.view_config"));
        }
    }



    // === Helper ===

    private void initViewConfig() {
        RoleModel configurable = new AssociationRoleModel(getId(), "dm3.core.assoc_def");
        this.viewConfig = new AttachedViewConfiguration(configurable, model.getViewConfigModel(), dms);
    }
}
