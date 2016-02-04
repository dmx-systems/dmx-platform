package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ModelFactory;

import org.codehaus.jettison.json.JSONObject;



class ModelFactoryImpl implements ModelFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private StorageDecorator storageDecorator;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ModelFactoryImpl(StorageDecorator storageDecorator) {
        this.storageDecorator = storageDecorator;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === DeepaMehtaObjectModel ===

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                         ChildTopicsModel childTopics) {
        if (childTopics == null) {
            childTopics = new ChildTopicsModelImpl();
        }
        return new DeepaMehtaObjectModelImpl(id, uri, typeUri, value, childTopics);
    }

    /* @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(DeepaMehtaObjectModel object) {
        return newDeepaMehtaObjectModel(object.getId(), object.getUri(), object.getTypeUri(), object.getSimpleValue(),
            object.getChildTopicsModel());
    } */

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(JSONObject object) {
        try {
            return newDeepaMehtaObjectModel(
                object.optLong("id", -1),
                object.optString("uri", null),
                object.optString("type_uri", null),
                object.has("value") ? new SimpleValue(object.get("value")) : null,
                object.has("childs") ? new ChildTopicsModel(object.getJSONObject("childs")) : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + object + ")", e);
        }
    }

    /* @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(ChildTopicsModel childTopics) {
        return newDeepaMehtaObjectModel(null, childTopics);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri) {
        return newDeepaMehtaObjectModel(-1, typeUri);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri, SimpleValue value) {
        return newDeepaMehtaObjectModel(null, typeUri, value);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String typeUri, ChildTopicsModel childTopics) {
        return newDeepaMehtaObjectModel(null, typeUri, childTopics);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri) {
        return newDeepaMehtaObjectModel(-1, uri, typeUri, null, null);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri, SimpleValue value) {
        return newDeepaMehtaObjectModel(-1, uri, typeUri, value, null);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        return newDeepaMehtaObjectModel(-1, uri, typeUri, null, childTopics);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id) {
        return newDeepaMehtaObjectModel(id, null, null);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, ChildTopicsModel childTopics) {
        return newDeepaMehtaObjectModel(id, null, childTopics);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String typeUri) {
        return newDeepaMehtaObjectModel(id, typeUri, null);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(long id, String typeUri, ChildTopicsModel childTopics) {
        return newDeepaMehtaObjectModel(id, null, typeUri, null, childTopics);
    } */



    // === ChildTopicsModel ===



    // === TopicModel ===

    @Override
    public TopicModel newTopicModel(long id, String uri, String typeUri, SimpleValue value,
                                                                         ChildTopicsModel childTopics) {
        return new TopicModelImpl(newDeepaMehtaObjectModel(id, uri, typeUri, value, childTopics));
    }

    @Override
    public TopicModel newTopicModel(JSONObject topic) {
        return new TopicModelImpl(newDeepaMehtaObjectModel(topic));
    }

    /* TopicModelImpl(ChildTopicsModel childTopics) {
        super(childTopics);
    }

    TopicModelImpl(String typeUri) {
        super(typeUri);
    }

    TopicModelImpl(String typeUri, SimpleValue value) {
        super(typeUri, value);
    }

    TopicModelImpl(String typeUri, ChildTopicsModel childTopics) {
        super(typeUri, childTopics);
    }

    TopicModelImpl(String uri, String typeUri) {
        super(uri, typeUri);
    }

    TopicModelImpl(String uri, String typeUri, SimpleValue value) {
        super(uri, typeUri, value);
    }

    TopicModelImpl(String uri, String typeUri, ChildTopicsModel childTopics) {
        super(uri, typeUri, childTopics);
    }

    TopicModelImpl(long id) {
        super(id);
    }

    TopicModelImpl(long id, String typeUri) {
        super(id, typeUri);
    }

    TopicModelImpl(long id, ChildTopicsModel childTopics) {
        super(id, childTopics);
    } */



    // === AssociationModel ===

    @Override
    public AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics) {
        return new AssociationModelImpl(newDeepaMehtaObjectModel(id, uri, typeUri, value, childTopics), roleModel1,
            roleModel2);
    }

    /* @Override
    public AssociationModel newAssociationModel(AssociationModel assoc) {
        super(assoc);
        this.roleModel1 = assoc.getRoleModel1();
        this.roleModel2 = assoc.getRoleModel2();
    } */

    @Override
    public AssociationModel newAssociationModel(JSONObject assoc) {
        try {
            return new AssociationModelImpl(newDeepaMehtaObjectModel(assoc),
                assoc.has("role_1") ? parseRole(assoc.getJSONObject("role_1")) : null,
                assoc.has("role_2") ? parseRole(assoc.getJSONObject("role_2")) : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationModel failed (JSONObject=" + assoc + ")", e);
        }
    }

    @Override
    public AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return new AssociationModelImpl(typeUri, roleModel1, roleModel2, null);
    }

    @Override
    public AssociationModel newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                                      ChildTopicsModel childTopics) {
        return new AssociationModelImpl(-1, null, typeUri, roleModel1, roleModel2, null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    @Override
    public AssociationModel newAssociationModel() {
        return new AssociationModelImpl(-1, null, null, null, null);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    @Override
    public AssociationModel newAssociationModel(ChildTopicsModel childTopics) {
        return new AssociationModelImpl(null, null, null, childTopics);
    }

    @Override
    public AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                                     RoleModel roleModel2) {
        return new AssociationModelImpl(id, uri, typeUri, roleModel1, roleModel2, null, null);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RoleModel parseRole(JSONObject roleModel) {
        if (roleModel.has("topic_id") || roleModel.has("topic_uri")) {
            return new TopicRoleModel(roleModel);
        } else if (roleModel.has("assoc_id")) {
            return new AssociationRoleModel(roleModel);
        } else {
            throw new RuntimeException("Parsing TopicRoleModel/AssociationRoleModel failed " +
                "(JSONObject=" + roleModel + ")");
        }
    }
}
