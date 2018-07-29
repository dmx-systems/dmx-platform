package systems.dmx.workspaces.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds "Sharing Mode" to "Workspace".
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.5
 */
public class Migration3 extends Migration {

    @Override
    public void run() {
        dm4.getTopicType("dm4.workspaces.workspace").addAssocDef(mf.newAssociationDefinitionModel(
            "dm4.core.aggregation_def",
            "dm4.workspaces.workspace", "dm4.workspaces.sharing_mode", "dm4.core.many", "dm4.core.one",
            mf.newViewConfigurationModel().setConfigValueRef(
                "dm4.webclient.view_config", "dm4.webclient.widget", "dm4.webclient.select"
            )
        ));
    }
}
