package systems.dmx.iconpicker.migrations;

import systems.dmx.core.service.Migration;



public class Migration1 extends Migration {

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dm4.webclient.icon", "simple_renderer_uri", "dm4.iconpicker.icon_renderer");
    }
}
