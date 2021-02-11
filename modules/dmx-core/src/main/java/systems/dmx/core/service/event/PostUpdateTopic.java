package systems.dmx.core.service.event;

import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.service.EventListener;



public interface PostUpdateTopic extends EventListener {

    void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel);
}
