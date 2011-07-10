package de.deepamehta.core.impl.service;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void getTopicType() {
        TopicType topicType = dms.getTopicType("dm3.core.plugin", null);  // clientContext=null
        logger.info(topicType.toString());
        assertTrue(topicType.toString().matches("topic type \\(id=\\d+, uri=\"dm3.core.plugin\", value=Plugin, " +
            "typeUri=\"dm3.core.topic_type\", dataTypeUri=\"dm3.core.composite\", indexModes=\\[\\], assocDefs=" +
            "\\{dm3.core.plugin_migration_nr=\n    association definition \\(association \\(id=\\d+, uri=\"dm3." +
            "core.plugin_migration_nr\", value=, typeUri=\"dm3.core.composition_def\", composite=\\{\\}, roleModel1=" +
            "\n        topic role \\(roleTypeUri=\"dm3.core.whole_topic_type\", topicId=-1, topicUri=\"dm3.core." +
            "plugin\", topicIdentifiedByUri=true\\), roleModel2=\n        topic role \\(roleTypeUri=\"dm3.core.part_" +
            "topic_type\", topicId=-1, topicUri=\"dm3.core.plugin_migration_nr\", topicIdentifiedByUri=true\\)\\)\\)" +
            "\n        pos 1: \\(type=\"dm3.core.plugin\", role=\"dm3.core.whole\", cardinality=\"dm3.core.one\"\\)\n" +
            "        pos 2: \\(type=\"dm3.core.plugin_migration_nr\", role=\"dm3.core.part\", cardinality=\"dm3.core." +
            "one\"\\)\n        association definition view configuration \\{\\}\\},\n    topic type view " +
            "configuration \\{\\}\\)"));
        assertEquals("dm3.core.composite", topicType.getDataTypeUri());
    }

    @Test
    public void createWithoutComposite() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
                "dm3.core.plugin", null), null);    // composite=null, clientContext=null
            //
            topic.setChildTopicValue("dm3.core.plugin_migration_nr", new TopicValue(23));
            //
            int nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
            assertEquals(23, nr);
            //
            topic.setChildTopicValue("dm3.core.plugin_migration_nr", new TopicValue(42));
            //
            nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Test createWithoutComposite() failed", e);
        } finally {
            tx.finish();
        }
    }

    @Test
    public void createWithComposite() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
                "dm3.core.plugin", new Composite("{dm3.core.plugin_migration_nr: 23}")), null);
            //
            assertTrue(topic.getComposite().has("dm3.core.plugin_migration_nr"));
            //
            int nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
            assertEquals(23, nr);
            //
            topic.setChildTopicValue("dm3.core.plugin_migration_nr", new TopicValue(42));
            //
            nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Test createWithComposite() failed", e);
        } finally {
            tx.finish();
        }
    }
}
