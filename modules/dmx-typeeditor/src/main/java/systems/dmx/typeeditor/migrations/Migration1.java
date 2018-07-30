package systems.dmx.typeeditor.migrations;

import systems.dmx.core.service.Migration;



public class Migration1 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dmx.core.topic_type", "page_renderer_uri", "dmx.typeeditor.topictype_renderer");
        setTopicTypeViewConfigValue("dmx.core.assoc_type", "page_renderer_uri", "dmx.typeeditor.assoctype_renderer");
    }
}
