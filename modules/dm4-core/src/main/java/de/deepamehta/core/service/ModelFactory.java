package de.deepamehta.core.service;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



public interface ModelFactory {



    // === DeepaMehtaObjectModel ===

    // ### TODO: make internal

    /**
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update operation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                        ChildTopicsModel childTopics);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(JSONObject object) throws JSONException;



    // === ChildTopicsModel ===

    ChildTopicsModel newChildTopicsModel();

    ChildTopicsModel newChildTopicsModel(JSONObject values);

    /**
     * Utility.
     */
    String childTypeUri(String assocDefUri);



    // === TopicModel ===

    TopicModel newTopicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics);

    TopicModel newTopicModel(ChildTopicsModel childTopics);

    TopicModel newTopicModel(String typeUri);

    TopicModel newTopicModel(String typeUri, SimpleValue value);

    TopicModel newTopicModel(String typeUri, ChildTopicsModel childTopics);

    TopicModel newTopicModel(String uri, String typeUri);

    TopicModel newTopicModel(String uri, String typeUri, SimpleValue value);

    TopicModel newTopicModel(String uri, String typeUri, ChildTopicsModel childTopics);

    TopicModel newTopicModel(long id);

    TopicModel newTopicModel(JSONObject topic);



    // === AssociationModel ===

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                         RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                               ChildTopicsModel childTopics);

    AssociationModel newAssociationModel();

    AssociationModel newAssociationModel(ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                              RoleModel roleModel2);

    AssociationModel newAssociationModel(AssociationModel assoc);

    AssociationModel newAssociationModel(JSONObject assoc);



    // === TopicRoleModel ===
    
    TopicRoleModel newTopicRoleModel(long topicId, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(String topicUri, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(JSONObject topicRoleModel);



    // === AssociationRoleModel ===

    AssociationRoleModel newAssociationRoleModel(long assocId, String roleTypeUri);

    AssociationRoleModel newAssociationRoleModel(JSONObject assocRoleModel);



    // === RoleModel ===

    RoleModel createRoleModel(DeepaMehtaObjectModel object, String roleTypeUri);



    // === RelatedTopicModel ===

    RelatedTopicModel newRelatedTopicModel(long topicId);

    RelatedTopicModel newRelatedTopicModel(long topicId, AssociationModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicUri);

    RelatedTopicModel newRelatedTopicModel(String topicUri, AssociationModel relatingAssoc);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, SimpleValue value);

    RelatedTopicModel newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic);

    RelatedTopicModel newRelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc);



    // === TopicReferenceModel ===

    TopicReferenceModel newTopicReferenceModel(long topicId);

    TopicReferenceModel newTopicReferenceModel(long topicId, AssociationModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(String topicUri);

    TopicReferenceModel newTopicReferenceModel(String topicUri, AssociationModel relatingAssoc);

    TopicReferenceModel newTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics);

    TopicReferenceModel newTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics);



    // === TopicDeletionModel ===

    TopicDeletionModel newTopicDeletionModel(long topicId);

    TopicDeletionModel newTopicDeletionModel(String topicUri);



    // === TypeModel ===

    // ### TODO: make internal

    TypeModel newTypeModel(TopicModel typeTopic, String dataTypeUri,
                           List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                           List<String> labelConfig, ViewConfigurationModel viewConfig);

    TypeModel newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri);

    TypeModel newTypeModel(JSONObject typeModel) throws JSONException;



    // === TopicTypeModel ===

    TopicTypeModel newTopicTypeModel(TopicModel typeTopic, String dataTypeUri,
                                     List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                     List<String> labelConfig, ViewConfigurationModel viewConfig);

    TopicTypeModel newTopicTypeModel(String uri, String value, String dataTypeUri);

    TopicTypeModel newTopicTypeModel(JSONObject topicType);



    // === AssociationTypeModel ===

    AssociationTypeModel newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                 List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                                 List<String> labelConfig, ViewConfigurationModel viewConfig);

    AssociationTypeModel newAssociationTypeModel(String uri, String value, String dataTypeUri);

    AssociationTypeModel newAssociationTypeModel(JSONObject assocType);



    // === AssociationDefinitionModel ===

    AssociationDefinitionModel newAssociationDefinitionModel(long id, String uri,
                                                    String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri,
                                                    ViewConfigurationModel viewConfigModel);

    AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri);

    AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri);

    AssociationDefinitionModel newAssociationDefinitionModel(AssociationModel assoc,
                                                    String parentCardinalityUri, String childCardinalityUri,
                                                    ViewConfigurationModel viewConfigModel);

    AssociationDefinitionModel newAssociationDefinitionModel(JSONObject assocDef);



    // === ViewConfigurationModel ===

    ViewConfigurationModel newViewConfigurationModel();

    ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics);

    ViewConfigurationModel newViewConfigurationModel(JSONArray configTopics);
}
