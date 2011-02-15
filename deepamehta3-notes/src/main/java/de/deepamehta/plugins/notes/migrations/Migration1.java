package de.deepamehta.plugins.notes.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * This migration creates the topic type "Note".
 * <p>
 * The deepamehta3-notes plugin was introduced with DeepaMehta 3 v0.4.1.
 * In DeepaMehta 3 v0.4 the topic type "Note" was created by the deepamehta3-core module.
 * <p>
 * Distributed with deepamehta3-notes v0.4.1.
 */
public class Migration1 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Creates the topic type "Note" by reading from file.
     * <p>
     * Note: usually we would do this by a declarative migration (setting up a types1.json file) and set it to run
     * only while a clean install (migration run mode CLEAN_INSTALL).
     * But, this migration always detects a clean install even while an update (from DeepaMehta 3 v0.4 to v0.4.1).
     * This is because the deepamehta3-notes plugin did not exist in DeepaMehta 3 v0.4 (and *is* clean installed in
     * DeepaMehta 3 v0.4.1). When updating from DeepaMehta 3 v0.4 to v0.4.1 "topic type already exists" would be thrown.
     * <p>
     * We solve this situation by using an imperative migration and check if the type already exists.
     */
    @Override
    public void run() {
        Set typeURIs = dms.getTopicTypeUris();
        // create topic type "Note"
        if (!typeURIs.contains("de/deepamehta/core/topictype/Note")) {
            readMigrationFile("/migrations/note.json");
        } else {
            logger.info("Do NOT create topic type \"Note\" -- already exists");
            // update icon_src
            TopicType noteType = dms.getTopicType("de/deepamehta/core/topictype/Note", null);   // clientContext=null
            logger.info("Updating icon_src of topic type \"Note\" (topic " + noteType.id + ")");
            Properties properties = new Properties();
            properties.put("icon_src", "/de.deepamehta.3-notes/images/pencil.png");
            dms.setTopicProperties(noteType.id, properties);
            // update fields
            DataField titleField = noteType.getDataField("de/deepamehta/core/property/Title");
            DataField textField  = noteType.getDataField("de/deepamehta/core/property/Text");
            titleField.setRendererClass("TitleRenderer");
            titleField.setIndexingMode("FULLTEXT");
            textField.setRendererClass("BodyTextRenderer");
            textField.setIndexingMode("FULLTEXT");
        }
    }
}
