package systems.dmx.topicmaps.migrations;

import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.Migration;



/**
 * Add "Private" flag to Topicmap.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.topicmaps.private", "Private", "dmx.core.boolean"));
        dmx.getTopicType("dmx.topicmaps.topicmap").addAssocDef(mf.newAssociationDefinitionModel(
            "dmx.core.composition_def", "dmx.topicmaps.topicmap", "dmx.topicmaps.private",
            "dmx.core.one", "dmx.core.one"
        ));
    }
}
