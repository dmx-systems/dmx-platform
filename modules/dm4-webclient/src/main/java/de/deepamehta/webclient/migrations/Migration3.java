package de.deepamehta.webclient.migrations;

import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Repairing types with missing "View Config" topic.
 * Runs ALWAYS.
 * <p>
 * Note: while a "clean install" the Core types are repaired.
 * While "update" all existing types are repaired, including the ones created by installed 3rd-party modules.
 * <p>
 * Part of DM 4.8.6
 */
public class Migration3 extends Migration {

    private long deepaMehtaWorkspaceId;

    private int[][] count = new int[2][2];

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        initDeepaMehtaWorkspaceId();
        //
        logger.info("########## Repairing types with missing \"View Config\" topic (" +
            (deepaMehtaWorkspaceId == -1 ? "clean install" : "update") + " detected)");
        //
        repair(dm4.getAllTopicTypes(), 0);
        repair(dm4.getAllAssociationTypes(), 1);
        //
        logger.info("########## Repairing types with missing \"View Config\" topic complete\n    " +
            "Topic types repaired: " + count[0][1] + "/" + count[0][0] + "\n    " +
            "Association types repaired: " + count[1][1] + "/" + count[1][0]);
    }

    private void repair(List<? extends DeepaMehtaType> types, int i) {
        for (DeepaMehtaType type : types) {
            // Note: no View Config can be associated to the type "View Configuration" itself as this would cause an
            // endless recursion while fetching that type. Fetching a type involves fetching its view config, that is
            // all its view config topics, including their child topics. Fetching child topics is driven by the topic's
            // type (its assoc defs), here: "View Configuration" -- the one we're fetching just now.
            if (type.getUri().equals("dm4.webclient.view_config")) {
                continue;
            }
            //
            repair(type, i);
        }
    }

    private void repair(DeepaMehtaType type, int i) {
        try {
            ViewConfiguration viewConfig = type.getViewConfig();
            Topic configTopic = viewConfig.getConfigTopic("dm4.webclient.view_config");
            if (configTopic == null) {
                // 1) create config topic
                configTopic = viewConfig.addConfigTopic(mf.newTopicModel("dm4.webclient.view_config"));
                //
                // 2) assign workspace
                // In case of a CLEAN_INSTALL the DeepaMehta workspace does not yet exist. The config topic gets its
                // workspace assignment via type-introduction of the Workspaces module. The Workspaces module is
                // activated *after* the Webclient module.
                // In case of a UPDATE the DeepaMehta workspace exists already and we make the assignment here.
                // Type-introduction of the Workspaces module will not perform as this module is installed already.
                if (deepaMehtaWorkspaceId != -1 && isDeepaMehtaStandardType(type)) {
                    dm4.getAccessControl().assignToWorkspace(configTopic, deepaMehtaWorkspaceId);
                }
                //
                count[i][1]++;
            }
            count[i][0]++;
        } catch (Exception e) {
            throw new RuntimeException("Repairing type \"" + type.getUri() + "\" failed", e);
        }
    }

    private void initDeepaMehtaWorkspaceId() {
        Topic ws = dm4.getTopicByUri("dm4.workspaces.deepamehta");
        deepaMehtaWorkspaceId = ws != null ? ws.getId() : -1;
    }

    // Copied from WorkspacePlugin.java
    private boolean isDeepaMehtaStandardType(DeepaMehtaType type) {
        return type.getUri().startsWith("dm4.");
    }
}
