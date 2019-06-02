package systems.dmx.core.service;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationRoleModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedAssociationModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicDeletionModel;
import systems.dmx.core.model.TopicReferenceModel;
import systems.dmx.core.model.TopicRoleModel;
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

    // ### TODO: make internal?
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



    // === AssociationModel ===

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                         RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                               ChildTopicsModel childTopics);

    // ### TODO: Refactoring needed. See comments in impl.
    AssociationModel newAssociationModel();

    // ### TODO: Refactoring needed. See comments in impl.
    AssociationModel newAssociationModel(ChildTopicsModel childTopics);

    // ### TODO: Refactoring needed. See comments in impl.
    AssociationModel newAssociationModel(String typeUri, ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                              RoleModel roleModel2);

    AssociationModel newAssociationModel(AssociationModel assoc);

    AssociationModel newAssociationModel(JSONObject assoc);



    // === ChildTopicsModel ===

    ChildTopicsModel newChildTopicsModel();

    ChildTopicsModel newChildTopicsModel(JSONObject values);

    /**
     * Utility.
     */
    String childTypeUri(String assocDefUri);



    // === TopicRoleModel ===
    
    TopicRoleModel newTopicRoleModel(long topicId, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(String topicUri, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(long topicId, String topicUri, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(JSONObject topicRoleModel);



    // === AssociationRoleModel ===

    AssociationRoleModel newAssociationRoleModel(long assocId, String roleTypeUri);

    AssociationRoleModel newAssociationRoleModel(JSONObject assocRoleModel);



    // === RelatedTopicModel ===

    RelatedTopicModel newRelatedTopicModel(long topicId);

    RelatedTopicModel newRelatedTopicModel(long topicId, AssociationModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicUri);

    RelatedTopicModel newRelatedTopicModel(String topicUri, AssociationModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, SimpleValue value);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc);



    // === RelatedAssociationModel ===

    RelatedAssociationModel newRelatedAssociationModel(AssociationModel assoc, AssociationModel relatingAssoc);



    // === TopicReferenceModel ===

    // TODO: make internal?

    TopicReferenceModel newTopicReferenceModel(long topicId);

    TopicReferenceModel newTopicReferenceModel(long topicId, AssociationModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(String topicUri);

    TopicReferenceModel newTopicReferenceModel(String topicUri, AssociationModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics);

    TopicReferenceModel newTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics);

    TopicReferenceModel newTopicReferenceModel(Object topicIdOrUri);



    // === TopicDeletionModel ===

    // TODO: make internal?

    TopicDeletionModel newTopicDeletionModel(long topicId);

    TopicDeletionModel newTopicDeletionModel(String topicUri);



    // === TopicTypeModel ===

    TopicTypeModel newTopicTypeModel(TopicModel typeTopic, String dataTypeUri,
                                     List<CompDefModel> assocDefs,
                                     ViewConfigurationModel viewConfig);

    TopicTypeModel newTopicTypeModel(String uri, String value, String dataTypeUri);

    TopicTypeModel newTopicTypeModel(JSONObject topicType);



    // === AssociationTypeModel ===

    AssociationTypeModel newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                 List<CompDefModel> assocDefs,
                                                 ViewConfigurationModel viewConfig);

    AssociationTypeModel newAssociationTypeModel(String uri, String value, String dataTypeUri);

    AssociationTypeModel newAssociationTypeModel(JSONObject assocType);



    // === CompDefModel ===

    CompDefModel newCompDefModel(String parentTypeUri, String childTypeUri, String childCardinalityUri);

    CompDefModel newCompDefModel(String parentTypeUri, String childTypeUri, String childCardinalityUri,
                                 ViewConfigurationModel viewConfig);

    CompDefModel newCompDefModel(String customAssocTypeUri, boolean isIdentityAttr, boolean includeInLabel,
                                 String parentTypeUri, String childTypeUri, String childCardinalityUri);

    CompDefModel newCompDefModel(AssociationModel assoc, ViewConfigurationModel viewConfig);

    CompDefModel newCompDefModel(JSONObject assocDef);



    // === ViewConfigurationModel ===

    ViewConfigurationModel newViewConfigurationModel();

    ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics);

    ViewConfigurationModel newViewConfigurationModel(JSONArray configTopics);



    // === Topicmaps ===

    ViewTopic newViewTopic(TopicModel topic, ViewProps viewProps);

    ViewAssoc newViewAssoc(AssociationModel assoc, ViewProps viewProps);

    ViewProps newViewProps();

    ViewProps newViewProps(int x, int y);

    ViewProps newViewProps(int x, int y, boolean visibility, boolean pinned);

    ViewProps newViewProps(boolean visibility);

    ViewProps newViewProps(boolean visibility, boolean pinned);

    ViewProps newViewProps(JSONObject viewProps);



    // === Facets ===

    FacetValueModel newFacetValueModel(String childTypeUri);    // TODO: rename param to "assocDefUri"?

    FacetValueModel newFacetValueModel(JSONObject facetValue);
}
