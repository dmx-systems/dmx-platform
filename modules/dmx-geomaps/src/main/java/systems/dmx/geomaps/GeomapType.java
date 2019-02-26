package systems.dmx.geomaps;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.topicmaps.TopicmapType;



class GeomapType implements TopicmapType {

    @Override
    public String getUri() {
        return "dmx.geomaps.geomap";
    }

    @Override
    public ChildTopicsModel initialTopicmapState(ModelFactory mf) {
        return mf.newChildTopicsModel()
            .put("dmx.topicmaps.translation", mf.newChildTopicsModel()
                .put("dmx.topicmaps.translation_x", 11.0)     // default region is "Germany"
                .put("dmx.topicmaps.translation_y", 51.0)
            )
            .put("dmx.topicmaps.zoom_level", 6);
    }
}
