package systems.dmx.topicmaps;

import static systems.dmx.topicmaps.Constants.*;
import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.service.CoreService;



class DefaultTopicmapType implements TopicmapType {

    @Override
    public String getUri() {
        return TOPICMAP;
    }

    @Override
    public void initTopicmapState(Topic topicmapTopic, ViewProps viewProps, CoreService dmx) {
        dmx.getModelFactory().newViewProps()
            .set(TOPICMAP_PAN_X, 0)
            .set(TOPICMAP_PAN_Y, 0)
            .set(TOPICMAP_ZOOM, 1.0)
            .store(topicmapTopic);
    }
}
