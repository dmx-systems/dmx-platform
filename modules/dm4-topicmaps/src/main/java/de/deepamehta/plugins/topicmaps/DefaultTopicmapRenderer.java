package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.ChildTopicsModel;



class DefaultTopicmapRenderer implements TopicmapRenderer {

    @Override
    public String getUri() {
        return "dm4.webclient.default_topicmap_renderer";
    }

    @Override
    public ChildTopicsModel initialTopicmapState() {
        return new ChildTopicsModel()
            .put("dm4.topicmaps.translation", new ChildTopicsModel()
                .put("dm4.topicmaps.translation_x", 0)
                .put("dm4.topicmaps.translation_y", 0)
            );
    }
}
