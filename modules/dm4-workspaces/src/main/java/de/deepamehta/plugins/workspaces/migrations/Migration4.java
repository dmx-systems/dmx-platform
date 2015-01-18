package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Migration;



/**
 * Corrects URI of DeepaMehta workspace: "de." -> "dm4."
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        dms.getTopic("uri", new SimpleValue("de.workspaces.deepamehta")).setUri("dm4.workspaces.deepamehta");
    }
}
