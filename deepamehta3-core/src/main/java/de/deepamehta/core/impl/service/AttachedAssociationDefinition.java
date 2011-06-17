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
    public String getWholeTopicTypeUri() {
        return model.getWholeTopicTypeUri();
    }

    @Override
    public String getPartTopicTypeUri() {
        return model.getPartTopicTypeUri();
    }

    @Override
    public String getWholeRoleTypeUri() {
        return model.getWholeRoleTypeUri();
    }

    @Override
    public String getPartRoleTypeUri() {
        return model.getPartRoleTypeUri();
    }

    @Override
    public String getWholeCardinalityUri() {
        return model.getWholeCardinalityUri();
    }

    @Override
    public String getPartCardinalityUri() {
        return model.getPartCardinalityUri();
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
    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        model.setWholeCardinalityUri(wholeCardinalityUri);
    }

    @Override
    public void setPartCardinalityUri(String partCardinalityUri) {
        model.setPartCardinalityUri(partCardinalityUri);
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
            if (!topicTypes.wholeTopicTypeUri.equals(topicTypeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(),
                topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri
                /* ###, roleTypes.wholeRoleTypeUri, roleTypes.partRoleTypeUri */);
            model.setWholeCardinalityUri(cardinality.wholeCardinalityUri);
            model.setPartCardinalityUri(cardinality.partCardinalityUri);
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
                    new TopicRoleModel(getWholeTopicTypeUri(), "dm3.core.whole_topic_type"),
                    new TopicRoleModel(getPartTopicTypeUri(), "dm3.core.part_topic_type"));
                setId(assoc.getId());
            }
            // role types
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getWholeRoleTypeUri(), "dm3.core.whole_role_type"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getPartRoleTypeUri(), "dm3.core.part_role_type"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            // cardinality
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getWholeCardinalityUri(), "dm3.core.whole_cardinality"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            dms.createAssociation("dm3.core.aggregation",
                new TopicRoleModel(getPartCardinalityUri(), "dm3.core.part_cardinality"),
                new AssociationRoleModel(getId(), "dm3.core.assoc_def"));
            //
            putInSequence(predecessor);
            //
            storeViewConfig();
        } catch (Exception e) {
            throw new RuntimeException("Storing association definition \"" + getUri() +
                "\" of topic type \"" + getWholeTopicTypeUri() + "\" failed", e);
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
        String wholeTopicTypeUri = assoc.getTopic("dm3.core.whole_topic_type").getUri();
        String partTopicTypeUri = assoc.getTopic("dm3.core.part_topic_type").getUri();
        return new TopicTypes(wholeTopicTypeUri, partTopicTypeUri);
    }

    /* ### private RoleTypes fetchRoleTypes(Association assoc) {
        Topic wholeRoleType = assoc.getTopic("dm3.core.whole_role_type");
        Topic partRoleType = assoc.getTopic("dm3.core.part_role_type");
        RoleTypes roleTypes = new RoleTypes();
        // role types are optional
        if (wholeRoleType != null) {
            roleTypes.setWholeRoleTypeUri(wholeRoleType.getUri());
        }
        if (partRoleType != null) {
            roleTypes.setPartRoleTypeUri(partRoleType.getUri());
        }
        return roleTypes;
    } */

    private Cardinality fetchCardinality(Association assoc) {
        Topic wholeCardinality = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.whole_cardinality", "dm3.core.cardinality", false);    // fetchComposite=false
        Topic partCardinality = assoc.getRelatedTopic("dm3.core.aggregation", "dm3.core.assoc_def",
            "dm3.core.part_cardinality", "dm3.core.cardinality", false);    // fetchComposite=false
        Cardinality cardinality = new Cardinality();
        if (wholeCardinality != null) {
            cardinality.setWholeCardinalityUri(wholeCardinality.getUri());
        }
        if (partCardinality != null) {
            cardinality.setPartCardinalityUri(partCardinality.getUri());
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
        return new ViewConfigurationModel(dms.getTopicModels(topics));
    }

    // --- Inner Classes ---

    private class TopicTypes {

        private String wholeTopicTypeUri;
        private String partTopicTypeUri;

        private TopicTypes(String wholeTopicTypeUri, String partTopicTypeUri) {
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = partTopicTypeUri;
        }
    }

    /* ### private class RoleTypes {

        private String wholeRoleTypeUri;
        private String partRoleTypeUri;

        private void setWholeRoleTypeUri(String wholeRoleTypeUri) {
            this.wholeRoleTypeUri = wholeRoleTypeUri;
        }

        private void setPartRoleTypeUri(String partRoleTypeUri) {
            this.partRoleTypeUri = partRoleTypeUri;
        }
    } */

    private class Cardinality {

        private String wholeCardinalityUri;
        private String partCardinalityUri;

        private void setWholeCardinalityUri(String wholeCardinalityUri) {
            this.wholeCardinalityUri = wholeCardinalityUri;
        }

        private void setPartCardinalityUri(String partCardinalityUri) {
            this.partCardinalityUri = partCardinalityUri;
        }
    }



    // === Store ===

    private void putInSequence(AssociationDefinition predecessor) {
        if (predecessor == null) {
            // start sequence
            AssociationModel assocModel = new AssociationModel("dm3.core.association");
            assocModel.setRoleModel1(new TopicRoleModel(getWholeTopicTypeUri(), "dm3.core.topic_type"));
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
