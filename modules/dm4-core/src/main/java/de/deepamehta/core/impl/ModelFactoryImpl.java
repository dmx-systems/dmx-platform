package de.deepamehta.core.impl;

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
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



class ModelFactoryImpl implements ModelFactory {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX  = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_ID_PREFIX  = "del_id:";
    private static final String DEL_URI_PREFIX = "del_uri:";

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
            childTopics = newChildTopicsModel();
        }
        return new DeepaMehtaObjectModelImpl(id, uri, typeUri, value, childTopics);
    }

    @Override
    public DeepaMehtaObjectModel newDeepaMehtaObjectModel(JSONObject object) {
        try {
            return newDeepaMehtaObjectModel(
                object.optLong("id", -1),
                object.optString("uri", null),
                object.optString("type_uri", null),
                object.has("value") ? new SimpleValue(object.get("value")) : null,
                object.has("childs") ? newChildTopicsModel(object.getJSONObject("childs")) : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + object + ")", e);
        }
    }



    // === ChildTopicsModel ===

    @Override
    public ChildTopicsModel newChildTopicsModel() {
        return new ChildTopicsModelImpl(new HashMap());
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
                    relatingAssoc = newAssociationModel(val.getJSONObject("assoc"));
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
                TopicModel topic = newTopicModel(val);
                if (relatingAssoc != null) {
                    return newRelatedTopicModel(topic, relatingAssoc);
                } else {
                    return newRelatedTopicModel(topic);
                }
            } else {
                // simplified format (composite topic)
                return newRelatedTopicModel(newTopicModel(childTypeUri, newChildTopicsModel(val)));
            }
        } else {
            // simplified format (simple topic or topic reference)
            RelatedTopicModel topicRef = createReferenceModel(value, null);
            if (topicRef != null) {
                return topicRef;
            }
            // simplified format (simple topic)
            return newRelatedTopicModel(newTopicModel(childTypeUri, new SimpleValue(value)));
        }
    }

    private RelatedTopicModel createReferenceModel(Object value, AssociationModel relatingAssoc) {
        if (value instanceof String) {
            String val = (String) value;
            if (val.startsWith(REF_ID_PREFIX)) {
                long topicId = refTopicId(val);
                if (relatingAssoc != null) {
                    return newTopicReferenceModel(topicId, relatingAssoc);
                } else {
                    return newTopicReferenceModel(topicId);
                }
            } else if (val.startsWith(REF_URI_PREFIX)) {
                String topicUri = refTopicUri(val);
                if (relatingAssoc != null) {
                    return newTopicReferenceModel(topicUri, relatingAssoc);
                } else {
                    return newTopicReferenceModel(topicUri);
                }
            } else if (val.startsWith(DEL_ID_PREFIX)) {
                return newTopicDeletionModel(delTopicId(val));
            } else if (val.startsWith(DEL_URI_PREFIX)) {
                return newTopicDeletionModel(delTopicUri(val));
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
    public TopicModel newTopicModel(ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, null, null, childTopics);
    }

    @Override
    public TopicModel newTopicModel(String typeUri) {
        return newTopicModel(-1, null, typeUri, null, null);
    }

    @Override
    public TopicModel newTopicModel(String typeUri, SimpleValue value) {
        return newTopicModel(-1, null, typeUri, value, null);
    }

    @Override
    public TopicModel newTopicModel(String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, typeUri, null, childTopics);
    }

    @Override
    public TopicModel newTopicModel(String uri, String typeUri) {
        return newTopicModel(-1, uri, typeUri, null, null);
    }

    @Override
    public TopicModel newTopicModel(String uri, String typeUri, SimpleValue value) {
        return newTopicModel(-1, uri, typeUri, value, null);
    }

    @Override
    public TopicModel newTopicModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, uri, typeUri, null, childTopics);
    }

    @Override
    public TopicModel newTopicModel(long id) {
        return newTopicModel(id, null, null, null, null);
    }

    /* TopicModelImpl(long id, String typeUri) {
        super(id, typeUri);
    }

    TopicModelImpl(long id, ChildTopicsModel childTopics) {
        super(id, childTopics);
    } */

    @Override
    public TopicModel newTopicModel(JSONObject topic) {
        return new TopicModelImpl(newDeepaMehtaObjectModel(topic));
    }



    // === AssociationModel ===

    @Override
    public AssociationModel newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics) {
        return new AssociationModelImpl(newDeepaMehtaObjectModel(id, uri, typeUri, value, childTopics), roleModel1,
            roleModel2);
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

    @Override
    public AssociationModel newAssociationModel(AssociationModel assoc) {
        return new AssociationModelImpl(assoc);
    }

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



    // === RelatedTopicModel ===

    @Override
    public RelatedTopicModel newRelatedTopicModel(long topicId) {
        return new RelatedTopicModelImpl(newTopicModel(topicId), newAssociationModel());
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(long topicId, AssociationModel relatingAssoc) {
        // ### FIXME: assoc's childTopics might be null, call newDeepaMehtaObjectModel()
        return new RelatedTopicModelImpl(newTopicModel(topicId), relatingAssoc);
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(String topicUri) {
        return new RelatedTopicModelImpl(newTopicModel(topicUri, null), newAssociationModel()); // topicTypeUri=null
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(String topicUri, AssociationModel relatingAssoc) {
        // ### FIXME: assoc's childTopics might be null, call newDeepaMehtaObjectModel()
        return new RelatedTopicModelImpl(newTopicModel(topicUri, null), relatingAssoc);         // topicTypeUri=null
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(String topicTypeUri, SimpleValue value) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, value), newAssociationModel());
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, childTopics), newAssociationModel());
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(TopicModel topic) {
        // ### FIXME: topic's childTopics might be null, call newDeepaMehtaObjectModel()
        return new RelatedTopicModelImpl(topic, newAssociationModel());
    }

    @Override
    public RelatedTopicModel newRelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc) {
        // ### FIXME: childTopics might be null, call newDeepaMehtaObjectModel()
        return new RelatedTopicModelImpl(topic, relatingAssoc);
    }



    // === TopicReferenceModel ===

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicId));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId, AssociationModel relatingAssoc) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicId, relatingAssoc));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicUri));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri, AssociationModel relatingAssoc) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicUri, relatingAssoc));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics) {
        return new TopicReferenceModelImpl(
            newRelatedTopicModel(topicId, newAssociationModel(relatingAssocChildTopics))
        );
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics) {
        return new TopicReferenceModelImpl(
            newRelatedTopicModel(topicUri, newAssociationModel(relatingAssocChildTopics))
        );
    }



    // === TopicDeletionModel ===

    @Override
    public TopicDeletionModel newTopicDeletionModel(long topicId) {
        return new TopicDeletionModelImpl(newRelatedTopicModel(topicId));
    }

    @Override
    public TopicDeletionModel newTopicDeletionModel(String topicUri) {
        return new TopicDeletionModelImpl(newRelatedTopicModel(topicUri));
    }



    // === TypeModel ===

    @Override
    public TypeModel newTypeModel(TopicModel typeTopic, String dataTypeUri, List<IndexMode> indexModes,
                                  List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                                  ViewConfigurationModel viewConfig) {
        SequencedHashMap<String, AssociationDefinitionModel> _assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            addAssocDef(assocDef);      // ###
        }
        return new TypeModelImpl(typeTopic, dataTypeUri, indexModes, _assocDefs, labelConfig, viewConfig);
    }

    // ### internal?
    @Override
    public TypeModel newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri) {
        return new TypeModelImpl(
            newTopicModel(uri, typeUri, value), dataTypeUri,
            new ArrayList(), new SequencedHashMap(), new ArrayList(), newViewConfigurationModel()
        );
    }

    @Override
    public TypeModel newTypeModel(JSONObject typeModel) {
        try {
            return new TypeModelImpl(
                newTopicModel(typeModel), typeModel.getString("data_type_uri"),
                IndexMode.parse(typeModel), parseAssocDefs(typeModel), parseLabelConfig(typeModel),
                newViewConfigurationModel(typeModel)
            );
        } catch (Exception e) {
            throw new RuntimeException("Parsing TypeModel failed (JSONObject=" + typeModel + ")", e);
        }
    }

    // ---

    private List<String> parseLabelConfig(JSONObject typeModel) throws JSONException {
        if (typeModel.has("label_config")) {
            return DeepaMehtaUtils.toList(typeModel.getJSONArray("label_config"));
        }
        return new ArrayList();
    }

    private SequencedHashMap<String, AssociationDefinitionModel> parseAssocDefs(JSONObject typeModel)
                                                                                                  throws JSONException {
        SequencedHashMap<String, AssociationDefinitionModel> assocDefs = new SequencedHashMap();
        JSONArray _assocDefs = typeModel.optJSONArray("assoc_defs");
        if (_assocDefs != null) {
            for (int i = 0; i < _assocDefs.length(); i++) {
                JSONObject assocDef = _assocDefs.getJSONObject(i);
                assocDef.put("parent_type_uri", this.uri);              // ###
                addAssocDef(newAssociationDefinitionModel(assocDef));   // ###
            }
        }
        return assocDefs;
    }



    // === TopicTypeModel ===

    @Override
    public TopicTypeModel newTopicTypeModel(TopicModel typeTopic, String dataTypeUri,
                                            List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                            List<String> labelConfig, ViewConfigurationModel viewConfig) {
        return new TopicTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            viewConfig));
    }

    @Override
    public TopicTypeModel newTopicTypeModel(String uri, String value, String dataTypeUri) {
        return new TopicTypeModelImpl(newTypeModel(uri, "dm4.core.topic_type", new SimpleValue(value), dataTypeUri));
    }

    @Override
    public TopicTypeModel newTopicTypeModel(JSONObject topicType) {
        return new TopicTypeModelImpl(newTypeModel(topicType.put("type_uri", "dm4.core.topic_type")));
    }



    // === AssociationTypeModel ===

    @Override
    public AssociationTypeModel newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                 List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                                 List<String> labelConfig, ViewConfigurationModel viewConfig) {
        return new AssociationTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            viewConfig));
    }

    @Override
    public AssociationTypeModel newAssociationTypeModel(String uri, String value, String dataTypeUri) {
        return new AssociationTypeModelImpl(newTypeModel(uri, "dm4.core.assoc_type", new SimpleValue(value),
            dataTypeUri));
    }

    @Override
    public AssociationTypeModel newAssociationTypeModel(JSONObject assocType) {
        return new AssociationTypeModelImpl(newTypeModel(assocType.put("type_uri", "dm4.core.assoc_type")));
    }



    // === AssociationDefinitionModel ===

    @Override
    public AssociationDefinitionModel newAssociationDefinitionModel(
                                                    long id, String uri, String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri,
                                                    ViewConfigurationModel viewConfigModel) {
        AssociationModel assoc = newAssociationModel(id, uri, assocTypeUri, parentRole(parentTypeUri),
            childRole(childTypeUri), null, childTopics(customAssocTypeUri));
        if (viewConfigModel == null) {
            viewConfigModel = new ViewConfigurationModel();
        }
        return new AssociationDefinitionModelImpl(assoc, parentCardinalityUri, childCardinalityUri, viewConfigModel);
    }

    @Override
    public AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri) {
        newAssociationDefinitionModel(-1, null, assocTypeUri, null, parentTypeUri, childTypeUri,
            parentCardinalityUri, childCardinalityUri, null);
    }

    @Override
    public AssociationDefinitionModel newAssociationDefinitionModel(String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri) {
        newAssociationDefinitionModel(-1, null, assocTypeUri, customAssocTypeUri, parentTypeUri, childTypeUri,
            parentCardinalityUri, childCardinalityUri, null);
    }

    /**
     * @param   assoc   the underlying association.
     *                  IMPORTANT: the association must identify its players <i>by URI</i> (not by ID). ### still true?
     */
    @Override
    public AssociationDefinitionModel newAssociationDefinitionModel(AssociationModel assoc, String parentCardinalityUri,
                                                   String childCardinalityUri, ViewConfigurationModel viewConfigModel) {
        // ### FIXME: may assoc's childTopics uninitialized? may viewConfigModel null?
        // ### Should we call the canonic newAssociationDefinitionModel()?
        return new AssociationDefinitionModelImpl(assoc, parentCardinalityUri, childCardinalityUri, viewConfigModel);
    }

    /**
     * Note: the AssociationDefinitionModel constructed by this constructor remains partially uninitialized,
     * which is OK for an update assoc def operation. It can not be used for a create operation.
     *
    AssociationDefinitionModelImpl(AssociationModel assoc) {
        // ### FIXME: the assoc must identify its players **by URI**
        super(assoc);
    } */

    @Override
    public AssociationDefinitionModel newAssociationDefinitionModel(JSONObject assocDef) {
        try {
            if (!assocDef.has("parent_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"parent_cardinality_uri\" is missing");
            }
            AssociationModel assoc = newAssociationModel(assocDef.optLong("id", -1), null,
                assocDef.getString("assoc_type_uri"), parentRole(assocDef), childRole(assocDef), null,
                childTopics(assocDef));
            String parentCardinalityUri = assocDef.optString("parent_cardinality_uri", "dm4.core.one");
            String childCardinalityUri  = assocDef.getString("child_cardinality_uri");
            //
            return new AssociationDefinitionModelImpl(assoc, parentCardinalityUri, childCardinalityUri,
                new ViewConfigurationModel(assocDef));
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // ---

    private static TopicRoleModel parentRole(JSONObject assocDef) throws JSONException {
        return parentRole(assocDef.getString("parent_type_uri"));
    }

    private static TopicRoleModel parentRole(String parentTypeUri) {
        return new TopicRoleModel(parentTypeUri, "dm4.core.parent_type");
    }

    // ---

    private static TopicRoleModel childRole(JSONObject assocDef) throws JSONException {
        return childRole(assocDef.getString("child_type_uri"));
    }

    private static TopicRoleModel childRole(String childTypeUri) {
        return new TopicRoleModel(childTypeUri, "dm4.core.child_type");
    }

    // ---

    private static ChildTopicsModel childTopics(JSONObject assocDef) throws JSONException {
        // Note: getString() called on a key with JSON null value would return the string "null"
        return childTopics(assocDef.isNull("custom_assoc_type_uri") ? null :
            assocDef.getString("custom_assoc_type_uri"));
    }

    private static ChildTopicsModel childTopics(String customAssocTypeUri) {
        if (customAssocTypeUri != null) {
            if (customAssocTypeUri.startsWith(DEL_URI_PREFIX)) {
                return new ChildTopicsModel().putDeletionRef("dm4.core.assoc_type#dm4.core.custom_assoc_type",
                    delTopicUri(customAssocTypeUri));
            } else {
                return new ChildTopicsModel().putRef("dm4.core.assoc_type#dm4.core.custom_assoc_type",
                    customAssocTypeUri);
            }
        } else {
            return null;
        }
    }



    // === ViewConfigurationModel ===

    @Override
    public ViewConfigurationModel newViewConfigurationModel() {
        return new ViewConfigurationModelImpl(new HashMap());
    }    

    @Override
    public ViewConfigurationModel newViewConfigurationModel(Iterable<? extends TopicModel> configTopics) {
        Map<String, TopicModel> viewConfig = new HashMap();
        for (TopicModel topic : configTopics) {
            addConfigTopic(topic);      // ###
        }
        return new ViewConfigurationModelImpl(viewConfig);
    }    

    /**
     * @param   configurable    A topic type, an association type, or an association definition.
     *                          ### TODO: pass sole JSONArray?
     */
    @Override
    public ViewConfigurationModel newViewConfigurationModel(JSONObject configurable) {
        try {
            JSONArray topics = configurable.optJSONArray("view_config_topics");
            if (topics != null) {
                for (int i = 0; i < topics.length(); i++) {
                    addConfigTopic(new TopicModel(topics.getJSONObject(i)));    // ###
                }
            }
            return new ViewConfigurationModelImpl();                            // ###
        } catch (Exception e) {
            throw new RuntimeException("Parsing ViewConfigurationModel failed (JSONObject=" + configurable + ")", e);
        }
    }    
}
