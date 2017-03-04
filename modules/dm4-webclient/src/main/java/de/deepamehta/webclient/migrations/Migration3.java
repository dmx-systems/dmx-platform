package de.deepamehta.webclient.migrations;

import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.accesscontrol.AccessControl;

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

    private int[][] count = new int[2][2];

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        logger.info("########## Repairing types with missing \"View Config\" topic");
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
            String typeUri = type.getUri();
            // Note: to these 2 types no View Config can be associated:
            // 1) "Meta Meta Type" as it does not exist in the DB (its in-memory ID is -1).
            // 2) "View Configuration" itself as this would cause an endless recursion while fetching that type.
            // Fetching a type involves fetching its view config, that is all its view config topics, including
            // their child topics. Fetching child topics is driven by the topic's type (its assoc defs),
            // here: "View Configuration" -- the one we're fetching just now.
            if (typeUri.equals("dm4.core.meta_meta_type") || typeUri.equals("dm4.webclient.view_config")) {
                continue;
            }
            ViewConfiguration viewConfig = type.getViewConfig();
            Topic configTopic = viewConfig.getConfigTopic("dm4.webclient.view_config");
            if (configTopic == null) {
                // create config topic
                configTopic = viewConfig.addConfigTopic(mf.newTopicModel("dm4.webclient.view_config"));
                // assign workspace
                if (isDeepaMehtaStandardType(type)) {
                    AccessControl ac = dm4.getAccessControl();
                    ac.assignToWorkspace(configTopic, ac.getDeepaMehtaWorkspaceId());
                }
                count[i][1]++;
            }
            count[i][0]++;
        }
    }

    // Copied from WorkspacePlugin.java
    private boolean isDeepaMehtaStandardType(DeepaMehtaType type) {
        return type.getUri().startsWith("dm4.");
    }
}
