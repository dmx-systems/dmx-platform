package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Sets cardinality of the workspace facet to "one".
 * <p>
 * Part of DM 4.5
 */
public class Migration6 extends Migration {

    @Override
    public void run() {
        dms.getTopicType("dm4.workspaces.workspace_facet").getAssocDef("dm4.workspaces.workspace")
            .setChildCardinalityUri("dm4.core.one");
    }
}
