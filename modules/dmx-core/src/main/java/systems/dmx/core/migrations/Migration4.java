package systems.dmx.core.migrations;

import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Migration;



/**
 * Renames data type "Identity" -> "Entity".
 *
 * Part of DMX 5.0
 * Runs only on UPDATE
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        Topic identity = dmx.getTopicByUri("dmx.core.identity");
        identity.setUri("dmx.core.entity");
        identity.setSimpleValue(new SimpleValue("Entity"));
    }
}
