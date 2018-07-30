package systems.dmx.iconpicker.migrations;

import systems.dmx.core.service.Migration;



public class Migration1 extends Migration {

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dmx.webclient.icon", "simple_renderer_uri", "dmx.iconpicker.icon_renderer");
    }
}
