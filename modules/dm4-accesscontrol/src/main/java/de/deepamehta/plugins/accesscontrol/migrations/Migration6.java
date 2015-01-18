package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Removes "owner" and "acl" properties.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration6 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long objects = 0, ownerRemoved = 0, aclRemoved = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Running Access Control migration 4: removing owner and acl properties");
        for (Topic topic : dms.getAllTopics()) {
            migrateObject(topic, "topic");
        }
        for (Association assoc : dms.getAllAssociations()) {
            migrateObject(assoc, "association");
        }
        logger.info("########## Access Control migration 4 complete.\n    Objects processed: " + objects +
            "\n    Owner properties removed: " + ownerRemoved + "\n    ACL properties removed: " + aclRemoved);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateObject(DeepaMehtaObject object, String type) {
        try {
            objects++;
            if (object.hasProperty("dm4.accesscontrol.owner")) {
                // only workspaces keep the "owner" property
                if (!object.getTypeUri().equals("dm4.workspaces.workspace")) {
                    object.removeProperty("dm4.accesscontrol.owner");
                    ownerRemoved++;
                }
            }
            if (object.hasProperty("dm4.accesscontrol.acl")) {
                object.removeProperty("dm4.accesscontrol.acl");
                aclRemoved++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Migrating " + type + " " + object.getId() + " failed (" + object + ")", e);
        }
    }
}
