package systems.dmx.core.service;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocPlayerModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedAssocModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicDeletionModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicPlayerModel;
import systems.dmx.core.model.TopicReferenceModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.model.facets.FacetValueModel;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.model.topicmaps.ViewProps;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



public interface ModelFactory {



    // === TopicModel ===

    TopicModel newTopicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics);

    TopicModel newTopicModel(ChildTopicsModel childTopics);

    TopicModel newTopicModel(String typeUri);

    TopicModel newTopicModel(String typeUri, SimpleValue value);

    TopicModel newTopicModel(String typeUri, ChildTopicsModel childTopics);

    TopicModel newTopicModel(String uri, String typeUri);

    TopicModel newTopicModel(String uri, String typeUri, SimpleValue value);

    TopicModel newTopicModel(String uri, String typeUri, ChildTopicsModel childTopics);

    // ### TODO: make internal?
    TopicModel newTopicModel(long id);

    // ### TODO: make internal?
    TopicModel newTopicModel(long id, ChildTopicsModel childTopics);

    TopicModel newTopicModel(TopicModel topic);

    TopicModel newTopicModel(JSONObject topic);



    // === AssocModel ===

    AssocModel newAssocModel(long id, String uri, String typeUri, PlayerModel player1, PlayerModel player2,
                             SimpleValue value, ChildTopicsModel childTopics);

    AssocModel newAssocModel(String typeUri, PlayerModel player1, PlayerModel player2);

    AssocModel newAssocModel(String typeUri, PlayerModel player1, PlayerModel player2,
                             ChildTopicsModel childTopics);

    // ### TODO: Refactoring needed. See comments in impl.
    AssocModel newAssocModel();

    // ### TODO: Refactoring needed. See comments in impl.
    AssocModel newAssocModel(ChildTopicsModel childTopics);

    // ### TODO: Refactoring needed. See comments in impl.
    AssocModel newAssocModel(String typeUri, ChildTopicsModel childTopics);

    AssocModel newAssocModel(long id, String uri, String typeUri, PlayerModel player1, PlayerModel player2);

    AssocModel newAssocModel(AssocModel assoc);

    AssocModel newAssocModel(JSONObject assoc);



    // === ChildTopicsModel ===

    ChildTopicsModel newChildTopicsModel();

    ChildTopicsModel newChildTopicsModel(JSONObject values);

    /**
     * Utility.
     */
    String childTypeUri(String compDefUri);



    // === TopicPlayerModel ===
    
    TopicPlayerModel newTopicPlayerModel(long topicId, String roleTypeUri);

    TopicPlayerModel newTopicPlayerModel(String topicUri, String roleTypeUri);

    TopicPlayerModel newTopicPlayerModel(long topicId, String topicUri, String roleTypeUri);

    TopicPlayerModel newTopicPlayerModel(JSONObject topicPlayerModel);



    // === AssocPlayerModel ===

    AssocPlayerModel newAssocPlayerModel(long assocId, String roleTypeUri);

    AssocPlayerModel newAssocPlayerModel(JSONObject assocPlayerModel);



    // === RelatedTopicModel ===

    RelatedTopicModel newRelatedTopicModel(long topicId);

    RelatedTopicModel newRelatedTopicModel(long topicId, AssocModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicUri);

    RelatedTopicModel newRelatedTopicModel(String topicUri, AssocModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, SimpleValue value);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic, AssocModel relatingAssoc);



    // === RelatedAssocModel ===

    RelatedAssocModel newRelatedAssocModel(AssocModel assoc, AssocModel relatingAssoc);



    // === TopicReferenceModel ===

    // TODO: make internal?

    TopicReferenceModel newTopicReferenceModel(long topicId);

    TopicReferenceModel newTopicReferenceModel(long topicId, AssocModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(String topicUri);

    TopicReferenceModel newTopicReferenceModel(String topicUri, AssocModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics);

    TopicReferenceModel newTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics);

    TopicReferenceModel newTopicReferenceModel(Object topicIdOrUri);



    // === TopicDeletionModel ===

    // TODO: make internal?

    TopicDeletionModel newTopicDeletionModel(long topicId);

    TopicDeletionModel newTopicDeletionModel(String topicUri);



    // === TopicTypeModel ===

    TopicTypeModel newTopicTypeModel(TopicModel typeTopic, String dataTypeUri, List<CompDefModel> compDefs,
                                     ViewConfigurationModel viewConfig);

    TopicTypeModel newTopicTypeModel(String uri, String value, String dataTypeUri);

    TopicTypeModel newTopicTypeModel(JSONObject topicType);



    // === AssocTypeModel ===

    AssocTypeModel newAssocTypeModel(TopicModel typeTopic, String dataTypeUri, List<CompDefModel> compDefs,
                                     ViewConfigurationModel viewConfig);

    AssocTypeModel newAssocTypeModel(String uri, String value, String dataTypeUri);

    AssocTypeModel newAssocTypeModel(JSONObject assocType);



    // === CompDefModel ===

    CompDefModel newCompDefModel(String parentTypeUri, String childTypeUri, String childCardinalityUri);

    CompDefModel newCompDefModel(String parentTypeUri, String childTypeUri, String childCardinalityUri,
                                 ViewConfigurationModel viewConfig);

    CompDefModel newCompDefModel(String customAssocTypeUri, boolean isIdentityAttr, boolean includeInLabel,
                                 String parentTypeUri, String childTypeUri, String childCardinalityUri);

    CompDefModel newCompDefModel(AssocModel assoc, ViewConfigurationModel viewConfig);

    CompDefModel newCompDefModel(JSONObject compDef);



    // === ViewConfigurationModel ===

    ViewConfigurationModel newViewConfigurationModel();

    ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics);

    ViewConfigurationModel newViewConfigurationModel(JSONArray configTopics);



    // === Topicmaps ===

    ViewTopic newViewTopic(TopicModel topic, ViewProps viewProps);

    ViewAssoc newViewAssoc(AssocModel assoc, ViewProps viewProps);

    ViewProps newViewProps();

    ViewProps newViewProps(int x, int y);

    ViewProps newViewProps(int x, int y, boolean visibility, boolean pinned);

    ViewProps newViewProps(boolean visibility);

    ViewProps newViewProps(boolean visibility, boolean pinned);

    ViewProps newViewProps(JSONObject viewProps);



    // === Facets ===

    FacetValueModel newFacetValueModel(String childTypeUri);    // TODO: rename param to "compDefUri"?

    FacetValueModel newFacetValueModel(JSONObject facetValue);
}
