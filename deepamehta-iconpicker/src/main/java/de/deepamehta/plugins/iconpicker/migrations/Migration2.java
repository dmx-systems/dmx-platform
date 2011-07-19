package de.deepamehta.plugins.iconpicker.migrations;

import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Distributed with deepamehta3-iconpicker 0.4.1.
 * <p>
 * Note: in deepamehta3-iconpicker 0.4 this was part of migration 1 (along with a declarative part).
 * In deepamehta3-iconpicker 0.4.1 this imperative part was separated as migration 2.
 * (Since deepamehta3-core 0.4.1 a migration can't have both anymore, a declarative and an imperative part.)
 * So, when updating from deepamehta3-iconpicker 0.4 to 0.4.1 this part has been run already.
 * This migration is set to run only while a clean install of deepamehta3-iconpicker 0.4.1.
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        createIconTopics();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createIconTopics() {
        for (String typeURI : dms.getTopicTypeUris()) {
            logger.fine("### Handling icon topic for type " + typeURI + " ...");
            TopicType type = dms.getTopicType(typeURI, null);   // clientContext=null
            String iconSrc = type.getProperty("icon_src", null).toString();
            if (iconSrc == null) {
                logger.fine("  # Type has no icon_src declaration -> no icon topic needed");
                continue;
            } else {
                logger.fine("  # Type has icon_src declaration: \"" + iconSrc + "\" -> icon topic is needed");
            }
            // create icon topic
            Topic iconTopic = getIconTopic(iconSrc);
            if (iconTopic == null) {
                iconTopic = createIconTopic(iconSrc);
                logger.fine("  # Creating icon topic for that source -> ID=" + iconTopic.id);
            } else {
                logger.fine("  # Icon topic for that source exists already (ID=" + iconTopic.id + ")");
            }
            // relate type to icon
            Relation relation = dms.getRelation(type.id, iconTopic.id, "RELATION", true);
            if (relation == null) {
                relateTypeToIcon(type.id, iconTopic.id);
                logger.fine("  # Creating relation between type and icon topic");
            } else {
                logger.fine("  # Relation between type and icon topic exists already");
            }
        }
    }

    private Topic getIconTopic(String iconSrc) {
        return dms.getTopic("de/deepamehta/core/property/IconSource", new PropValue(iconSrc));
    }

    private Topic createIconTopic(String iconSrc) {
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/IconSource", iconSrc);
        return dms.createTopic("de/deepamehta/core/topictype/Icon", properties, null);
    }

    private void relateTypeToIcon(long typeId, long iconId) {
        dms.createRelation("RELATION", typeId, iconId, null);
    }
}
