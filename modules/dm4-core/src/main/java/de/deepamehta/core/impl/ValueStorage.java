package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.JavaUtils;

import java.util.Iterator;
import java.util.List;



class ValueStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_SEPARATOR = " ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueStorage(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Fetches the composite value (child topic models) of the specified object model and stores it within the model.
     */
    void fetchCompositeValue(DeepaMehtaObjectModel model) {
        try {
            Type type = getType(model);
            if (!type.getDataTypeUri().equals("dm4.core.composite")) {
                return;
            }
            //
            ChildTopicsModel comp = model.getChildTopicsModel();
            for (AssociationDefinition assocDef : type.getAssocDefs()) {
                String cardinalityUri = assocDef.getPartCardinalityUri();
                String childTypeUri   = assocDef.getPartTypeUri();
                if (cardinalityUri.equals("dm4.core.one")) {
                    TopicModel childTopic = fetchChildTopic(model.getId(), assocDef);
                    if (childTopic != null) {
                        comp.put(childTypeUri, childTopic);
                        fetchCompositeValue(childTopic);    // recursive call
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    for (TopicModel childTopic : fetchChildTopics(model.getId(), assocDef)) {
                        comp.add(childTypeUri, childTopic);
                        fetchCompositeValue(childTopic);    // recursive call
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching composite value of object " + model.getId() + " failed (" +
                model + ")", e);
        }
    }

    /**
     * Stores and indexes the specified model's value, either a simple value or a composite value (child topics).
     * Depending on the model type's data type dispatches either to storeSimpleValue() or to storeCompositeValue().
     */
    void storeValue(DeepaMehtaObjectModel model, ClientState clientState, Directives directives) {
        if (getType(model).getDataTypeUri().equals("dm4.core.composite")) {
            storeCompositeValue(model, clientState, directives);
            refreshLabel(model);
        } else {
            storeSimpleValue(model);
        }
    }

    // ---

    /**
     * Prerequisite: this is a composite object.
     */
    void refreshLabel(DeepaMehtaObjectModel model) {
        try {
            String label = buildLabel(model);
            setSimpleValue(model, new SimpleValue(label));
        } catch (Exception e) {
            throw new RuntimeException("Refreshing label of object " + model.getId() + " failed (" + model + ")", e);
        }
    }

    void setSimpleValue(DeepaMehtaObjectModel model, SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        // update memory
        model.setSimpleValue(value);
        // update DB
        storeSimpleValue(model);
    }



    // === Helper ===

    void associateChildTopic(long childTopicId, DeepaMehtaObjectModel parent, AssociationDefinition assocDef,
                                                                              ClientState clientState) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.whole"),
            new TopicRoleModel(childTopicId, "dm4.core.part"), clientState
        );
    }

    void associateChildTopic(String childTopicUri, DeepaMehtaObjectModel parent, AssociationDefinition assocDef,
                                                                                 ClientState clientState) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.whole"),
            new TopicRoleModel(childTopicUri, "dm4.core.part"), clientState
        );
    }

    Topic associateChildTopic(TopicModel childModel, DeepaMehtaObjectModel parent, AssociationDefinition assocDef,
                                                                                   ClientState clientState) {
        if (isReferenceById(childModel)) {
            associateChildTopic(childModel.getId(), parent, assocDef, clientState);
            return dms.getTopic(childModel.getId(), false, null);       // fetchComposite=false, clientState=null
        } else if (isReferenceByUri(childModel)) {
            associateChildTopic(childModel.getUri(), parent, assocDef, clientState);
            return dms.getTopic("uri", new SimpleValue(childModel.getUri()), false, null);
        } else {
            throw new RuntimeException("Topic model is not a reference (" + childModel + ")");
        }
    }

    // ---

    /**
     * Checks weather an update topic model represents a reference.
     */
    boolean isReference(TopicModel childTopic) {
        return isReferenceById(childTopic) || isReferenceByUri(childTopic);
    }

    boolean isReferenceById(TopicModel childTopic) {
        return childTopic.getId() != -1;
    }

    boolean isReferenceByUri(TopicModel childTopic) {
        return !childTopic.getUri().equals("");     // ### FIXME: in an update topic model the URI might be null
    }

    // ---

    Type getType(DeepaMehtaObjectModel model) {
        if (model instanceof TopicModel) {
            return dms.getTopicType(model.getTypeUri(), null);
        } else if (model instanceof AssociationModel) {
            return dms.getAssociationType(model.getTypeUri(), null);
        }
        throw new RuntimeException("Unexpected model: " + model);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Stores and indexes the simple value of the specified topic or association model.
     * Determines the index key and index modes.
     */
    private void storeSimpleValue(DeepaMehtaObjectModel model) {
        Type type = getType(model);
        if (model instanceof TopicModel) {
            dms.storage.storeTopicValue(
                model.getId(),
                model.getSimpleValue(),
                type.getIndexModes(),
                type.getUri(),
                getIndexValue(model)
            );
        } else if (model instanceof AssociationModel) {
            dms.storage.storeAssociationValue(
                model.getId(),
                model.getSimpleValue(),
                type.getIndexModes(),
                type.getUri(),
                getIndexValue(model)
            );
        }
    }

    private void storeCompositeValue(DeepaMehtaObjectModel parent, ClientState clientState, Directives directives) {
        ChildTopicsModel model = null;
        try {
            model = parent.getChildTopicsModel();
            for (AssociationDefinition assocDef : getType(parent).getAssocDefs()) {
                String childTypeUri   = assocDef.getPartTypeUri();
                String cardinalityUri = assocDef.getPartCardinalityUri();
                TopicModel childTopic        = null;     // only used for "one"
                List<TopicModel> childTopics = null;     // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    childTopic = model.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in create request
                    if (childTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    childTopics = model.getTopics(childTypeUri, null);      // defaultValue=null
                    // skip if not contained in create request
                    if (childTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                storeChildTopics(childTopic, childTopics, parent, assocDef, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing composite value of object " + parent.getId() + " failed (" +
                model + ")", e);
        }
    }

    // ---

    private void storeChildTopics(TopicModel childTopic, List<TopicModel> childTopics, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = childTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                storeCompositionOne(childTopic, parent, assocDef, clientState, directives);
            } else {
                storeCompositionMany(childTopics, parent, assocDef, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                storeAggregationOne(childTopic, parent, assocDef, clientState, directives);
            } else {
                storeAggregationMany(childTopics, parent, assocDef, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // --- Composition ---

    private void storeCompositionOne(TopicModel model, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        // == create child ==
        // update DB
        Topic childTopic = dms.createTopic(model, clientState);
        associateChildTopic(childTopic.getId(), parent, assocDef, clientState);
        // update memory
        // ### putInCompositeModel(assocDef, childTopic);   // ### TODO?
    }

    private void storeCompositionMany(List<TopicModel> models, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        for (TopicModel model : models) {
            // == create child ==
            // update DB
            Topic childTopic = dms.createTopic(model, clientState);
            associateChildTopic(childTopic.getId(), parent, assocDef, clientState);
            // update memory
            // ### addToCompositeModel(assocDef, childTopic);   // ### TODO?
        }
    }

    // --- Aggregation ---

    private void storeAggregationOne(TopicModel model, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        if (isReference(model)) {
            // == create assignment ==
            // update DB
            Topic topic = associateChildTopic(model, parent, assocDef, clientState);
            // update memory
            // ### putInCompositeModel(assocDef, topic);    // ### TODO?
        } else {
            // == create child ==
            // update DB
            Topic topic = dms.createTopic(model, clientState);
            associateChildTopic(topic.getId(), parent, assocDef, clientState);
            // update memory
            // ### putInCompositeModel(assocDef, topic);    // ### TODO?
        }
    }

    private void storeAggregationMany(List<TopicModel> models, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        for (TopicModel model : models) {
            if (isReference(model)) {
                // == create assignment ==
                // update DB
                Topic topic = associateChildTopic(model, parent, assocDef, clientState);
                // update memory
                // ### addToCompositeModel(assocDef, topic);    // ### TODO?
            } else {
                // == create child ==
                // update DB
                Topic topic = dms.createTopic(model, clientState);
                associateChildTopic(topic.getId(), parent, assocDef, clientState);
                // update memory
                // ### addToCompositeModel(assocDef, topic);    // ### TODO?
            }
        }
    }



    // === Label ===

    private String buildLabel(DeepaMehtaObjectModel model) {
        Type type = getType(model);
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            List<String> labelConfig = type.getLabelConfig();
            if (labelConfig.size() > 0) {
                return buildLabelFromConfig(labelConfig, model);
            } else {
                return buildDefaultLabel(model);
            }
        } else {
            return model.getSimpleValue().toString();
        }
    }

    /**
     * Builds the specified object model's label according to a label configuration.
     */
    private String buildLabelFromConfig(List<String> labelConfig, DeepaMehtaObjectModel model) {
        StringBuilder label = new StringBuilder();
        for (String childTypeUri : labelConfig) {
            TopicModel childTopic = model.getChildTopicsModel().getTopic(childTypeUri, null);
            // Note: topics just created have no child topics yet
            if (childTopic != null) {
                String l = buildLabel(childTopic);
                // add separator
                if (label.length() > 0 && l.length() > 0) {
                    label.append(LABEL_SEPARATOR);
                }
                //
                label.append(l);
            }
        }
        return label.toString();
    }

    private String buildDefaultLabel(DeepaMehtaObjectModel model) {
        Iterator<AssociationDefinition> i = getType(model).getAssocDefs().iterator();
        // Note: types just created might have no child types yet
        if (i.hasNext()) {
            String childTypeUri = i.next().getPartTypeUri();
            TopicModel childTopic = model.getChildTopicsModel().getTopic(childTypeUri, null);
            // Note: topics just created have no child topics yet
            if (childTopic != null) {
                return buildLabel(childTopic);
            }
        }
        return "";
    }



    // === Helper ===

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopicModel fetchChildTopic(long id, AssociationDefinition assocDef) {
        String assocTypeUri  = assocDef.getInstanceLevelAssocTypeUri();
        String othersTypeUri = assocDef.getPartTypeUri();
        return dms.storage.fetchRelatedTopic(id, assocTypeUri, "dm4.core.whole", "dm4.core.part", othersTypeUri);
    }

    private ResultSet<RelatedTopicModel> fetchChildTopics(long id, AssociationDefinition assocDef) {
        String assocTypeUri  = assocDef.getInstanceLevelAssocTypeUri();
        String othersTypeUri = assocDef.getPartTypeUri();
        return dms.storage.fetchRelatedTopics(id, assocTypeUri, "dm4.core.whole", "dm4.core.part", othersTypeUri);
    }

    // ---

    /**
     * Calculates the simple value that is to be indexed for this object.
     *
     * HTML tags are stripped from HTML values. Non-HTML values are returned directly.
     */
    private SimpleValue getIndexValue(DeepaMehtaObjectModel model) {
        SimpleValue value = model.getSimpleValue();
        if (getType(model).getDataTypeUri().equals("dm4.core.html")) {
            return new SimpleValue(JavaUtils.stripHTML(value.toString()));
        } else {
            return value;
        }
    }
}
