package systems.dmx.accesscontrol.migrations;

import systems.dmx.core.Association;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;

import java.util.logging.Logger;



public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long count;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        count = 0;
        for (Topic topic : dmx.getAllTopics()) {
            migrateObject(topic, "topic");
        }
        count = 0;
        for (Association assoc : dmx.getAllAssociations()) {
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
                renameProperty(object, "creator", "dmx.accesscontrol.creator", true);   // addToIndex=true
                renameProperty(object, "owner",   "dmx.accesscontrol.owner",   true);   // addToIndex=true
                renameProperty(object, "acl",     "dmx.accesscontrol.acl",     false);  // addToIndex=false
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
