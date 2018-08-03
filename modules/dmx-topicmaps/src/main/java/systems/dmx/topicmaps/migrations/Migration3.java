package systems.dmx.topicmaps.migrations;

import systems.dmx.core.TopicType;
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
        TopicType type = dmx.getTopicType("dmx.topicmaps.topicmap");
        type.addAssocDef(mf.newAssociationDefinitionModel("dmx.core.composition_def",
            "dmx.topicmaps.topicmap", "dmx.topicmaps.state", "dmx.core.one", "dmx.core.one"));
    }
}
