package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
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
        for (Topic topic : dms.getAllTopics()) {
            migrateObject(topic, "topic");
        }
        count = 0;
        for (Association assoc : dms.getAllAssociations()) {
            migrateObject(assoc, "association");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateObject(DeepaMehtaObject object, String type) {
        try {
            count++;
            String info = "### Migrating " + type + " " + object.getId() + " (#" + count + ")";
            if (object.hasProperty("creator")) {
                logger.info(info);
                renameProperty(object, "creator", "dm4.accesscontrol.creator", true);   // addToIndex=true
                renameProperty(object, "owner",   "dm4.accesscontrol.owner",   true);   // addToIndex=true
                renameProperty(object, "acl",     "dm4.accesscontrol.acl",     false);  // addToIndex=false
            } else {
                logger.info(info + " ABORTED -- Access control information not availble");
            }
        } catch (Exception e) {
            throw new RuntimeException("Migrating " + type + " " + object.getId() + " failed (" + object + ")", e);
        }
    }

    private void renameProperty(DeepaMehtaObject object, String oldPropUri, String newPropUri, boolean addToIndex) {
        String propValue = (String) object.getProperty(oldPropUri);
        object.setProperty(newPropUri, propValue, addToIndex);
        object.removeProperty(oldPropUri);
    }
}
