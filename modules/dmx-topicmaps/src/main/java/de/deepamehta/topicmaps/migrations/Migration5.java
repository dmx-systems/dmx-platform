package de.deepamehta.topicmaps.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Migration;



/**
 * Add "Private" flag to Topicmap.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        dm4.createTopicType(mf.newTopicTypeModel("dm4.topicmaps.private", "Private", "dm4.core.boolean"));
        dm4.getTopicType("dm4.topicmaps.topicmap").addAssocDef(mf.newAssociationDefinitionModel(
            "dm4.core.composition_def", "dm4.topicmaps.topicmap", "dm4.topicmaps.private",
            "dm4.core.one", "dm4.core.one"
        ));
    }
}
