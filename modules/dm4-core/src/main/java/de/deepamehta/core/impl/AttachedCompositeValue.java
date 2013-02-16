package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.CompositeValue;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



class AttachedCompositeValue implements CompositeValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: AttachedTopic or List<AttachedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();

    private CompositeValueModel model;
    private AttachedDeepaMehtaObject parent;

    private EmbeddedService dms;
    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedCompositeValue(CompositeValueModel model, AttachedDeepaMehtaObject parent, EmbeddedService dms) {
        this.model = model;
        this.parent = parent;
        this.dms = dms;
        initChildTopics(model);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************************
    // *** CompositeValue Implementation ***
    // *************************************



    @Override
    public Topic getTopic(String childTypeUri) {
        requireChildTopics(getAssocDef(childTypeUri));  // lazy load
        return _getTopic(childTypeUri);
    }

    @Override
    public Topic getTopic(String childTypeUri, Topic defaultTopic) {
        requireChildTopics(getAssocDef(childTypeUri));  // lazy load
        return _getTopic(childTypeUri, defaultTopic);
    }

    // ---

    @Override
    public List<Topic> getTopics(String childTypeUri) {
        requireChildTopics(getAssocDef(childTypeUri));  // lazy load
        return _getTopics(childTypeUri);
    }

    @Override
    public List<Topic> getTopics(String childTypeUri, List<Topic> defaultValue) {
        requireChildTopics(getAssocDef(childTypeUri));  // lazy load
        return _getTopics(childTypeUri, defaultValue);
    }

    // === Convenience methods ===

    @Override
    public String getString(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().toString();
    }

    @Override
    public int getInt(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().intValue();
    }

    @Override
    public long getLong(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().longValue();
    }

    @Override
    public double getDouble(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().doubleValue();
    }

    @Override
    public boolean getBoolean(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().booleanValue();
    }

    @Override
    public Object getObject(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().value();
    }

    // ---

    @Override
    public CompositeValue getComposite(String childTypeUri) {
        return getTopic(childTypeUri).getCompositeValue();
    }

    // Note: there are no convenience accessors for a multiple-valued child.

    // ---

    @Override
    public void set(String childTypeUri, SimpleValue value, ClientState clientState, Directives directives) {
        update(new CompositeValueModel().put(
            childTypeUri, new TopicModel(childTypeUri, value)
        ), clientState, directives);
    }

    // ---

    @Override
    public boolean has(String childTypeUri) {
        return model.has(childTypeUri);
    }

    // ---

    @Override
    public CompositeValueModel getModel() {
        return model;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(CompositeValueModel newComp, ClientState clientState, Directives directives) {
        try {
            for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
                String childTypeUri   = assocDef.getPartTypeUri();
                String cardinalityUri = assocDef.getPartCardinalityUri();
                TopicModel newChildTopic        = null;     // only used for "one"
                List<TopicModel> newChildTopics = null;     // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newComp.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newComp.getTopics(childTypeUri, null);      // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                updateChildTopics(newChildTopic, newChildTopics, assocDef, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating composite value of " + parent.className() + " " + parent.getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    void updateChildTopics(TopicModel newChildTopic, List<TopicModel> newChildTopics,
                                   AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        // Note: updating the child topics requires them to be loaded
        requireChildTopics(assocDef);
        //
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = newChildTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                updateCompositionOne(newChildTopic, assocDef, clientState, directives);
            } else {
                updateCompositionMany(newChildTopics, assocDef, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                updateAggregationOne(newChildTopic, assocDef, clientState, directives);
            } else {
                updateAggregationMany(newChildTopics, assocDef, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic _getTopic(String childTypeUri) {
        Topic topic = (Topic) childTopics.get(childTypeUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Child topic of type \"" + childTypeUri + "\" not found in " + childTopics);
        }
        //
        return topic;
    }

    private Topic _getTopic(String childTypeUri, Topic defaultTopic) {
        Topic topic = (Topic) childTopics.get(childTypeUri);
        return topic != null ? topic : defaultTopic;
    }

    // ---

    private List<Topic> _getTopics(String childTypeUri) {
        try {
            List<Topic> topics = (List<Topic>) childTopics.get(childTypeUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Child topics of type \"" + childTypeUri + "\" not found in " + childTopics);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    private List<Topic> _getTopics(String childTypeUri, List<Topic> defaultValue) {
        try {
            List<Topic> topics = (List<Topic>) childTopics.get(childTypeUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    // --- Composition ---

    private void updateCompositionOne(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                                ClientState clientState, Directives directives) {
        Topic childTopic = _getTopic(assocDef.getPartTypeUri(), null);
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (childTopic != null) {
            // == update child ==
            // update DB
            childTopic.update(newChildTopic, clientState, directives);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            // == create child ==
            // update DB
            childTopic = dms.createTopic(newChildTopic, clientState);
            dms.valueStorage.associateChildTopic(childTopic.getId(), parent.getModel(), assocDef, clientState);
            // update memory
            putInCompositeValue(childTopic, assocDef);
        }
    }

    private void updateCompositionMany(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                                       ClientState clientState, Directives directives) {
        for (TopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                Topic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete child" is an idempotent operation. A delete request for an child which has been
                    // deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete child ==
                // update DB
                childTopic.delete(directives);
                // update memory
                removeFromCompositeValue(childTopic, assocDef);
            } else if (childTopicId != -1) {
                Topic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    throw new RuntimeException("Topic " + childTopicId + " is not a child of " +
                        parent.className() + " " + parent.getId() + " according to " + assocDef);
                }
                // == update child ==
                // update DB
                childTopic.update(newChildTopic, clientState, directives);
                // Note: memory is already up-to-date. The child topic is updated in-place of parent.
            } else {
                // == create child ==
                // update DB
                Topic childTopic = dms.createTopic(newChildTopic, clientState);
                dms.valueStorage.associateChildTopic(childTopic.getId(), parent.getModel(), assocDef, clientState);
                // update memory
                addToCompositeValue(childTopic, assocDef);
            }
        }
    }

    // --- Aggregation ---

    private void updateAggregationOne(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                                ClientState clientState, Directives directives) {
        RelatedTopic childTopic = (RelatedTopic) _getTopic(assocDef.getPartTypeUri(), null);
        if (dms.valueStorage.isReference(newChildTopic)) {
            if (childTopic != null) {
                if (isReferingTo(newChildTopic, childTopic)) {
                    return;
                }
                // == update assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete(directives);
                Topic topic = dms.valueStorage.associateChildTopic(newChildTopic, parent.getModel(), assocDef,
                    clientState);
                // update memory
                putInCompositeValue(topic, assocDef);
            } else {
                // == create assignment ==
                // update DB
                Topic topic = dms.valueStorage.associateChildTopic(newChildTopic, parent.getModel(), assocDef,
                    clientState);
                // update memory
                putInCompositeValue(topic, assocDef);
            }
        } else {
            // == create child ==
            // update DB
            if (childTopic != null) {
                childTopic.getRelatingAssociation().delete(directives);
            }
            Topic topic = dms.createTopic(newChildTopic, clientState);
            dms.valueStorage.associateChildTopic(topic.getId(), parent.getModel(), assocDef, clientState);
            // update memory
            putInCompositeValue(topic, assocDef);
        }
    }

    private void updateAggregationMany(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                                       ClientState clientState, Directives directives) {
        for (TopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                RelatedTopic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete assignment" is an idempotent operation. A delete request for an assignment which
                    // has been deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete(directives);
                // update memory
                removeFromCompositeValue(childTopic, assocDef);
            } else if (dms.valueStorage.isReference(newChildTopic)) {
                if (isReferingTo(newChildTopic, assocDef)) {
                    // Note: "create assignment" is an idempotent operation. A create request for an assignment which
                    // exists already is not an error. Instead, nothing is performed.
                    continue;
                }
                // == create assignment ==
                // update DB
                Topic topic = dms.valueStorage.associateChildTopic(newChildTopic, parent.getModel(), assocDef,
                    clientState);
                // update memory
                addToCompositeValue(topic, assocDef);
            } else {
                // == create child ==
                // update DB
                Topic topic = dms.createTopic(newChildTopic, clientState);
                dms.valueStorage.associateChildTopic(topic.getId(), parent.getModel(), assocDef, clientState);
                // update memory
                addToCompositeValue(topic, assocDef);
            }
        }
    }

    // ---

    /**
     * Lazy-loads child topics (model) and updates this attached object cache accordingly.
     *
     * @param   assocDef    the child topics according to this association definition are loaded.
     *                      Note: the association definition must not necessarily originate from this object's
     *                      type definition. It may originate from a facet definition as well.
     */
    private void requireChildTopics(AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        if (!has(childTypeUri)) {
            logger.fine("### Lazy-loading \"" + childTypeUri + "\" child topic(s) of " + parent.className() + " " +
                parent.getId());
            dms.valueStorage.fetchChildTopics(parent.getModel(), assocDef);
            reinit(childTypeUri);
        }
    }

    // ---

    private void initChildTopics(CompositeValueModel model) {
        for (String childTypeUri : model.keys()) {
            initChildTopics(model, childTypeUri);
        }
    }

    private void initChildTopics(CompositeValueModel model, String childTypeUri) {
        Object value = model.get(childTypeUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return;
        }
        //
        if (value instanceof TopicModel) {
            TopicModel childTopic = (TopicModel) value;
            childTopics.put(childTypeUri, createTopic(childTopic));
            // recursion
            initChildTopics(childTopic.getCompositeValueModel());
        } else if (value instanceof List) {
            List<Topic> topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
            for (TopicModel childTopic : (List<TopicModel>) value) {
                topics.add(createTopic(childTopic));
                // recursion
                initChildTopics(childTopic.getCompositeValueModel());
            }
        } else {
            throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
        }
    }

    private Topic createTopic(TopicModel model) {
        if (model instanceof RelatedTopicModel) {
            // Note: composite value models obtained through *fetching* contain *related topic models*.
            // We exploit the related topics when updating assignments (in conjunction with aggregations).
            // See updateAggregationOne() and updateAggregationMany().
            return new AttachedRelatedTopic((RelatedTopicModel) model, dms);
        } else {
            // Note: composite value models for *new topics* to be created contain sole *topic models*.
            return new AttachedTopic(model, dms);
        }
    }

    // ---

    private void reinit(String childTypeUri) {
        initChildTopics(model, childTypeUri);
    }



    // === Update ===

    // --- Update this attached object cache ---

    /**
     * Puts a single-valued child. An existing value is overwritten.
     */
    private void put(String childTypeUri, Topic topic) {
        childTopics.put(childTypeUri, topic);
    }

    /**
     * Adds a value to a multiple-valued child.
     */
    private void add(String childTypeUri, Topic topic) {
        List<Topic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
        }
        topics.add(topic);
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    private void remove(String childTypeUri, Topic topic) {
        List<Topic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        if (topics != null) {
            topics.remove(topic);
        }
    }

    // --- Update this attached object cache + underlying model ---

    /**
     * For single-valued childs
     */
    private void putInCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        put(childTypeUri, childTopic);                              // attached object cache
        getModel().put(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void addToCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        add(childTypeUri, childTopic);                              // attached object cache
        getModel().add(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void removeFromCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        remove(childTypeUri, childTopic);                           // attached object cache
        getModel().remove(childTypeUri, childTopic.getModel());     // underlying model
    }



    // === Helper ===

    private RelatedTopic findChildTopic(long childTopicId, AssociationDefinition assocDef) {
        List<Topic> childTopics = _getTopics(assocDef.getPartTypeUri(), new ArrayList());
        for (Topic childTopic : childTopics) {
            if (childTopic.getId() == childTopicId) {
                return (RelatedTopic) childTopic;
            }
        }
        return null;
    }

    // ---

    /**
     * Checks weather the specified topic reference refers to any of the child topics.
     *
     * @param   assocDef    the child topics according to this association definition are considered.
     */
    private boolean isReferingTo(TopicModel topicRef, AssociationDefinition assocDef) {
        List<Topic> childTopics = _getTopics(assocDef.getPartTypeUri(), new ArrayList());
        for (Topic childTopic : childTopics) {
            if (isReferingTo(topicRef, childTopic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks weather the specified topic reference refers the specified topic.
     */
    private boolean isReferingTo(TopicModel topicRef, Topic topic) {
        if (dms.valueStorage.isReferenceById(topicRef)) {
            return topicRef.getId() == topic.getId();
        } else if (dms.valueStorage.isReferenceByUri(topicRef)) {
            return topicRef.getUri().equals(topic.getUri());
        } else {
            throw new RuntimeException("Not a topic reference (" + topicRef + ")");
        }
    }

    // ---

    private AssociationDefinition getAssocDef(String assocDefUri) {
        // Note: doesn't work for facets
        return parent.getType().getAssocDef(assocDefUri);
    }
}
