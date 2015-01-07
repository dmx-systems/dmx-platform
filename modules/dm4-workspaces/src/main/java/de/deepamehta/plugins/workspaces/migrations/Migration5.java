package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;



/**
 * Part of DM 4.5
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        // set type "Public" for workspace "DeepaMehta"
        dms.getTopic("uri", new SimpleValue("dm4.workspaces.deepamehta")).update(
            new TopicModel(null, new ChildTopicsModel().putRef("dm4.workspaces.sharing_mode", "dm4.workspaces.public"))
        );
    }
}
