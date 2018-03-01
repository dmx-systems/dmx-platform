package de.deepamehta.core.service;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.model.facets.FacetValueModel;
import de.deepamehta.core.model.topicmaps.AssociationViewModel;
import de.deepamehta.core.model.topicmaps.TopicViewModel;
import de.deepamehta.core.model.topicmaps.ViewProperties;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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
                                     List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                     ViewConfigurationModel viewConfig);

    TopicTypeModel newTopicTypeModel(String uri, String value, String dataTypeUri);

    TopicTypeModel newTopicTypeModel(JSONObject topicType);



    // === AssociationTypeModel ===

    AssociationTypeModel newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                 List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                                 ViewConfigurationModel viewConfig);

    AssociationTypeModel newAssociationTypeModel(String uri, String value, String dataTypeUri);

    AssociationTypeModel newAssociationTypeModel(JSONObject assocType);



    // === AssociationDefinitionModel ===

    AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri,
                                                             String parentTypeUri, String childTypeUri,
                                                             String parentCardinalityUri, String childCardinalityUri);

    AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri,
                                                             String parentTypeUri, String childTypeUri,
                                                             String parentCardinalityUri, String childCardinalityUri,
                                                             ViewConfigurationModel viewConfig);

    AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri,
                                                             String customAssocTypeUri,
                                                             boolean isIdentityAttr, boolean includeInLabel,
                                                             String parentTypeUri, String childTypeUri,
                                                             String parentCardinalityUri, String childCardinalityUri);

    AssociationDefinitionModel newAssociationDefinitionModel(AssociationModel assoc,
                                                             String parentCardinalityUri, String childCardinalityUri,
                                                             ViewConfigurationModel viewConfig);

    AssociationDefinitionModel newAssociationDefinitionModel(JSONObject assocDef);



    // === ViewConfigurationModel ===

    ViewConfigurationModel newViewConfigurationModel();

    ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics);

    ViewConfigurationModel newViewConfigurationModel(JSONArray configTopics);



    // === Topicmaps ===

    TopicViewModel newTopicViewModel(TopicModel topic, ViewProperties viewProps);

    AssociationViewModel newAssociationViewModel(AssociationModel assoc, ViewProperties viewProps);



    // === Facets ===

    FacetValueModel newFacetValueModel(String childTypeUri);

    FacetValueModel newFacetValueModel(JSONObject facetValue);
}
