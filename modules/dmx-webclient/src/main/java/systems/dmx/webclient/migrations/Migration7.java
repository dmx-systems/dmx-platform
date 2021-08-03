package systems.dmx.webclient.migrations;

import static systems.dmx.webclient.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.service.Migration;



/**
 * Adds arrow heads to 2 more role types.
 * <p>
 * Part of DMX 5.3
 * Runs ALWAYS
 */
public class Migration7 extends Migration {

    @Override
    public void run() {
        dmx.getRoleType(SEQUENCE_START).getViewConfig().setConfigValue(
            VIEW_CONFIG, "dmx.webclient.arrow_shape", "triangle"
        );
        dmx.getRoleType(SUCCESSOR).getViewConfig().setConfigValue(
            VIEW_CONFIG, "dmx.webclient.arrow_shape", "triangle"
        );
    }
}
