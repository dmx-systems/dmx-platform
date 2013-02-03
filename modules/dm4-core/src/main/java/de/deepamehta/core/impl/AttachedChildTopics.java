package de.deepamehta.core.impl;

import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class AttachedChildTopics implements ChildTopics {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: AttachedTopic or List<AttachedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedChildTopics(ChildTopicsModel model, EmbeddedService dms) {
        this.dms = dms;
        initChildTopics(model);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initChildTopics(ChildTopicsModel model) {
        for (String childTypeUri : model.keys()) {
            Object value = model.get(childTypeUri);
            if (value instanceof TopicModel) {
                TopicModel childTopic = (TopicModel) value;
                childTopics.put(childTypeUri, new AttachedTopic(childTopic, dms));
                initChildTopics(childTopic.getCompositeValue());
            } else if (value instanceof List) {
                List<Topic> topics = new ArrayList();
                childTopics.put(childTypeUri, topics);
                for (TopicModel childTopic : (List<TopicModel>) value) {
                    topics.add(new AttachedTopic(childTopic, dms));
                    initChildTopics(childTopic.getCompositeValue());
                }
            } else {
                throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
            }
        }
    }
}
