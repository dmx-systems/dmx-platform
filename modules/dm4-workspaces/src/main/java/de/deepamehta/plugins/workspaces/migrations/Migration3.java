package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Migration;



public class Migration3 extends Migration {

    @Override
    public void run() {
        //
        // 1) add "Workspace Type" to "Workspace"
        dms.getTopicType("dm4.workspaces.workspace").addAssocDef(
            new AssociationDefinitionModel("dm4.core.aggregation_def",
                "dm4.workspaces.workspace", "dm4.workspaces.type", "dm4.core.many", "dm4.core.one")
        );
        //
        // 2) set type "Public" for workspace "DeepaMehta"
        dms.getTopic("uri", new SimpleValue("de.workspaces.deepamehta"), false).update(
            new TopicModel(null, new CompositeValueModel().putRef("dm4.workspaces.type", "dm4.workspaces.type.public")),
            null, new Directives());
    }
}
