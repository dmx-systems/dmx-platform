package de.deepamehta.core.impl;

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
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



// ### TODO: should methods return model *impl* objects? -> Yes!
public class ModelFactoryImpl implements ModelFactory {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX  = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_ID_PREFIX  = "del_id:";
    private static final String DEL_URI_PREFIX = "del_uri:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    PersistenceLayer pl;

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicModel ===

    @Override
    public TopicModelImpl newTopicModel(long id, String uri, String typeUri, SimpleValue value,
                                                                         ChildTopicsModel childTopics) {
        return new TopicModelImpl(newDeepaMehtaObjectModel(id, uri, typeUri, value, childTopics));
    }

    @Override
    public TopicModelImpl newTopicModel(ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, null, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri) {
        return newTopicModel(-1, null, typeUri, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri, SimpleValue value) {
        return newTopicModel(-1, null, typeUri, value, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, typeUri, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri) {
        return newTopicModel(-1, uri, typeUri, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri, SimpleValue value) {
        return newTopicModel(-1, uri, typeUri, value, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, uri, typeUri, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(long id) {
        return newTopicModel(id, null, null, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(long id, ChildTopicsModel childTopics) {
        return newTopicModel(id, null, null, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(TopicModel topic) {
        return new TopicModelImpl((TopicModelImpl) topic);
    }

    @Override
    public TopicModelImpl newTopicModel(JSONObject topic) {
        try {
            return new TopicModelImpl(newDeepaMehtaObjectModel(topic));
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicModel failed (JSONObject=" + topic + ")", e);
        }
    }



    // === AssociationModel ===

    @Override
    public AssociationModelImpl newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics) {
        return new AssociationModelImpl(newDeepaMehtaObjectModel(id, uri, typeUri, value, childTopics),
            (RoleModelImpl) roleModel1, (RoleModelImpl) roleModel2);
    }

    @Override
    public AssociationModelImpl newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return newAssociationModel(-1, null, typeUri, roleModel1, roleModel2, null, null);
    }

    @Override
    public AssociationModelImpl newAssociationModel(String typeUri, RoleModel roleModel1, RoleModel roleModel2,
                                                                                      ChildTopicsModel childTopics) {
        return newAssociationModel(-1, null, typeUri, roleModel1, roleModel2, null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    @Override
    public AssociationModelImpl newAssociationModel() {
        return newAssociationModel(-1, null, null, null, null, null, null);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    @Override
    public AssociationModelImpl newAssociationModel(ChildTopicsModel childTopics) {
        return newAssociationModel(-1, null, null, null, null, null, childTopics);
    }

    @Override
    public AssociationModelImpl newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                                                     RoleModel roleModel2) {
        return newAssociationModel(id, uri, typeUri, roleModel1, roleModel2, null, null);
    }

    @Override
    public AssociationModelImpl newAssociationModel(AssociationModel assoc) {
        return new AssociationModelImpl((AssociationModelImpl) assoc);
    }

    @Override
    public AssociationModelImpl newAssociationModel(JSONObject assoc) {
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

    private RoleModelImpl parseRole(JSONObject roleModel) {
        if (roleModel.has("topic_id") || roleModel.has("topic_uri")) {
            return newTopicRoleModel(roleModel);
        } else if (roleModel.has("assoc_id")) {
            return newAssociationRoleModel(roleModel);
        } else {
            throw new RuntimeException("Parsing TopicRoleModel/AssociationRoleModel failed (JSONObject=" +
                roleModel + ")");
        }
    }



    // === DeepaMehtaObjectModel ===

    /**
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update operation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    DeepaMehtaObjectModelImpl newDeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                         ChildTopicsModel childTopics) {
        return new DeepaMehtaObjectModelImpl(id, uri, typeUri, value, (ChildTopicsModelImpl) childTopics, pl());
    }

    DeepaMehtaObjectModelImpl newDeepaMehtaObjectModel(JSONObject object) throws JSONException {
        return newDeepaMehtaObjectModel(
            object.optLong("id", -1),
            object.optString("uri", null),
            object.optString("type_uri", null),
            object.has("value") ? new SimpleValue(object.get("value")) : null,
            object.has("childs") ? newChildTopicsModel(object.getJSONObject("childs")) : null
        );
    }



    // === ChildTopicsModel ===

    @Override
    public ChildTopicsModelImpl newChildTopicsModel() {
        return new ChildTopicsModelImpl(new HashMap(), this);
    }

    @Override
    public ChildTopicsModelImpl newChildTopicsModel(JSONObject values) {
        try {
            Map<String, Object> childTopics = new HashMap();
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String assocDefUri = i.next();
                String childTypeUri = childTypeUri(assocDefUri);
                Object value = values.get(assocDefUri);
                if (!(value instanceof JSONArray)) {
                    childTopics.put(assocDefUri, createTopicModel(childTypeUri, value));
                } else {
                    JSONArray valueArray = (JSONArray) value;
                    List<RelatedTopicModel> topics = new ArrayList();
                    childTopics.put(assocDefUri, topics);
                    for (int j = 0; j < valueArray.length(); j++) {
                        topics.add(createTopicModel(childTypeUri, valueArray.get(j)));
                    }
                }
            }
            return new ChildTopicsModelImpl(childTopics, this);
        } catch (Exception e) {
            throw new RuntimeException("Parsing ChildTopicsModel failed (JSONObject=" + values + ")", e);
        }
    }

    @Override
    public String childTypeUri(String assocDefUri) {
        return assocDefUri.split("#")[0];
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



    // === TopicRoleModel ===

    @Override
    public TopicRoleModelImpl newTopicRoleModel(long topicId, String roleTypeUri) {
        return new TopicRoleModelImpl(topicId, roleTypeUri, pl());
    }

    @Override
    public TopicRoleModelImpl newTopicRoleModel(String topicUri, String roleTypeUri) {
        return new TopicRoleModelImpl(topicUri, roleTypeUri, pl());
    }

    @Override
    public TopicRoleModelImpl newTopicRoleModel(JSONObject topicRoleModel) {
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
    public AssociationRoleModelImpl newAssociationRoleModel(long assocId, String roleTypeUri) {
        return new AssociationRoleModelImpl(assocId, roleTypeUri, pl());
    }    

    @Override
    public AssociationRoleModelImpl newAssociationRoleModel(JSONObject assocRoleModel) {
        try {
            long assocId       = assocRoleModel.getLong("assoc_id");
            String roleTypeUri = assocRoleModel.getString("role_type_uri");
            return newAssociationRoleModel(assocId, roleTypeUri);
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationRoleModel failed (JSONObject=" + assocRoleModel + ")", e);
        }
    }    



    // === RelatedTopicModel ===

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(long topicId) {
        return new RelatedTopicModelImpl(newTopicModel(topicId), newAssociationModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(long topicId, AssociationModel relatingAssoc) {
        return new RelatedTopicModelImpl(newTopicModel(topicId), (AssociationModelImpl) relatingAssoc);
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicUri) {
        return new RelatedTopicModelImpl(newTopicModel(topicUri, (String) null), newAssociationModel());
                                                          // topicTypeUri=null
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicUri, AssociationModel relatingAssoc) {
        return new RelatedTopicModelImpl(newTopicModel(topicUri, (String) null), (AssociationModelImpl) relatingAssoc);
                                                          // topicTypeUri=null
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicTypeUri, SimpleValue value) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, value), newAssociationModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, childTopics), newAssociationModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(TopicModel topic) {
        return new RelatedTopicModelImpl((TopicModelImpl) topic, newAssociationModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(TopicModel topic, AssociationModel relatingAssoc) {
        return new RelatedTopicModelImpl((TopicModelImpl) topic, (AssociationModelImpl) relatingAssoc);
    }



    // === RelatedAssociationModel ===

    @Override
    public RelatedAssociationModel newRelatedAssociationModel(AssociationModel assoc, AssociationModel relatingAssoc) {
        return new RelatedAssociationModelImpl((AssociationModelImpl) assoc, (AssociationModelImpl) relatingAssoc);
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



    // === TopicTypeModel ===

    @Override
    public TopicTypeModelImpl newTopicTypeModel(TopicModel typeTopic, String dataTypeUri,
                                            List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                            List<String> labelConfig, ViewConfigurationModel viewConfig) {
        return new TopicTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            (ViewConfigurationModelImpl) viewConfig));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(String uri, String value, String dataTypeUri) {
        return new TopicTypeModelImpl(newTypeModel(uri, "dm4.core.topic_type", new SimpleValue(value), dataTypeUri));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(JSONObject topicType) {
        try {
            return new TopicTypeModelImpl(newTypeModel(topicType.put("type_uri", "dm4.core.topic_type")));
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeModel failed (JSONObject=" + topicType + ")", e);
        }
    }



    // === AssociationTypeModel ===

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                 List<IndexMode> indexModes, List<AssociationDefinitionModel> assocDefs,
                                                 List<String> labelConfig, ViewConfigurationModel viewConfig) {
        return new AssociationTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            (ViewConfigurationModelImpl) viewConfig));
    }

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(String uri, String value, String dataTypeUri) {
        return new AssociationTypeModelImpl(newTypeModel(uri, "dm4.core.assoc_type", new SimpleValue(value),
            dataTypeUri));
    }

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(JSONObject assocType) {
        try {
            return new AssociationTypeModelImpl(newTypeModel(assocType.put("type_uri", "dm4.core.assoc_type")));
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationTypeModel failed (JSONObject=" + assocType + ")", e);
        }
    }



    // === TypeModel ===

    TypeModelImpl newTypeModel(TopicModel typeTopic, String dataTypeUri, List<IndexMode> indexModes,
                                  List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                                  ViewConfigurationModelImpl viewConfig) {
        return new TypeModelImpl((TopicModelImpl) typeTopic, dataTypeUri, indexModes, assocDefs, labelConfig,
            viewConfig);
    }

    TypeModelImpl newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri) {
        return new TypeModelImpl(newTopicModel(uri, typeUri, value), dataTypeUri,
            new ArrayList(), new ArrayList(), new ArrayList(), newViewConfigurationModel()
        );
    }

    TypeModelImpl newTypeModel(JSONObject typeModel) throws JSONException {
        TopicModelImpl typeTopic = newTopicModel(typeModel);
        return new TypeModelImpl(typeTopic,
            typeModel.optString("data_type_uri", null),
            parseIndexModes(typeModel.optJSONArray("index_mode_uris")),
            parseAssocDefs(typeModel.optJSONArray("assoc_defs"), typeTopic.getUri()),
            parseLabelConfig(typeModel.optJSONArray("label_config")),
            newViewConfigurationModel(typeModel.optJSONArray("view_config_topics"))
        );
    }

    // ---

    private List<IndexMode> parseIndexModes(JSONArray indexModeUris) {
        try {
            List<IndexMode> indexModes = new ArrayList();
            if (indexModeUris != null) {
                for (int i = 0; i < indexModeUris.length(); i++) {
                    indexModes.add(IndexMode.fromUri(indexModeUris.getString(i)));
                }
            }
            return indexModes;
        } catch (Exception e) {
            throw new RuntimeException("Parsing index modes failed (JSONArray=" + indexModeUris + ")", e);
        }
    }

    private List<AssociationDefinitionModel> parseAssocDefs(JSONArray assocDefs, String parentTypeUri) throws
                                                                                                       JSONException {
        List<AssociationDefinitionModel> _assocDefs = new ArrayList();
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                JSONObject assocDef = assocDefs.getJSONObject(i)
                    .put("parent_type_uri", parentTypeUri);
                _assocDefs.add(newAssociationDefinitionModel(assocDef));
            }
        }
        return _assocDefs;
    }

    private List<String> parseLabelConfig(JSONArray labelConfig) {
        return labelConfig != null ? DeepaMehtaUtils.toList(labelConfig) : new ArrayList();
    }



    // === AssociationDefinitionModel ===

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(
                                                    long id, String uri, String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri,
                                                    ViewConfigurationModel viewConfig) {
        return new AssociationDefinitionModelImpl(
            newAssociationModel(id, uri, assocTypeUri, parentRole(parentTypeUri), childRole(childTypeUri),
                null, childTopics(customAssocTypeUri)
            ),
            parentCardinalityUri, childCardinalityUri, (ViewConfigurationModelImpl) viewConfig
        );
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(String assocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri) {
        return newAssociationDefinitionModel(-1, null, assocTypeUri, null, parentTypeUri, childTypeUri,
            parentCardinalityUri, childCardinalityUri, null);
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(String assocTypeUri, String customAssocTypeUri,
                                                    String parentTypeUri, String childTypeUri,
                                                    String parentCardinalityUri, String childCardinalityUri) {
        return newAssociationDefinitionModel(-1, null, assocTypeUri, customAssocTypeUri, parentTypeUri, childTypeUri,
            parentCardinalityUri, childCardinalityUri, null);
    }

    /**
     * @param   assoc   the underlying association.
     *                  IMPORTANT: the association must identify its players <i>by URI</i> (not by ID). ### still true?
     */
    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(AssociationModel assoc,
                                                    String parentCardinalityUri, String childCardinalityUri,
                                                    ViewConfigurationModel viewConfig) {
        return new AssociationDefinitionModelImpl((AssociationModelImpl) assoc, parentCardinalityUri,
            childCardinalityUri, (ViewConfigurationModelImpl) viewConfig);
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(JSONObject assocDef) {
        try {
            AssociationModelImpl assoc = newAssociationModel(assocDef.optLong("id", -1), null,
                assocDef.getString("assoc_type_uri"),
                parentRole(assocDef.getString("parent_type_uri")),
                childRole(assocDef.getString("child_type_uri")),
                null, childTopics(assocDef)
            );
            //
            if (!assocDef.has("parent_cardinality_uri") && !assoc.getTypeUri().equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"parent_cardinality_uri\" is missing");
            }
            //
            return new AssociationDefinitionModelImpl(assoc,
                assocDef.optString("parent_cardinality_uri", "dm4.core.one"),
                assocDef.getString("child_cardinality_uri"),
                newViewConfigurationModel(assocDef.optJSONArray("view_config_topics"))
            );
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // ---

    private TopicRoleModel parentRole(String parentTypeUri) {
        return newTopicRoleModel(parentTypeUri, "dm4.core.parent_type");
    }

    private TopicRoleModel childRole(String childTypeUri) {
        return newTopicRoleModel(childTypeUri, "dm4.core.child_type");
    }

    // ---

    private ChildTopicsModel childTopics(JSONObject assocDef) throws JSONException {
        // Note: getString() called on a key with JSON null value would return the string "null"
        return childTopics(assocDef.isNull("custom_assoc_type_uri") ? null :
            assocDef.getString("custom_assoc_type_uri"));
    }

    private ChildTopicsModel childTopics(String customAssocTypeUri) {
        if (customAssocTypeUri != null) {
            if (customAssocTypeUri.startsWith(DEL_URI_PREFIX)) {
                return newChildTopicsModel().putDeletionRef("dm4.core.assoc_type#dm4.core.custom_assoc_type",
                    delTopicUri(customAssocTypeUri));
            } else {
                return newChildTopicsModel().putRef("dm4.core.assoc_type#dm4.core.custom_assoc_type",
                    customAssocTypeUri);
            }
        } else {
            return null;
        }
    }



    // === ViewConfigurationModel ===

    @Override
    public ViewConfigurationModelImpl newViewConfigurationModel() {
        return new ViewConfigurationModelImpl(new HashMap());
    }    

    @Override
    public ViewConfigurationModelImpl newViewConfigurationModel(Iterable<? extends TopicModel> configTopics) {
        Map<String, TopicModelImpl> _configTopics = new HashMap();
        for (TopicModel configTopic : configTopics) {
            _configTopics.put(configTopic.getTypeUri(), (TopicModelImpl) configTopic);
        }
        return new ViewConfigurationModelImpl(_configTopics);
    }    

    /**
     * @param   configurable    A topic type, an association type, or an association definition. ### FIXDOC
     */
    @Override
    public ViewConfigurationModelImpl newViewConfigurationModel(JSONArray configTopics) {
        try {
            Map<String, TopicModelImpl> _configTopics = new HashMap();
            if (configTopics != null) {
                for (int i = 0; i < configTopics.length(); i++) {
                    TopicModelImpl configTopic = newTopicModel(configTopics.getJSONObject(i));
                    _configTopics.put(configTopic.getTypeUri(), configTopic);
                }
            }
            return new ViewConfigurationModelImpl(_configTopics);
        } catch (Exception e) {
            throw new RuntimeException("Parsing ViewConfigurationModel failed (JSONArray=" + configTopics + ")", e);
        }
    }    



    // === Topicmaps ===

    @Override
    public TopicViewModel newTopicViewModel(TopicModel topic, ViewProperties viewProps) {
        return new TopicViewModelImpl((TopicModelImpl) topic, viewProps);
    }

    @Override
    public AssociationViewModel newAssociationViewModel(AssociationModel assoc) {
        return new AssociationViewModelImpl((AssociationModelImpl) assoc);
    }



    // === Facets ===

    @Override
    public FacetValueModel newFacetValueModel(String childTypeUri) {
        return new FacetValueModelImpl(childTypeUri, this);
    }

    @Override
    public FacetValueModel newFacetValueModel(JSONObject facetValue) {
        try {
            ChildTopicsModelImpl childTopics = newChildTopicsModel(facetValue);
            if (childTopics.size() != 1) {
                throw new RuntimeException("There are " + childTopics.size() + " child topic entries (expected is 1)");
            }
            return new FacetValueModelImpl(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Parsing FacetValueModel failed (JSONObject=" + facetValue + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private PersistenceLayer pl() {
        if (pl == null) {
            throw new RuntimeException("before using the ModelFactory a PersistenceLayer must be set");
        }
        return pl;
    }
}
