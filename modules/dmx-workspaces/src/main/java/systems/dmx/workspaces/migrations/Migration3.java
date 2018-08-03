package systems.dmx.workspaces.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds "Sharing Mode" to "Workspace".
 * Runs ALWAYS.
 * <p>
 * Part of DMX 5.0
 */
public class Migration3 extends Migration {

    @Override
    public void run() {
        dmx.getTopicType("dmx.workspaces.workspace").addAssocDef(mf.newAssociationDefinitionModel(
            "dmx.core.composition_def",
            "dmx.workspaces.workspace", "dmx.workspaces.sharing_mode", "dmx.core.many", "dmx.core.one",
            mf.newViewConfigurationModel().setConfigValueRef(
                "dmx.webclient.view_config", "dmx.webclient.widget", "dmx.webclient.select"
            )
        ));
    }
}
