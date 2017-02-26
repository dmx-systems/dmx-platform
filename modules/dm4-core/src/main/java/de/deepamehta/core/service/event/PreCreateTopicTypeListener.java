package de.deepamehta.core.service.event;

import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.EventListener;



public interface PreCreateTopicTypeListener extends EventListener {

    void preCreateTopicType(TopicTypeModel model);
}
