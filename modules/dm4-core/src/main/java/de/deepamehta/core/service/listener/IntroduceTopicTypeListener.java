package de.deepamehta.core.service.listener;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Listener;



public interface IntroduceTopicTypeListener extends Listener {

    void introduceTopicType(TopicType topicType, ClientState clientState);
}
