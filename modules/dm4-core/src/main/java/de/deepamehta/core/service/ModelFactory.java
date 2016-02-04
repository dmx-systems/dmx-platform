package de.deepamehta.core.service;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



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



    // === TopicModel ===

    TopicModel newTopicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics);

    TopicModel newTopicModel(JSONObject topic);



    // === AssociationModel ===

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                         RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(JSONObject assoc);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2);

    AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                               ChildTopicsModel childTopics);

    AssociationModel newAssociationModel();

    AssociationModel newAssociationModel(ChildTopicsModel childTopics);

    AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                              RoleModel roleModel2);

}
