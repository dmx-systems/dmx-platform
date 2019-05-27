package systems.dmx.webclient.migrations;

import systems.dmx.core.DMXType;
import systems.dmx.core.Topic;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Repairing types with missing "View Config" topic.
 * <p>
 * Note: while a "clean install" the Core types, and other types created before Webclient is activated, are repaired.
 * While "update" all existing types are repaired, including the ones created by installed 3rd-party modules.
 * <p>
 * Part of DMX 5.0-beta-3
 * Runs ALWAYS
 */
public class Migration3 extends Migration {

    private long dmxWorkspaceId;

    private int[][] count = new int[2][2];

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        initDMXWorkspaceId();
        //
        logger.info("########## Repairing types with missing \"View Config\" topic (" +
            (dmxWorkspaceId == -1 ? "clean install" : "update") + " detected)");
        //
        repair(dmx.getAllTopicTypes(), 0);
        repair(dmx.getAllAssociationTypes(), 1);
        //
        logger.info("########## Repairing types with missing \"View Config\" topic complete\n    " +
            "Topic types repaired: " + count[0][1] + "/" + count[0][0] + "\n    " +
            "Association types repaired: " + count[1][1] + "/" + count[1][0]);
    }

    private void repair(List<? extends DMXType> types, int i) {
        for (DMXType type : types) {
            // Note: no View Config can be associated to the type "View Configuration" itself as this would cause an
            // endless recursion while fetching that type. Fetching a type involves fetching its view config, that is
            // all its view config topics, including their child topics. Fetching child topics is driven by the topic's
            // type (its assoc defs), here: "View Configuration" -- the one we're fetching just now.
            if (type.getUri().equals("dmx.webclient.view_config")) {
                continue;
            }
            //
            repair(type, i);
        }
    }

    private void repair(DMXType type, int i) {
        try {
            ViewConfiguration viewConfig = type.getViewConfig();
            Topic configTopic = viewConfig.getConfigTopic("dmx.webclient.view_config");
            if (configTopic == null) {
                // 1) create config topic
                configTopic = viewConfig.addConfigTopic(mf.newTopicModel("dmx.webclient.view_config"));
                //
                // 2) assign workspace
                // In case of a CLEAN_INSTALL the DMX workspace does not yet exist. The config topic gets its
                // workspace assignment via type-introduction of the Workspaces module. The Workspaces module is
                // activated *after* the Webclient module.
                // In case of a UPDATE the DMX workspace exists already and we make the assignment here.
                // Type-introduction of the Workspaces module will not perform as this module is installed already.
                if (dmxWorkspaceId != -1 && isDMXStandardType(type)) {
                    dmx.getAccessControl().assignToWorkspace(configTopic, dmxWorkspaceId);
                }
                //
                count[i][1]++;
            }
            count[i][0]++;
        } catch (Exception e) {
            throw new RuntimeException("Repairing type \"" + type.getUri() + "\" failed", e);
        }
    }

    private void initDMXWorkspaceId() {
        Topic ws = dmx.getTopicByUri("dmx.workspaces.dmx");
        dmxWorkspaceId = ws != null ? ws.getId() : -1;
    }

    // Copied from WorkspacePlugin.java
    private boolean isDMXStandardType(DMXType type) {
        return type.getUri().startsWith("dmx.");
    }
}
