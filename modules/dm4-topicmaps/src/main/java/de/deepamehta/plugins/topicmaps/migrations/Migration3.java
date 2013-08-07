package de.deepamehta.plugins.topicmaps.migrations;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;



public class Migration3 extends Migration {

    @Override
    public void run() {
        TopicType type = dms.getTopicType("dm4.topicmaps.topicmap");
        type.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
            "dm4.topicmaps.topicmap", "dm4.topicmaps.state", "dm4.core.one", "dm4.core.one"));
    }
}
