package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Distributed with Workspaces plugin v0.4
 */
public class Migration1 extends Migration {

    @Override
    public void run() {
        createWorkspaceTopicType();
    }

    // ---

    private void createWorkspaceTopicType() {
        DataField nameField = new DataField("Name", "text");
        nameField.setUri("de/deepamehta/core/property/Name");
        nameField.setRendererClass("TitleRenderer");
        nameField.setIndexingMode("FULLTEXT");          // Added *after* v0.4. Upadated through migration 2.
        //
        DataField descriptionField = new DataField("Description", "html");
        descriptionField.setUri("de/deepamehta/core/property/Description");
        descriptionField.setRendererClass("BodyTextRenderer");
        descriptionField.setIndexingMode("FULLTEXT");   // Added *after* v0.4. Upadated through migration 2.
        //
        List dataFields = new ArrayList();
        dataFields.add(nameField);
        dataFields.add(descriptionField);
        //
        Map properties = new HashMap();
        properties.put("de/deepamehta/core/property/TypeURI", "de/deepamehta/core/topictype/Workspace");
        properties.put("de/deepamehta/core/property/TypeLabel", "Workspace");
        properties.put("icon_src", "/de.deepamehta.3-workspaces/images/star.png");
        properties.put("js_renderer_class", "PlainDocument");
        //
        dms.createTopicType(properties, dataFields, null);  // clientContext=null
    }
}
