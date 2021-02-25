package systems.dmx.files.migrations;

import systems.dmx.core.service.Migration;



/**
 * Sets "Noneditable" view config for "File" and "Folder" types.
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration4 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        dmx.getTopicType("dmx.files.file").getViewConfig().setConfigValue(
            "dmx.webclient.view_config", "dmx.webclient.noneditable", true
        );
        dmx.getTopicType("dmx.files.folder").getViewConfig().setConfigValue(
            "dmx.webclient.view_config", "dmx.webclient.noneditable", true
        );
    }
}
