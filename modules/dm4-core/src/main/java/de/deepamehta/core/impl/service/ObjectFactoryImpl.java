package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ObjectFactory;



class ObjectFactoryImpl implements ObjectFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectFactoryImpl(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** ObjectFactory Implementation ***
    // ************************************



    @Override
    public AssociationDefinition fetchAssociationDefinition(Association assoc) {
        try {
            TopicTypes topicTypes = fetchTopicTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri,
                cardinality.wholeCardinalityUri, cardinality.partCardinalityUri,
                fetchViewConfig(assoc));
            //
            return new AttachedAssociationDefinition(model, dms);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (" + assoc + ")", e);
        }
    }

    // ---

    @Override
    public RelatedTopic fetchWholeCardinality(Association assoc) {
        return assoc.getRelatedTopic("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.whole_cardinality", "dm4.core.cardinality", false, false, null);  // fetchComposite=false
    }

    @Override
    public RelatedTopic fetchPartCardinality(Association assoc) {
        return assoc.getRelatedTopic("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.part_cardinality", "dm4.core.cardinality", false, false, null);   // fetchComposite=false
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicTypes fetchTopicTypes(Association assoc) {
        String wholeTopicTypeUri = getWholeTopicTypeUri(assoc);
        String partTopicTypeUri = getPartTopicTypeUri(assoc);
        return new TopicTypes(wholeTopicTypeUri, partTopicTypeUri);
    }

    private Cardinality fetchCardinality(Association assoc) {
        Topic wholeCardinality = fetchWholeCardinality(assoc);
        Topic partCardinality = fetchPartCardinality(assoc);
        Cardinality cardinality = new Cardinality();
        if (wholeCardinality != null) {
            cardinality.setWholeCardinalityUri(wholeCardinality.getUri());
        }
        if (partCardinality != null) {
            cardinality.setPartCardinalityUri(partCardinality.getUri());
        } else {
            throw new RuntimeException("Missing part cardinality");
        }
        return cardinality;
    }

    private ViewConfigurationModel fetchViewConfig(Association assoc) {
        ResultSet<RelatedTopic> topics = assoc.getRelatedTopics("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.view_config", null, true, false, 0, null);    // fetchComposite=true, fetchRelatingComposite=false
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(dms.getTopicModels(topics.getItems()));
    }

    // === Helper ===

    // ### FIXME: copy in TypeEditorPlugin
    private String getWholeTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm4.core.whole_type").getUri();
    }

    // ### FIXME: copy in TypeEditorPlugin
    private String getPartTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm4.core.part_type").getUri();
    }



    // ------------------------------------------------------------------------------------------- Private Inner Classes

    private class TopicTypes {

        private String wholeTopicTypeUri;
        private String partTopicTypeUri;

        private TopicTypes(String wholeTopicTypeUri, String partTopicTypeUri) {
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = partTopicTypeUri;
        }
    }

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
}
