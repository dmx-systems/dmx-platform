package systems.dmx.workspaces.migrations;

import static systems.dmx.workspaces.Constants.*;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;



/**
 * Sets labels of "Enabled Sharing Modes" topics created before DMX 5.2
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration4 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        for (Topic topic : dmx.getTopicsByType(ENABLED_SHARING_MODES)) {
            topic.setSimpleValue(ENABLED_SHARING_MODES_LABEL);
        }
    }
}
