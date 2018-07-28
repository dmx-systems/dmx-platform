package de.deepamehta.accesscontrol.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.DMXObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long count;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        count = 0;
        for (Topic topic : dm4.getAllTopics()) {
            migrateObject(topic, "topic");
        }
        count = 0;
        for (Association assoc : dm4.getAllAssociations()) {
            migrateObject(assoc, "association");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateObject(DMXObject object, String type) {
        try {
            count++;
            String info = "### Migrating " + type + " " + object.getId() + " (#" + count + ")";
            if (object.hasProperty("creator")) {
                logger.info(info);
                renameProperty(object, "creator", "dm4.accesscontrol.creator", true);   // addToIndex=true
                renameProperty(object, "owner",   "dm4.accesscontrol.owner",   true);   // addToIndex=true
                renameProperty(object, "acl",     "dm4.accesscontrol.acl",     false);  // addToIndex=false
            } else {
                logger.info(info + " SKIPPED -- Access control information not availble");
            }
        } catch (Exception e) {
            throw new RuntimeException("Migrating " + type + " " + object.getId() + " failed (" + object + ")", e);
        }
    }

    private void renameProperty(DMXObject object, String oldPropUri, String newPropUri, boolean addToIndex) {
        String propValue = (String) object.getProperty(oldPropUri);
        object.setProperty(newPropUri, propValue, addToIndex);
        object.removeProperty(oldPropUri);
    }
}
