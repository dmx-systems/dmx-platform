package systems.dmx.typeeditor.migrations;

import systems.dmx.core.service.Migration;



public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dmx.core.role_type", "page_renderer_uri", "dmx.typeeditor.roletype_renderer");
    }
}
