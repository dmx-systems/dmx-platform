package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ObjectFactory;
import de.deepamehta.core.util.DeepaMehtaUtils;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



class ObjectFactoryImpl implements ObjectFactory {

    private static final String DEFAULT_URI_PREFIX = "domain.project.topic_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Storage topicStorage;
    private Storage assocStorage;

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectFactoryImpl(EmbeddedService dms) {
        this.topicStorage = new TopicStorage(dms);
        this.assocStorage = new AssociationStorage(dms);
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
            Cardinality cardinality = fetchCardinality(assoc.getId());
            AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                topicTypes.wholeTopicTypeUri, topicTypes.partTopicTypeUri,
                cardinality.wholeCardinalityUri, cardinality.partCardinalityUri,
                fetchAssocDefViewConfig(assoc.getId()));
            //
            return new AttachedAssociationDefinition(model, dms);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (" + assoc + ")", e);
        }
    }

    // ---

    @Override
    public Topic fetchWholeTopicType(Association assoc) {
        Topic wholeTypeTopic = assoc.getTopic("dm4.core.whole_type");
        // error check
        if (wholeTypeTopic == null) {
            throw new RuntimeException("Invalid association definition: topic role dm4.core.whole_type " +
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
            throw new RuntimeException("Invalid association definition: topic role dm4.core.part_type " +
                "is missing in " + assoc);
        }
        //
        return partTypeTopic;
    }

    // ---

    @Override
    public RelatedTopicModel fetchWholeCardinality(long assocDefId) {
        RelatedTopicModel wholeCard = dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.whole_cardinality", "dm4.core.cardinality");
        // error check
        if (wholeCard == null) {
            throw new RuntimeException("Invalid association definition: whole cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return wholeCard;
    }

    @Override
    public RelatedTopicModel fetchPartCardinality(long assocDefId) {
        RelatedTopicModel partCard = dms.storage.getAssociationRelatedTopic(assocDefId, "dm4.core.aggregation",
            "dm4.core.assoc_def", "dm4.core.part_cardinality", "dm4.core.cardinality");
        // error check
        if (partCard == null) {
            throw new RuntimeException("Invalid association definition: part cardinality is missing (assocDefId=" +
                assocDefId + ")");
        }
        //
        return partCard;
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
        // 3) init association definitions
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic.getId(), topicTypeUri,
            "topic type");
        // 4) init label configuration
        List<String> labelConfig = fetchLabelConfig(assocDefs);
        // 5) init view configuration
        ViewConfigurationModel viewConfig = fetchViewConfig(typeTopic.getId(), topicTypeUri, "topic type");
        //
        TopicTypeModel topicType = new TopicTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            viewConfig);
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
        // 3) init association definitions
        List<AssociationDefinitionModel> assocDefs = fetchAssociationDefinitions(typeTopic.getId(), assocTypeUri,
            "association type");
        // 4) init label configuration
        List<String> labelConfig = fetchLabelConfig(assocDefs);
        // 5) init view configuration
        ViewConfigurationModel viewConfig = fetchViewConfig(typeTopic.getId(), assocTypeUri, "association type");
        //
        AssociationTypeModel assocType = new AssociationTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs,
            labelConfig, viewConfig);
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



    // === Association Definitions ===

    private List<AssociationDefinitionModel> fetchAssociationDefinitions(long typeId, String typeUri,
                                                                                      String className) {
        Map<Long, AssociationDefinitionModel> assocDefs = fetchAssociationDefinitionsUnsorted(typeId, typeUri);
        List<RelatedAssociationModel> sequence = fetchSequence(typeId, typeUri, className);
        // error check
        if (assocDefs.size() != sequence.size()) {
            throw new RuntimeException("DB inconsistency: for " + className + " \"" + typeUri + "\" there are " +
                assocDefs.size() + " association definitions but sequence has " + sequence.size());
        }
        //
        return sortAssocDefs(assocDefs, DeepaMehtaUtils.idList(sequence));
    }

    private Map<Long, AssociationDefinitionModel> fetchAssociationDefinitionsUnsorted(long typeId, String typeUri) {
        Map<Long, AssociationDefinitionModel> assocDefs = new HashMap();
        //
        // 1) fetch part topic types
        // Note: we must set fetchRelatingComposite to false here. Fetching the composite of association type
        // Composition Definition would cause an endless recursion. Composition Definition is defined through
        // Composition Definition itself (child types "Include in Label", "Ordered"). ### FIXDOC
        ResultSet<RelatedTopicModel> partTopicTypes = dms.storage.getTopicRelatedTopics(typeId,
            asList("dm4.core.aggregation_def", "dm4.core.composition_def"), "dm4.core.whole_type", "dm4.core.part_type",
            "dm4.core.topic_type", 0);
        //
        // 2) create association definitions
        // Note: the returned map is an intermediate, hashed by ID. The actual type model is
        // subsequently build from it by sorting the assoc def's according to the sequence IDs.
        for (RelatedTopicModel partTopicType : partTopicTypes) {
            AssociationDefinitionModel assocDef = fetchAssociationDefinition(
                partTopicType.getAssociationModel(), typeUri, partTopicType.getUri());
            assocDefs.put(assocDef.getId(), assocDef);
        }
        return assocDefs;
    }

    private AssociationDefinitionModel fetchAssociationDefinition(AssociationModel assoc, String wholeTopicTypeUri,
                                                                                          String partTopicTypeUri) {
        try {
            Cardinality cardinality = fetchCardinality(assoc.getId());
            return new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
                wholeTopicTypeUri, partTopicTypeUri,
                cardinality.wholeCardinalityUri, cardinality.partCardinalityUri,
                fetchAssocDefViewConfig(assoc.getId()));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association definition failed (wholeTopicTypeUri=\"" +
                wholeTopicTypeUri + "\", partTopicTypeUri=" + partTopicTypeUri + ", " + assoc + ")", e);
        }
    }

    private List<AssociationDefinitionModel> sortAssocDefs(Map<Long, AssociationDefinitionModel> assocDefs,
                                                           List<Long> sequence) {
        List<AssociationDefinitionModel> sortedAssocDefs = new ArrayList();
        for (long assocDefId : sequence) {
            AssociationDefinitionModel assocDef = assocDefs.get(assocDefId);
            // error check
            if (assocDef == null) {
                throw new RuntimeException("DB inconsistency: ID " + assocDefId +
                    " is in sequence but not in the type's association definitions");
            }
            sortedAssocDefs.add(assocDef);
        }
        return sortedAssocDefs;
    }

    // --- Sequence ---

    List<RelatedAssociationModel> fetchSequence(long typeId, String typeUri, String className) {
        try {
            List<RelatedAssociationModel> sequence = new ArrayList();
            // find sequence start
            RelatedAssociationModel assocDef = dms.storage.getTopicRelatedAssociation(typeId, "dm4.core.aggregation",
                "dm4.core.type", "dm4.core.sequence_start", null);      // othersAssocTypeUri=null
            // fetch sequence segments
            if (assocDef != null) {
                sequence.add(assocDef);
                while ((assocDef = dms.storage.getAssociationRelatedAssociation(assocDef.getId(), "dm4.core.sequence",
                    "dm4.core.predecessor", "dm4.core.successor")) != null) {
                    //
                    sequence.add(assocDef);
                }
            }
            //
            return sequence;
        } catch (Exception e) {
            throw new RuntimeException("Fetching sequence for " + className + " \"" + typeUri + "\" failed", e);
        }
    }



    // ===

    private List<String> fetchLabelConfig(List<AssociationDefinitionModel> assocDefs) {
        List<String> labelConfig = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            RelatedTopicModel includeInLabel = dms.storage.getAssociationRelatedTopic(assocDef.getId(),
                "dm4.core.composition", "dm4.core.whole", "dm4.core.part", "dm4.core.include_in_label");
            if (includeInLabel != null && includeInLabel.getSimpleValue().booleanValue()) {
                labelConfig.add(assocDef.getUri());
            }
        }
        return labelConfig;
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

    private Cardinality fetchCardinality(long assocDefId) {
        TopicModel wholeCardinality = fetchWholeCardinality(assocDefId);
        TopicModel partCardinality  = fetchPartCardinality(assocDefId);
        return new Cardinality(wholeCardinality.getUri(), partCardinality.getUri());
    }

    // ---

    private ViewConfigurationModel fetchAssocDefViewConfig(long assocDefId) {
        ResultSet<RelatedTopicModel> topics = dms.storage.getAssociationRelatedTopics(assocDefId,
            "dm4.core.aggregation", "dm4.core.assoc_def", "dm4.core.view_config", null, 0);
        // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
        // ### FIXME: the composites must be fetched
        return new ViewConfigurationModel(topics.getItems());
    }

    private ViewConfigurationModel fetchViewConfig(long typeId, String typeUri, String className) {
        try {
            ResultSet<RelatedTopicModel> topics = dms.storage.getTopicRelatedTopics(typeId,
                "dm4.core.aggregation", "dm4.core.type", "dm4.core.view_config", null, 0);
            // Note: the view config's topic type is unknown (it is client-specific), othersTopicTypeUri=null
            // ### FIXME: the composites must be fetched
            return new ViewConfigurationModel(topics.getItems());
        } catch (Exception e) {
            throw new RuntimeException("Fetching view configuration for " + className + " \"" + typeUri +
                "\" failed", e);
        }
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



    // -----------------------------------------------------------------------------------------------------------------



    // *************
    // *** Store ***
    // *************



    void storeType(TypeModel type) {
        // Note: if no URI is set a default URI is generated
        if (type.getUri().equals("")) {
            type.setUri(DEFAULT_URI_PREFIX + type.getId());
        }
        //
        dms.storage.createTopic(type);
        dms.associateWithTopicType(type);
        topicStorage.storeAndIndexValue(type.getId(), type.getUri(), type.getSimpleValue());
    }
}
