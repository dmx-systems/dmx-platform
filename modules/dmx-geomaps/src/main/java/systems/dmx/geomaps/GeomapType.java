package systems.dmx.geomaps;

import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.topicmaps.TopicmapsConstants;
import systems.dmx.topicmaps.TopicmapType;



class GeomapType implements TopicmapType, TopicmapsConstants {

    @Override
    public String getUri() {
        return "dmx.geomaps.geomap";
    }

    @Override
    public void initTopicmapState(ViewProps viewProps) {
        viewProps
            .put(PROP_PAN_X, 11.0)      // default region is "Germany"
            .put(PROP_PAN_Y, 51.0)
            .put(PROP_ZOOM, 6);
    }
}
