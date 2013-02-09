package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.CompositeValue;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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
        Topic topic = (Topic) childTopics.get(childTypeUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Child topic of type \"" + childTypeUri + "\" not found in " + this);
        }
        //
        return topic;
    }

    @Override
    public Topic getTopic(String childTypeUri, Topic defaultTopic) {
        Topic topic = (Topic) childTopics.get(childTypeUri);
        return topic != null ? topic : defaultTopic;
    }

    @Override
    public boolean has(String childTypeUri) {
        return model.has(childTypeUri);
    }

    // --- Convenience methods ---

    @Override
    public String getString(String childTypeUri) {
        return model.getString(childTypeUri);
    }

    // ---

    @Override
    public CompositeValueModel getModel() {
        return model;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void updateCompositionOne(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                        Directives directives) {
        Topic childTopic = getTopic(assocDef.getPartTypeUri(), null);
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (childTopic != null) {
            // == update child ==
            // update DB
            childTopic.update(newChildTopic, clientState, directives);
            // update memory
            // Note: memory is already up-to-date! The child topic is updated in-place of parent.
        } else {
            // == create child ==
            // update DB
            childTopic = dms.createTopic(newChildTopic, clientState);
            dms.valueStorage.associateChildTopic(childTopic.getId(), parent.getModel(), assocDef, clientState);
            // update memory
            putInCompositeValue(childTopic, assocDef);
        }
    }

    // ---

    void reinit() {
        initChildTopics(model);
    }

    void reinit(String childTypeUri) {
        initChildTopics(model, childTypeUri);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

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
            childTopics.put(childTypeUri, new AttachedTopic(childTopic, dms));
            // recursion
            initChildTopics(childTopic.getCompositeValueModel());
        } else if (value instanceof List) {
            List<Topic> topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
            for (TopicModel childTopic : (List<TopicModel>) value) {
                topics.add(new AttachedTopic(childTopic, dms));
                // recursion
                initChildTopics(childTopic.getCompositeValueModel());
            }
        } else {
            throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
        }
    }

    // ---

    private void put(String childTypeUri, Topic topic) {
        childTopics.put(childTypeUri, topic);
    }

    // ---

    /**
     * For single-valued childs
     */
    private void putInCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        //
        put(childTypeUri, childTopic);                          // update this attached object cache
        getModel().put(childTypeUri, childTopic.getModel());    // update underlying model
    }
}
