package systems.dmx.topicmaps;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.ModelFactory;



// ### TODO: rename to "DefaultTopicmap"
class DefaultTopicmapRenderer implements TopicmapRenderer {

    @Override
    public String getUri() {
        // ### TODO: change to "dmx.topicmaps.default_topicmap"
        return "dmx.webclient.default_topicmap_renderer";
    }

    @Override
    public ChildTopicsModel initialTopicmapState(ModelFactory mf) {
        return mf.newChildTopicsModel()
            .put("dmx.topicmaps.translation", mf.newChildTopicsModel()
                .put("dmx.topicmaps.translation_x", 0)
                .put("dmx.topicmaps.translation_y", 0)
            );
    }
}
