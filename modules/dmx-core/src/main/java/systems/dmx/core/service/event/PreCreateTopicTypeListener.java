package systems.dmx.core.service.event;

import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateTopicTypeListener extends EventListener {

    void preCreateTopicType(TopicTypeModel model);
}
