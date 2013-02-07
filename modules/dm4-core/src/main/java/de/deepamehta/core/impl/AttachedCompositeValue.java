package de.deepamehta.core.impl;

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

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initChildTopics(CompositeValueModel model) {
        for (String childTypeUri : model.keys()) {
            Object value = model.get(childTypeUri);
            if (value instanceof TopicModel) {
                TopicModel childTopic = (TopicModel) value;
                childTopics.put(childTypeUri, new AttachedTopic(childTopic, dms));
                initChildTopics(childTopic.getCompositeValueModel());
            } else if (value instanceof List) {
                List<Topic> topics = new ArrayList();
                childTopics.put(childTypeUri, topics);
                for (TopicModel childTopic : (List<TopicModel>) value) {
                    topics.add(new AttachedTopic(childTopic, dms));
                    initChildTopics(childTopic.getCompositeValueModel());
                }
            } else {
                throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
            }
        }
    }

    // ---

    private void put(String childTypeUri, Topic topic) {
        childTopics.put(childTypeUri, topic);
    }
}
