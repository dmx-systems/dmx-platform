package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;



/**
 * Part of DM 4.5
 */
public class Migration3 extends Migration {

    @Override
    public void run() {
        // add "Sharing Mode" to "Workspace"
        dms.getTopicType("dm4.workspaces.workspace").addAssocDef(
            new AssociationDefinitionModel("dm4.core.aggregation_def",
                "dm4.workspaces.workspace", "dm4.workspaces.sharing_mode", "dm4.core.many", "dm4.core.one")
        );
    }
}
