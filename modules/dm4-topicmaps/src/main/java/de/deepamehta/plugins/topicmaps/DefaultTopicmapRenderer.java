package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.ModelFactory;



class DefaultTopicmapRenderer implements TopicmapRenderer {

    @Override
    public String getUri() {
        return "dm4.webclient.default_topicmap_renderer";
    }

    @Override
    public ChildTopicsModel initialTopicmapState(ModelFactory mf) {
        return mf.newChildTopicsModel()
            .put("dm4.topicmaps.translation", mf.newChildTopicsModel()
                .put("dm4.topicmaps.translation_x", 0)
                .put("dm4.topicmaps.translation_y", 0)
            );
    }
}
