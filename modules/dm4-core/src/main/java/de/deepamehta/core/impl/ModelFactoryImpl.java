package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ModelFactory;

import org.codehaus.jettison.json.JSONException;
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

    @Override
    public ChildTopicsModel newChildTopicsModel() {
        return new ChildTopicsModelImpl();
    }

    @Override
    public ChildTopicsModel newChildTopicsModel(JSONObject values) {
        try {
            Map<String, Object> childTopics = new HashMap();
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String assocDefUri = i.next();
                String childTypeUri = childTypeUri(assocDefUri);
                Object value = values.get(assocDefUri);
                if (!(value instanceof JSONArray)) {
                    put(assocDefUri, createTopicModel(childTypeUri, value));                    // ###
                } else {
                    JSONArray valueArray = (JSONArray) value;
                    for (int j = 0; j < valueArray.length(); j++) {
                        add(assocDefUri, createTopicModel(childTypeUri, valueArray.get(j)));    // ###
                    }
                }
            }
            return new ChildTopicsModelImpl(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Parsing ChildTopicsModel failed (JSONObject=" + values + ")", e);
        }
    }

    // ---

    /**
     * Creates a topic model from a JSON value.
     *
     * Both topic serialization formats are supported:
     * 1) canonic format -- contains entire topic models.
     * 2) simplified format -- contains the topic value only (simple or composite).
     */
    private RelatedTopicModel createTopicModel(String childTypeUri, Object value) throws JSONException {
        if (value instanceof JSONObject) {
            JSONObject val = (JSONObject) value;
            // we detect the canonic format by checking for mandatory topic properties
            if (val.has("value") || val.has("childs")) {
                // canonic format (topic or topic reference)
                AssociationModel relatingAssoc = null;
                if (val.has("assoc")) {
                    relatingAssoc = new AssociationModel(val.getJSONObject("assoc"));
                }
                if (val.has("value")) {
                    RelatedTopicModel topicRef = createReferenceModel(val.get("value"), relatingAssoc);
                    if (topicRef != null) {
                        return topicRef;
                    }
                }
                //
                initTypeUri(val, childTypeUri);
                //
                TopicModel topic = new TopicModel(val);
                if (relatingAssoc != null) {
                    return new RelatedTopicModel(topic, relatingAssoc);
                } else {
                    return new RelatedTopicModel(topic);
                }
            } else {
                // simplified format (composite topic)
                return new RelatedTopicModel(new TopicModel(childTypeUri, new ChildTopicsModel(val)));
            }
        } else {
            // simplified format (simple topic or topic reference)
            RelatedTopicModel topicRef = createReferenceModel(value, null);
            if (topicRef != null) {
                return topicRef;
            }
            // simplified format (simple topic)
            return new RelatedTopicModel(new TopicModel(childTypeUri, new SimpleValue(value)));
        }
    }

    private RelatedTopicModel createReferenceModel(Object value, AssociationModel relatingAssoc) {
        if (value instanceof String) {
            String val = (String) value;
            if (val.startsWith(REF_ID_PREFIX)) {
                long topicId = refTopicId(val);
                if (relatingAssoc != null) {
                    return new TopicReferenceModel(topicId, relatingAssoc);
                } else {
                    return new TopicReferenceModel(topicId);
                }
            } else if (val.startsWith(REF_URI_PREFIX)) {
                String topicUri = refTopicUri(val);
                if (relatingAssoc != null) {
                    return new TopicReferenceModel(topicUri, relatingAssoc);
                } else {
                    return new TopicReferenceModel(topicUri);
                }
            } else if (val.startsWith(DEL_ID_PREFIX)) {
                return new TopicDeletionModel(delTopicId(val));
            } else if (val.startsWith(DEL_URI_PREFIX)) {
                return new TopicDeletionModel(delTopicUri(val));
            }
        }
        return null;
    }

    private void initTypeUri(JSONObject value, String childTypeUri) throws JSONException {
        if (!value.has("type_uri")) {
            value.put("type_uri", childTypeUri);
        } else {
            // sanity check
            String typeUri = value.getString("type_uri");
            if (!typeUri.equals(childTypeUri)) {
                throw new IllegalArgumentException("A \"" + childTypeUri + "\" topic model has type_uri=\"" +
                    typeUri + "\"");
            }
        }
    }

    // ---

    private long refTopicId(String val) {
        return Long.parseLong(val.substring(REF_ID_PREFIX.length()));
    }

    private String refTopicUri(String val) {
        return val.substring(REF_URI_PREFIX.length());
    }

    private long delTopicId(String val) {
        return Long.parseLong(val.substring(DEL_ID_PREFIX.length()));
    }

    private String delTopicUri(String val) {
        return val.substring(DEL_URI_PREFIX.length());
    }



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

    // ---

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



    // === TopicRoleModel ===

    @Override
    public TopicRoleModel newTopicRoleModel(long topicId, String roleTypeUri) {
        return new TopicRoleModelImpl(topicId, roleTypeUri);
    }

    @Override
    public TopicRoleModel newTopicRoleModel(String topicUri, String roleTypeUri) {
        return new TopicRoleModelImpl(topicUri, roleTypeUri);
    }

    @Override
    public TopicRoleModel newTopicRoleModel(JSONObject topicRoleModel) {
        try {
            long topicId       = topicRoleModel.optLong("topic_id", -1);
            String topicUri    = topicRoleModel.optString("topic_uri", null);
            String roleTypeUri = topicRoleModel.getString("role_type_uri");
            //
            if (topicId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topic_id\" nor \"topic_uri\" is set");
            }
            if (topicId != -1 && topicUri != null) {
                throw new IllegalArgumentException("\"topic_id\" and \"topic_uri\" must not be set at the same time");
            }
            //
            if (topicId != -1) {
                return newTopicRoleModel(topicId, roleTypeUri);
            } else {
                return newTopicRoleModel(topicUri, roleTypeUri);
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicRoleModel failed (JSONObject=" + topicRoleModel + ")", e);
        }
    }



    // === AssociationRoleModel ===

    @Override
    public AssociationRoleModel newAssociationRoleModel(long assocId, String roleTypeUri) {
        return new TopicRoleModelImpl(assocId, roleTypeUri);
    }    

    @Override
    public AssociationRoleModel newAssociationRoleModel(JSONObject assocRoleModel) {
        try {
            long assocId       = assocRoleModel.getLong("assoc_id");
            String roleTypeUri = assocRoleModel.getString("role_type_uri");
            return newAssociationRoleModel(assocId, roleTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationRoleModel failed (JSONObject=" + assocRoleModel + ")", e);
        }
    }    



    // === RoleModel ===

    @Override
    public RoleModel createRoleModel(DeepaMehtaObjectModel object, String roleTypeUri) {
        // Note: can't be located in model classes as models can't create models.
        if (object instanceof TopicModel) {
            return newTopicRoleModel(object.getId(), roleTypeUri);
        } else if (object instanceof AssociationModel) {
            return newAssociationRoleModel(object.getId(), roleTypeUri);
        } else {
            throw new RuntimeException("Unexpected model object: " + object);
        }
    }
}
