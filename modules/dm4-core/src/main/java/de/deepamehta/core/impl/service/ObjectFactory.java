package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.ViewConfigurationModel;



class ObjectFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectFactory(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * @param   topicTypeUri    only used for sanity check
     */
    AttachedAssociationDefinition fetchAssociationDefinition(Association assoc, String topicTypeUri) {
        try {
            TopicTypes topicTypes = fetchTopicTypes(assoc);
            // ### RoleTypes roleTypes = fetchRoleTypes(assoc);
            Cardinality cardinality = fetchCardinality(assoc);
            // sanity check
            if (!topicTypes.wholeTopicTypeUri.equals(topicTypeUri)) {
                throw new RuntimeException("jri doesn't understand Neo4j traversal");
            }
            //
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri,
                cardinality.wholeCardinalityUri, cardinality.partCardinalityUri,
                fetchViewConfig(assoc));
            //
            return new AttachedAssociationDefinition(model, dms);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition for topic type \"" + topicTypeUri +
                "\" failed (" + assoc + ")", e);
        }
    }

    private TopicTypes fetchTopicTypes(Association assoc) {
        String wholeTopicTypeUri = getWholeTopicTypeUri(assoc);
        String partTopicTypeUri = getPartTopicTypeUri(assoc);
        return new TopicTypes(wholeTopicTypeUri, partTopicTypeUri);
    }

    /* ### private RoleTypes fetchRoleTypes(Association assoc) {
        Topic wholeRoleType = assoc.getTopic("dm4.core.whole_role_type");
        Topic partRoleType = assoc.getTopic("dm4.core.part_role_type");
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
        Topic wholeCardinality = assoc.getRelatedTopic("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.whole_cardinality", "dm4.core.cardinality", false, false, null);    // fetchComposite=false
        Topic partCardinality = assoc.getRelatedTopic("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.part_cardinality", "dm4.core.cardinality", false, false, null);     // fetchComposite=false
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
}
