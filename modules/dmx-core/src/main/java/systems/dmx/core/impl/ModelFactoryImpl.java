package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicDeletionModel;
import systems.dmx.core.model.TopicReferenceModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.model.facets.FacetValueModel;
import systems.dmx.core.model.topicmaps.AssociationViewModel;
import systems.dmx.core.model.topicmaps.TopicViewModel;
import systems.dmx.core.model.topicmaps.ViewProperties;
import systems.dmx.core.service.ModelFactory;

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

    private static final String TYPE_COMP_DEF = "dmx.core.composition_def";

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
        return new TopicModelImpl(newDMXObjectModel(id, uri, typeUri, value, childTopics));
    }

    // TODO: needed?
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
            return new TopicModelImpl(newDMXObjectModel(topic));
        } catch (Exception e) {
            throw parsingFailed(topic, e, "TopicModelImpl");
        }
    }



    // === AssociationModel ===

    @Override
    public AssociationModelImpl newAssociationModel(long id, String uri, String typeUri, RoleModel roleModel1,
                                                RoleModel roleModel2, SimpleValue value, ChildTopicsModel childTopics) {
        return new AssociationModelImpl(newDMXObjectModel(id, uri, typeUri, value, childTopics),
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
    // ### TODO: make internal?
    @Override
    public AssociationModelImpl newAssociationModel() {
        return newAssociationModel(-1, null, null, null, null, null, null);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    // ### TODO: make internal?
    @Override
    public AssociationModelImpl newAssociationModel(ChildTopicsModel childTopics) {
        return newAssociationModel(null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    // ### TODO: make internal?
    @Override
    public AssociationModelImpl newAssociationModel(String typeUri, ChildTopicsModel childTopics) {
        return newAssociationModel(typeUri, null, null, childTopics);
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
            return new AssociationModelImpl(newDMXObjectModel(assoc),
                assoc.has("role1") ? parseRole(assoc.getJSONObject("role1")) : null,
                assoc.has("role2") ? parseRole(assoc.getJSONObject("role2")) : null
            );
        } catch (Exception e) {
            throw parsingFailed(assoc, e, "AssociationModelImpl");
        }
    }

    // ---

    private RoleModelImpl parseRole(JSONObject roleModel) {
        try {
            if (roleModel.has("topicId") || roleModel.has("topicUri")) {
                return newTopicRoleModel(roleModel);
            } else if (roleModel.has("assocId")) {
                return newAssociationRoleModel(roleModel);
            } else {
                throw new RuntimeException("One of \"topicId\"/\"topicUri\"/\"assocId\" is expected");
            }
        } catch (Exception e) {
            throw parsingFailed(roleModel, e, "RoleModelImpl");
        }
    }



    // === DMXObjectModel ===

    /**
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update operation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    DMXObjectModelImpl newDMXObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                         ChildTopicsModel childTopics) {
        return new DMXObjectModelImpl(id, uri, typeUri, value, (ChildTopicsModelImpl) childTopics, pl());
    }

    DMXObjectModelImpl newDMXObjectModel(JSONObject object) throws JSONException {
        return newDMXObjectModel(
            object.optLong("id", -1),
            object.optString("uri", null),
            object.optString("typeUri", null),
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
            throw parsingFailed(values, e, "ChildTopicsModelImpl");
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
                        // for updating multi-refs the original ID must be preserved
                        if (topicRef instanceof TopicReferenceModelImpl && val.has("id")) {
                            ((TopicReferenceModelImpl) topicRef).originalId = val.getLong("id");
                        }
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
        if (!value.has("typeUri")) {
            value.put("typeUri", childTypeUri);
        } else {
            // sanity check
            String typeUri = value.getString("typeUri");
            if (!typeUri.equals(childTypeUri)) {
                throw new IllegalArgumentException("A \"" + childTypeUri + "\" topic model has typeUri=\"" + typeUri +
                    "\"");
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
            long topicId       = topicRoleModel.optLong("topicId", -1);
            String topicUri    = topicRoleModel.optString("topicUri", null);
            String roleTypeUri = topicRoleModel.getString("roleTypeUri");
            //
            if (topicId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topicId\" nor \"topicUri\" is set");
            }
            if (topicId != -1 && topicUri != null) {
                throw new IllegalArgumentException("\"topicId\" and \"topicUri\" must not be set at the same time");
            }
            //
            if (topicId != -1) {
                return newTopicRoleModel(topicId, roleTypeUri);
            } else {
                return newTopicRoleModel(topicUri, roleTypeUri);
            }
        } catch (Exception e) {
            throw parsingFailed(topicRoleModel, e, "TopicRoleModelImpl");
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
            long assocId       = assocRoleModel.getLong("assocId");
            String roleTypeUri = assocRoleModel.getString("roleTypeUri");
            return newAssociationRoleModel(assocId, roleTypeUri);
        } catch (Exception e) {
            throw parsingFailed(assocRoleModel, e, "AssociationRoleModelImpl");
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
    public RelatedAssociationModelImpl newRelatedAssociationModel(AssociationModel assoc,
                                                                  AssociationModel relatingAssoc) {
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

    @Override
    public TopicReferenceModel newTopicReferenceModel(Object topicIdOrUri) {
        RelatedTopicModelImpl relTopic;
        if (topicIdOrUri instanceof Long) {
            relTopic = newRelatedTopicModel((Long) topicIdOrUri);
        } else if (topicIdOrUri instanceof String) {
            relTopic = newRelatedTopicModel((String) topicIdOrUri);
        } else {
            throw new IllegalArgumentException("Tried to build a TopicReferenceModel from a " +
                topicIdOrUri.getClass().getName() + " (expected are String or Long)");
        }
        return new TopicReferenceModelImpl(relTopic);
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
                                                List<AssociationDefinitionModel> assocDefs,
                                                ViewConfigurationModel viewConfig) {
        return new TopicTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, assocDefs,
            (ViewConfigurationModelImpl) viewConfig));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(String uri, String value, String dataTypeUri) {
        return new TopicTypeModelImpl(newTypeModel(uri, "dmx.core.topic_type", new SimpleValue(value), dataTypeUri));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(JSONObject topicType) {
        try {
            return new TopicTypeModelImpl(newTypeModel(topicType.put("typeUri", "dmx.core.topic_type")));
        } catch (Exception e) {
            throw parsingFailed(topicType, e, "TopicTypeModelImpl");
        }
    }



    // === AssociationTypeModel ===

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(TopicModel typeTopic, String dataTypeUri,
                                                            List<AssociationDefinitionModel> assocDefs,
                                                            ViewConfigurationModel viewConfig) {
        return new AssociationTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, assocDefs,
            (ViewConfigurationModelImpl) viewConfig));
    }

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(String uri, String value, String dataTypeUri) {
        return new AssociationTypeModelImpl(newTypeModel(uri, "dmx.core.assoc_type", new SimpleValue(value),
            dataTypeUri));
    }

    @Override
    public AssociationTypeModelImpl newAssociationTypeModel(JSONObject assocType) {
        try {
            return new AssociationTypeModelImpl(newTypeModel(assocType.put("typeUri", "dmx.core.assoc_type")));
        } catch (Exception e) {
            throw parsingFailed(assocType, e, "AssociationTypeModelImpl");
        }
    }



    // === TypeModel ===

    TypeModelImpl newTypeModel(TopicModel typeTopic, String dataTypeUri, List<AssociationDefinitionModel> assocDefs,
                                                                         ViewConfigurationModelImpl viewConfig) {
        return new TypeModelImpl((TopicModelImpl) typeTopic, dataTypeUri, assocDefs, viewConfig);
    }

    TypeModelImpl newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri) {
        return new TypeModelImpl(newTopicModel(uri, typeUri, value), dataTypeUri, new ArrayList(),
            newViewConfigurationModel());
    }

    TypeModelImpl newTypeModel(JSONObject typeModel) throws JSONException {
        TopicModelImpl typeTopic = newTopicModel(typeModel);
        return new TypeModelImpl(typeTopic,
            typeModel.optString("dataTypeUri", null),
            parseAssocDefs(typeModel.optJSONArray("assocDefs"), typeTopic.getUri()),    // optJSONArray may return null
            newViewConfigurationModel(typeModel.optJSONArray("viewConfigTopics")));     // optJSONArray may return null
    }

    // ---

    private List<AssociationDefinitionModel> parseAssocDefs(JSONArray assocDefs, String parentTypeUri) throws
                                                                                                       JSONException {
        List<AssociationDefinitionModel> _assocDefs = new ArrayList();
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                JSONObject assocDef = assocDefs.getJSONObject(i)
                    .put("parentTypeUri", parentTypeUri);
                _assocDefs.add(newAssociationDefinitionModel(assocDef));
            }
        }
        return _assocDefs;
    }



    // === AssociationDefinitionModel ===

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(
                                                    String parentTypeUri, String childTypeUri,
                                                    String childCardinalityUri) {
        return newAssociationDefinitionModel(-1, null, null, false, false, parentTypeUri, childTypeUri,
            childCardinalityUri, null);
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(
                                                    String parentTypeUri, String childTypeUri,
                                                    String childCardinalityUri,
                                                    ViewConfigurationModel viewConfig) {
        return newAssociationDefinitionModel(-1, null, null, false, false, parentTypeUri, childTypeUri,
            childCardinalityUri, viewConfig);
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(
                                                    String customAssocTypeUri,
                                                    boolean isIdentityAttr, boolean includeInLabel,
                                                    String parentTypeUri, String childTypeUri,
                                                    String childCardinalityUri) {
        return newAssociationDefinitionModel(-1, null, customAssocTypeUri, isIdentityAttr, includeInLabel,
            parentTypeUri, childTypeUri, childCardinalityUri, null);
    }

    /**
     * @param   assoc   the underlying association.
     *                  IMPORTANT: the association must identify its players <i>by URI</i> (not by ID). ### still true?
     */
    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(AssociationModel assoc,
                                                                        ViewConfigurationModel viewConfig) {
        return new AssociationDefinitionModelImpl((AssociationModelImpl) assoc,
            (ViewConfigurationModelImpl) viewConfig);
    }

    @Override
    public AssociationDefinitionModelImpl newAssociationDefinitionModel(JSONObject assocDef) {
        try {
            return new AssociationDefinitionModelImpl(
                newAssociationModel(assocDef.optLong("id", -1), null,
                    TYPE_COMP_DEF,
                    parentRole(assocDef.getString("parentTypeUri")),
                    childRole(assocDef.getString("childTypeUri")),
                    null, childTopics(assocDef)
                ),
                newViewConfigurationModel(assocDef.optJSONArray("viewConfigTopics"))
            );
        } catch (Exception e) {
            throw parsingFailed(assocDef, e, "AssociationDefinitionModelImpl");
        }
    }

    /**
     * Internal.
     */
    AssociationDefinitionModelImpl newAssociationDefinitionModel(long id, String uri, String customAssocTypeUri,
                                                                 boolean isIdentityAttr, boolean includeInLabel,
                                                                 String parentTypeUri, String childTypeUri,
                                                                 String childCardinalityUri,
                                                                 ViewConfigurationModel viewConfig) {
        return new AssociationDefinitionModelImpl(
            newAssociationModel(id, uri, TYPE_COMP_DEF, parentRole(parentTypeUri), childRole(childTypeUri),
                null, childTopics(childCardinalityUri, customAssocTypeUri, isIdentityAttr, includeInLabel) // value=null
            ),
            (ViewConfigurationModelImpl) viewConfig
        );
    }

    /**
     * Internal.
     */
    AssociationDefinitionModelImpl newAssociationDefinitionModel(ChildTopicsModel childTopics) {
        return new AssociationDefinitionModelImpl(newAssociationModel(TYPE_COMP_DEF, childTopics));
    }

    // ---

    private TopicRoleModel parentRole(String parentTypeUri) {
        return newTopicRoleModel(parentTypeUri, "dmx.core.parent_type");
    }

    private TopicRoleModel childRole(String childTypeUri) {
        return newTopicRoleModel(childTypeUri, "dmx.core.child_type");
    }

    // ---

    private ChildTopicsModel childTopics(JSONObject assocDef) throws JSONException {
        return childTopics(
            assocDef.getString("childCardinalityUri"),
            // Note: getString()/optString() on a key with JSON null value would return the string "null"
            assocDef.isNull("customAssocTypeUri") ? null : assocDef.getString("customAssocTypeUri"),
            assocDef.optBoolean("isIdentityAttr"),
            assocDef.optBoolean("includeInLabel")
        );
    }

    private ChildTopicsModel childTopics(String cardinalityUri, String customAssocTypeUri, boolean isIdentityAttr,
                                         boolean includeInLabel) {
        ChildTopicsModel childTopics = newChildTopicsModel()
            .putRef("dmx.core.cardinality", cardinalityUri)
            .put("dmx.core.identity_attr", isIdentityAttr)
            .put("dmx.core.include_in_label", includeInLabel);
        //
        if (customAssocTypeUri != null) {
            if (customAssocTypeUri.startsWith(DEL_URI_PREFIX)) {
                childTopics.putDeletionRef("dmx.core.assoc_type#dmx.core.custom_assoc_type",
                    delTopicUri(customAssocTypeUri));
            } else {
                childTopics.putRef("dmx.core.assoc_type#dmx.core.custom_assoc_type", customAssocTypeUri);
            }
        }
        //
        return childTopics;
    }



    // === ViewConfigurationModel ===

    @Override
    public ViewConfigurationModelImpl newViewConfigurationModel() {
        return new ViewConfigurationModelImpl(new HashMap(), pl());
    }    

    @Override
    public ViewConfigurationModelImpl newViewConfigurationModel(Iterable<? extends TopicModel> configTopics) {
        Map<String, TopicModelImpl> _configTopics = new HashMap();
        for (TopicModel configTopic : configTopics) {
            _configTopics.put(configTopic.getTypeUri(), (TopicModelImpl) configTopic);
        }
        return new ViewConfigurationModelImpl(_configTopics, pl());
    }    

    /**
     * @param   configTopics    may be null
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
            return new ViewConfigurationModelImpl(_configTopics, pl());
        } catch (Exception e) {
            throw parsingFailed(configTopics, e, "ViewConfigurationModelImpl");
        }
    }    



    // === Topicmaps ===

    @Override
    public TopicViewModel newTopicViewModel(TopicModel topic, ViewProperties viewProps) {
        return new TopicViewModelImpl((TopicModelImpl) topic, viewProps);
    }

    @Override
    public AssociationViewModel newAssociationViewModel(AssociationModel assoc, ViewProperties viewProps) {
        return new AssociationViewModelImpl((AssociationModelImpl) assoc, viewProps);
    }

    @Override
    public ViewProperties newViewProperties() {
        return new ViewPropertiesImpl();
    }

    @Override
    public ViewProperties newViewProperties(int x, int y, boolean visibility, boolean pinned) {
        return new ViewPropertiesImpl(x, y, visibility, pinned);
    }

    @Override
    public ViewProperties newViewProperties(int x, int y) {
        return new ViewPropertiesImpl(x, y);
    }

    @Override
    public ViewProperties newViewProperties(boolean visibility) {
        return new ViewPropertiesImpl(visibility);
    }

    @Override
    public ViewProperties newViewProperties(JSONObject viewProps) {
        return new ViewPropertiesImpl(viewProps);
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
            throw parsingFailed(facetValue, e, "FacetValueModelImpl");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RuntimeException parsingFailed(JSONObject o, Exception e, String className) {
        try {
            return new RuntimeException("JSON parsing failed, " + className + " " + o.toString(4), e);
        } catch (JSONException je) {
            // fallback: no prettyprinting
            return new RuntimeException("JSON parsing failed, " + className + " " + o, e);
        }
    }

    private RuntimeException parsingFailed(JSONArray a, Exception e, String className) {
        try {
            return new RuntimeException("JSON parsing failed, " + className + " " + a.toString(4), e);
        } catch (JSONException je) {
            // fallback: no prettyprinting
            return new RuntimeException("JSON parsing failed, " + className + " " + a, e);
        }
    }

    // ---

    private PersistenceLayer pl() {
        if (pl == null) {
            throw new RuntimeException("Before using the ModelFactory a PersistenceLayer must be set");
        }
        return pl;
    }
}
