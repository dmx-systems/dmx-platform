package systems.dmx.webclient.migrations;

import static systems.dmx.webclient.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.RoleType;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;

import java.util.logging.Logger;



/**
 * Adds view configs to all role types.
 * <p>
 * Part of DMX 5.2
 * Runs ALWAYS
 */
public class Migration6 extends Migration {

    // Basically copied from Migration3.java

    private long dmxWorkspaceId;

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        int count = 0;
        int assigned = 0;
        //
        initDMXWorkspaceId();
        //
        for (RoleType roleType : dmx.getAllRoleTypes()) {
            // 1) create view config topic
            Topic configTopic = roleType.getViewConfig().addConfigTopic(mf.newTopicModel(VIEW_CONFIG));
            //
            // 2) assign view config topic to workspace
            // In case of a CLEAN_INSTALL the DMX workspace does not yet exist. The config topic gets its
            // workspace assignment via type-introduction of the Workspaces module. The Workspaces module is
            // activated *after* the Webclient module. ### FIXME: for role types there is no type-introduction.
            // In case of a UPDATE the DMX workspace exists already and we make the assignment here.
            // Type-introduction of the Workspaces module will not perform as this module is installed already.
            if (dmxWorkspaceId != -1 && isDMXStandardType(roleType)) {
                dmx.getPrivilegedAccess().assignToWorkspace(configTopic, dmxWorkspaceId);
                assigned++;
            }
            count++;
        }
        //
        dmx.getRoleType(CHILD_TYPE).getViewConfig().setConfigValue(
            VIEW_CONFIG, "dmx.webclient.arrow_shape", "triangle"
        );
        dmx.getRoleType(CHILD).getViewConfig().setConfigValue(
            VIEW_CONFIG, "dmx.webclient.arrow_shape", "triangle"
        );
        dmx.getRoleType(TYPE).getViewConfig().setConfigValue(
            VIEW_CONFIG, "dmx.webclient.arrow_shape", "triangle"
        );
        //
        logger.info("##### Adding view configs to role types complete\n    " +
            "View configs added: " + count + "\n    " +
            "Assigned to DMX workspace: " + assigned);
    }

    private void initDMXWorkspaceId() {
        Topic ws = dmx.getTopicByUri("dmx.workspaces.dmx");
        dmxWorkspaceId = ws != null ? ws.getId() : -1;
    }

    // Copied from WorkspacePlugin.java
    private boolean isDMXStandardType(Topic roleType) {
        return roleType.getUri().startsWith("dmx.");
    }
}
