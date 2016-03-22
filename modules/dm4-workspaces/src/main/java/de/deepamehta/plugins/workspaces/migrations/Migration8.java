package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Add index to the workspace property.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.8-SNAPSHOT
 */
public class Migration8 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        dm4.addTopicPropertyIndex(PROP_WORKSPACE_ID);
        dm4.addAssociationPropertyIndex(PROP_WORKSPACE_ID);
    }
}
