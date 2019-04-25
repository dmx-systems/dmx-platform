package systems.dmx.topicmaps;

import systems.dmx.core.model.topicmaps.ViewProps;



class DefaultTopicmapType implements TopicmapType, TopicmapsConstants {

    @Override
    public String getUri() {
        return TOPICMAP;
    }

    @Override
    public void initTopicmapState(ViewProps viewProps) {
        viewProps
            .put(PROP_PAN_X, 0)
            .put(PROP_PAN_Y, 0)
            .put(PROP_ZOOM, 1.0);
    }
}
