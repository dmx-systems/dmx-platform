package systems.dmx.topicmaps;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.ModelFactory;



class DefaultTopicmapType implements TopicmapType {

    @Override
    public String getUri() {
        return "dmx.topicmaps.topicmap";
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
