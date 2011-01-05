package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Repairs two data field definitions which were broken in deepamehta3-workspaces v0.4.
 * <p>
 * Note: this migration is distributed with deepamehta3-workspaces v0.4.1. and is set to run only when updating
 * from deepamehta3-workspaces v0.4 to v0.4.1 (see migration2.properties).
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // clientContext=null
        TopicType workspaceType = dms.getTopicType("de/deepamehta/core/topictype/Workspace", null);
        DataField nameField        = workspaceType.getDataField("de/deepamehta/core/property/Name");
        DataField descriptionField = workspaceType.getDataField("de/deepamehta/core/property/Description");
        nameField.setRendererClass("TitleRenderer");
        nameField.setIndexingMode("FULLTEXT");
        descriptionField.setRendererClass("BodyTextRenderer");
        descriptionField.setIndexingMode("FULLTEXT");
    }
}
