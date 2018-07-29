package systems.dmx.typeeditor.migrations;

import systems.dmx.core.service.Migration;



public class Migration1 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dm4.core.topic_type", "page_renderer_uri", "dm4.typeeditor.topictype_renderer");
        setTopicTypeViewConfigValue("dm4.core.assoc_type", "page_renderer_uri", "dm4.typeeditor.assoctype_renderer");
    }
}
