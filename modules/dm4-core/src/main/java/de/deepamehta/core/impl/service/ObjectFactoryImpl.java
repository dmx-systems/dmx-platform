package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ObjectFactory;

import java.util.Set;



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

    /**
     * Not part of public interface. Used internally by type loader (see AttachedType.fetchAssociationDefinitions()).
     * Type loading must perform low-level, that is by accessing the storage layer directly (bypassing the application
     * service).
     */
    AssociationDefinition fetchAssociationDefinition(Association assoc, String wholeTopicTypeUri,
                                                                        long partTopicTypeId) {
        try {
            // Note: the low-level storage call prevents possible endless recursion (caused by POST_FETCH_HOOK).
            // Consider the Access Control plugin: loading topic type dm4.accesscontrol.acl_facet would imply
            // loading its ACL which in turn would rely on this very topic type.
            // ### FIXME: is this still true? The POST_FETCH_HOOK is dropped meanwhile.
            String partTopicTypeUri = dms.storage.getTopic(partTopicTypeId).getUri();
            Cardinality cardinality = fetchCardinality(assoc);
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                wholeTopicTypeUri, partTopicTypeUri,
                cardinality.wholeCardinalityUri, cardinality.partCardinalityUri,
                fetchViewConfig(assoc));
            //
            return new AttachedAssociationDefinition(model, dms);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (wholeTopicTypeUri=\"" +
                wholeTopicTypeUri + "\", partTopicTypeId=" + partTopicTypeId + ", " + assoc + ")", e);
        }
    }

    // ---

    @Override
    public Topic fetchWholeTopicType(Association assoc) {
        Topic wholeTypeTopic = assoc.getTopic("dm4.core.whole_type");
        // error check
        if (wholeTypeTopic == null) {
            throw new RuntimeException("Illegal association definition: topic role dm4.core.whole_type " +
                "is missing in " + assoc);
        }
        //
        return wholeTypeTopic;
    }

    @Override
    public Topic fetchPartTopicType(Association assoc) {
        Topic partTypeTopic = assoc.getTopic("dm4.core.part_type");
        // error check
        if (partTypeTopic == null) {
            throw new RuntimeException("Illegal association definition: topic role dm4.core.part_type " +
                "is missing in " + assoc);
        }
        //
        return partTypeTopic;
    }

    // ---

    @Override
    public RelatedTopic fetchWholeCardinality(Association assoc) {
        // Note: the low-level storage call prevents possible endless recursion (caused by POST_FETCH_HOOK).
        // Consider the Access Control plugin: loading topic type dm4.accesscontrol.acl_facet would imply
        // loading its ACL which in turn would rely on this very topic type.
        // ### FIXME: is this still true? The POST_FETCH_HOOK is dropped meanwhile.
        RelatedTopicModel model = dms.storage.getAssociationRelatedTopic(assoc.getId(), "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.whole_cardinality", "dm4.core.cardinality");    // fetchComposite=false
        // error check
        if (model == null) {
            throw new RuntimeException("Illegal association definition: whole cardinality is missing (" + assoc + ")");
        }
        //
        return new AttachedRelatedTopic(model, dms);
    }

    @Override
    public RelatedTopic fetchPartCardinality(Association assoc) {
        // Note: the low-level storage call prevents possible endless recursion (caused by POST_FETCH_HOOK).
        // Consider the Access Control plugin: loading topic type dm4.accesscontrol.acl_facet would imply
        // loading its ACL which in turn would rely on this very topic type.
        // ### FIXME: is this still true? The POST_FETCH_HOOK is dropped meanwhile.
        RelatedTopicModel model = dms.storage.getAssociationRelatedTopic(assoc.getId(), "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.part_cardinality", "dm4.core.cardinality");     // fetchComposite=false
        // error check
        if (model == null) {
            throw new RuntimeException("Illegal association definition: part cardinality is missing (" + assoc + ")");
        }
        //
        return new AttachedRelatedTopic(model, dms);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    TopicType fetchTopicType(String topicTypeUri) {
        TopicModel typeTopic = dms.storage.getTopic("uri", new SimpleValue(topicTypeUri));
        checkTopicType(topicTypeUri, typeTopic);
        //
        // 1) init data type
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), topicTypeUri, "topic type").getUri();
        // 2) init index modes
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        //
        TopicTypeModel topicType = new TopicTypeModel(typeTopic, dataTypeUri, indexModes);
        return new AttachedTopicType(topicType, dms);
    }

    AssociationType fetchAssociationType(String assocTypeUri) {
        TopicModel typeTopic = dms.storage.getTopic("uri", new SimpleValue(assocTypeUri));
        checkAssociationType(assocTypeUri, typeTopic);
        //
        // 1) init data type
        String dataTypeUri = fetchDataTypeTopic(typeTopic.getId(), assocTypeUri, "association type").getUri();
        // 2) init index modes
        Set<IndexMode> indexModes = fetchIndexModes(typeTopic.getId());
        // ### TODO: to be completed
        //
        AssociationTypeModel assocType = new AssociationTypeModel(typeTopic, dataTypeUri, indexModes);
        return new AttachedAssociationType(assocType, dms);
    }

    // ---

    RelatedTopicModel fetchDataTypeTopic(long typeId, String typeUri, String className) {
        try {
            RelatedTopicModel dataType = dms.storage.getTopicRelatedTopic(typeId, "dm4.core.aggregation",
                "dm4.core.type", null, "dm4.core.data_type");   // ### FIXME: null
            if (dataType == null) {
                throw new RuntimeException("No data type topic is associated to " + className + " \"" + typeUri + "\"");
            }
            return dataType;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the data type topic for " + className + " \"" + typeUri + "\" failed",
                e);
        }
    }

    private Set<IndexMode> fetchIndexModes(long typeId) {
        ResultSet<RelatedTopicModel> indexModes = dms.storage.getTopicRelatedTopics(typeId, "dm4.core.aggregation",
            "dm4.core.type", null, "dm4.core.index_mode", 0);   // ### FIXME: null
        return IndexMode.fromTopics(indexModes.getItems());
    }

    // ---

    private void checkTopicType(String topicTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.topic_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_type") &&
                   !typeTopic.getTypeUri().equals("dm4.core.meta_meta_type")) {
            throw new RuntimeException("URI \"" + topicTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.topic_type\"");
        }
    }

    private void checkAssociationType(String assocTypeUri, TopicModel typeTopic) {
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        } else if (!typeTopic.getTypeUri().equals("dm4.core.assoc_type")) {
            throw new RuntimeException("URI \"" + assocTypeUri + "\" refers to a \"" + typeTopic.getTypeUri() +
                "\" when the caller expects a \"dm4.core.assoc_type\"");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicTypes fetchTopicTypes(Association assoc) {
        Topic wholeTopicType = fetchWholeTopicType(assoc);
        Topic partTopicType  = fetchPartTopicType(assoc);
        return new TopicTypes(wholeTopicType.getUri(), partTopicType.getUri());
    }

    private Cardinality fetchCardinality(Association assoc) {
        Topic wholeCardinality = fetchWholeCardinality(assoc);
        Topic partCardinality  = fetchPartCardinality(assoc);
        return new Cardinality(wholeCardinality.getUri(), partCardinality.getUri());
    }

    // ---

    private ViewConfigurationModel fetchViewConfig(Association assoc) {
        // ### FIXME: use low-level storage call.
        // ### Note: also the composite must be low-level fetched.
        ResultSet<RelatedTopic> topics = assoc.getRelatedTopics("dm4.core.aggregation", "dm4.core.assoc_def",
            "dm4.core.view_config", null, true, false, 0, null);    // fetchComposite=true, fetchRelatingComposite=false
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        return new ViewConfigurationModel(dms.getTopicModels(topics.getItems()));
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

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

        private Cardinality(String wholeCardinalityUri, String partCardinalityUri) {
            this.wholeCardinalityUri = wholeCardinalityUri;
            this.partCardinalityUri = partCardinalityUri;
        }
    }
}
