package systems.dmx.typeeditor.migrations;

import systems.dmx.core.service.Migration;



public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dm4.core.role_type", "page_renderer_uri", "dm4.typeeditor.roletype_renderer");
    }
}
