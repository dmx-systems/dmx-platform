package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long topicCount = 0;
    private long assocCount = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        for (Topic topic : dms.getAllTopics()) {
            migrateTopic(topic);
        }
        for (Association assoc : dms.getAllAssociations()) {
            migrateAssociation(assoc);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateTopic(Topic topic) {
        try {
            topicCount++;
            long topicId = topic.getId();
            String info = "### Migrating topic " + topicId + " (#" + topicCount + ")";
            if (dms.hasTopicProperty(topicId, "creator")) {
                logger.info(info);
                renameTopicProperty(topicId, "creator", "dm4.accesscontrol.creator", true);         // addToIndex=true
                renameTopicProperty(topicId, "owner",   "dm4.accesscontrol.owner",   true);         // addToIndex=true
                renameTopicProperty(topicId, "acl",     "dm4.accesscontrol.acl",     false);        // addToIndex=false
            } else {
                logger.info(info + " ABORTED -- Access control information not availble");
            }
        } catch (Exception e) {
            throw new RuntimeException("Migrating topic " + topic.getId() + " failed (" + topic + ")", e);
        }
    }

    private void migrateAssociation(Association assoc) {
        try {
            assocCount++;
            long assocId = assoc.getId();
            String info = "### Migrating association " + assocId + " (#" + assocCount + ")";
            if (dms.hasAssociationProperty(assocId, "creator")) {
                logger.info(info);
                renameAssociationProperty(assocId, "creator", "dm4.accesscontrol.creator", true);   // addToIndex=true
                renameAssociationProperty(assocId, "owner",   "dm4.accesscontrol.owner",   true);   // addToIndex=true
                renameAssociationProperty(assocId, "acl",     "dm4.accesscontrol.acl",     false);  // addToIndex=false
            } else {
                logger.info(info + " ABORTED -- Access control information not availble");
            }
        } catch (Exception e) {
            throw new RuntimeException("Migrating association " + assoc.getId() + " failed (" + assoc + ")", e);
        }
    }

    // ---

    private void renameTopicProperty(long topicId, String oldPropUri, String newPropUri, boolean addToIndex) {
        String propValue = (String) dms.getTopicProperty(topicId, oldPropUri);
        dms.setTopicProperty(topicId, newPropUri, propValue, addToIndex);
        dms.removeTopicProperty(topicId, oldPropUri);
    }

    private void renameAssociationProperty(long assocId, String oldPropUri, String newPropUri, boolean addToIndex) {
        String propValue = (String) dms.getAssociationProperty(assocId, oldPropUri);
        dms.setAssociationProperty(assocId, newPropUri, propValue, addToIndex);
        dms.removeAssociationProperty(assocId, oldPropUri);
    }
}
