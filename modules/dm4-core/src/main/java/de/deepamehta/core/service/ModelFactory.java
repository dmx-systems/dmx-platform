package de.deepamehta.core.service;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



public interface ModelFactory {



    // === DeepaMehtaObjectModel ===

    /**
     * Canonic.
     *
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update operation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                        ChildTopicsModel childTopics);
    /**
     * Copy.
     */
    // DeepaMehtaObjectModel newDeepaMehtaObjectModel(DeepaMehtaObjectModel object);

    /**
     * JSON.
     */
    DeepaMehtaObjectModel newDeepaMehtaObjectModel(JSONObject object);

    /* DeepaMehtaObjectModel newDeepaMehtaObjectModel(ChildTopicsModel childTopics);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri, SimpleValue value);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri, ChildTopicsModel childTopics);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri, SimpleValue value);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri, ChildTopicsModel childTopics);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, ChildTopicsModel childTopics);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String typeUri);

    DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String typeUri, ChildTopicsModel childTopics); */



    // === ChildTopicsModel ===

    ChildTopicsModel newChildTopicsModel();

    ChildTopicsModel newChildTopicsModel(JSONObject values);



    // === TopicModel ===

    TopicModel newTopicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics);

    TopicModel newTopicModel(JSONObject topic);

    // ---

    TopicModel newTopicModel(String uri, String typeUri, SimpleValue value);


    // === AssociationModel ===

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                         RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(JSONObject assoc);

    AssociationModel newAssociationModel(AssociationModel assoc);

    // ---

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                               ChildTopicsModel childTopics);

    AssociationModel newAssociationModel();

    AssociationModel newAssociationModel(ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                              RoleModel roleModel2);


    // === TopicRoleModel ===
    
    TopicRoleModel newTopicRoleModel(long topicId, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(String topicUri, String roleTypeUri);

    TopicRoleModel newTopicRoleModel(JSONObject topicRoleModel);



    // === AssociationRoleModel ===

    AssociationRoleModel newAssociationRoleModel(long assocId, String roleTypeUri);

    AssociationRoleModel newAssociationRoleModel(JSONObject assocRoleModel);



    // === RoleModel ===

    RoleModel createRoleModel(DeepaMehtaObjectModel object, String roleTypeUri);



    // === TypeModel ===

    TypeModel newTypeModel(TopicModel typeTopic, String dataTypeUri,
                           List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                           List<String> labelConfig, ViewConfigurationModel viewConfig);

    TypeModel newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri);

    TypeModel newTypeModel(JSONObject typeModel);



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

    AssociationDefinitionModel newAssociationDefinitionModel(long id, String uri, String assocTypeUri,
                                                             String customAssocTypeUri,
                                                             String parentTypeUri, String childTypeUri,
                                                             String parentCardinalityUri, String childCardinalityUri,
                                                             ViewConfigurationModel viewConfigModel);

    AssociationDefinitionModel newAssociationDefinitionModel(JSONObject assocDef);



    // === ViewConfigurationModel ===

    ViewConfigurationModel newViewConfigurationModel();

    ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics);

    ViewConfigurationModel newViewConfigurationModel(JSONObject configurable);
}
