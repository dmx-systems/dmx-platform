package systems.dmx.topicmaps.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds "Topicmap State" to Topicmap.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 */
public class Migration3 extends Migration {

    @Override
    public void run() {
        dmx.getTopicType("dmx.topicmaps.topicmap").addAssocDef(
            mf.newAssociationDefinitionModel("dmx.topicmaps.topicmap", "dmx.topicmaps.state", "dmx.core.one")
        );
    }
}
