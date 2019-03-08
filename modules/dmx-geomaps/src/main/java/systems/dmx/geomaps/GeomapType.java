package systems.dmx.geomaps;

import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.topicmaps.TopicmapType;



class GeomapType implements TopicmapType, GeomapsConstants {

    @Override
    public String getUri() {
        return "dmx.geomaps.geomap";
    }

    @Override
    public void initTopicmapState(ViewProps viewProps) {
        viewProps
            .put(PROP_LONGITUDE, 11.0)      // default region is "Germany"
            .put(PROP_LATITUDE, 51.0)
            .put(PROP_ZOOM, 6.0);
    }
}
