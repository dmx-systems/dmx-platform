package systems.dmx.core.service.event;

import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateTopicType extends EventListener {

    void preCreateTopicType(TopicTypeModel model);
}
