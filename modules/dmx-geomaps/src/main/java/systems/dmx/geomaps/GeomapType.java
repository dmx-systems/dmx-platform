package systems.dmx.geomaps;

import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.service.CoreService;
import systems.dmx.topicmaps.TopicmapType;



class GeomapType implements TopicmapType, GeomapsConstants {

    @Override
    public String getUri() {
        return "dmx.geomaps.geomap";
    }

    @Override
    public void initTopicmapState(Topic topicmapTopic, ViewProps viewProps, CoreService dmx) {
        dmx.getModelFactory().newViewProps()
            .put(PROP_LONGITUDE, 11.0)      // default region is "Germany"
            .put(PROP_LATITUDE, 51.0)
            .put(PROP_ZOOM, 6.0)
            .store(topicmapTopic);
    }
}
