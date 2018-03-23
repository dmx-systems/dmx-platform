package de.deepamehta.geomaps;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.topicmaps.TopicmapRenderer;



// ### TODO: rename to Geomap
class GeomapRenderer implements TopicmapRenderer {

    @Override
    public String getUri() {
        // ### TODO: change to "dm4.geomaps.geomap"
        return "dm4.geomaps.geomap_renderer";
    }

    @Override
    public ChildTopicsModel initialTopicmapState(ModelFactory mf) {
        return mf.newChildTopicsModel()
            .put("dm4.topicmaps.translation", mf.newChildTopicsModel()
                .put("dm4.topicmaps.translation_x", 11.0)     // default region is "Germany"
                .put("dm4.topicmaps.translation_y", 51.0)
            )
            .put("dm4.topicmaps.zoom_level", 6);
    }
}
