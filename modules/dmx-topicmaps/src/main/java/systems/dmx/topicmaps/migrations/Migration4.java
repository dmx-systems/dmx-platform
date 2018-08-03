package systems.dmx.topicmaps.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds "Private" flag to Topicmap.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.topicmaps.private", "Private", "dmx.core.boolean"));
        dmx.getTopicType("dmx.topicmaps.topicmap").addAssocDef(mf.newAssociationDefinitionModel(
            "dmx.core.composition_def", "dmx.topicmaps.topicmap", "dmx.topicmaps.private",
            "dmx.core.one", "dmx.core.one"
        ));
    }
}
