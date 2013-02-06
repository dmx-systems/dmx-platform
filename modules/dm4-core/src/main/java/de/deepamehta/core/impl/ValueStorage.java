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

import java.util.List;



class ValueStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueStorage(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetchComposite(DeepaMehtaObjectModel model) {
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
                        fetchComposite(childTopic);
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    for (TopicModel childTopic : fetchChildTopics(model.getId(), assocDef)) {
                        comp.add(childTypeUri, childTopic);
                        fetchComposite(childTopic);
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching child topics of object " + model.getId() + " failed (" + model + ")",
                e);
        }
    }

    // ---

    /**
     * Stores and indexes the specified model's value, either a simple value or a composite value (child topics).
     * Depending on the model type's data type dispatches either to storeSimpleValue() or to storeChildTopics().
     */
    void storeValue(DeepaMehtaObjectModel model, ClientState clientState, Directives directives) {
        if (getType(model).getDataTypeUri().equals("dm4.core.composite")) {
            storeChildTopics(model, clientState, directives);
            refreshLabel(model);
        } else {
            storeSimpleValue(model);
        }
    }

    // ---

    /**
     * Stores and indexes the simple value of the specified topic or association model.
     * Determines the index key and index modes.
     */
    void storeSimpleValue(DeepaMehtaObjectModel model) {
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

    private void storeChildTopics(DeepaMehtaObjectModel parent, ClientState clientState, Directives directives) {
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
            throw new RuntimeException("Storing child topics of object " + parent.getId() + " failed (" + model + ")",
                e);
        }
    }

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

    // ### TODO
    private void refreshLabel(DeepaMehtaObjectModel model) {
        // update memory
        model.setSimpleValue("-label-");    // ### TODO
        // update DB
        storeSimpleValue(model);            // ### TODO
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
