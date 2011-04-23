package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.TopicType;

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
        assertTrue(topicType.toString().matches("topic type data \\(id=\\d+, uri=\"dm3.core.plugin\", value=Plugin, " +
            "typeUri=\"dm3.core.topic_type\", dataTypeUri=\"dm3.core.composite\", indexModes=\\[\\], assocDefs=" +
            "\\{dm3.core.plugin_migration_nr=\n    association definition \\(id=\\d+, uri=\"dm3.core.plugin_" +
            "migration_nr\", assocTypeUri=\"dm3.core.composition\"\\)\n        whole: \\(type=\"dm3.core.plugin\", " +
            "role=\"dm3.core.plugin\", cardinality=\"dm3.core.one\"\\)\n        part: \\(type=\"dm3.core.plugin_" +
            "migration_nr\", role=\"dm3.core.plugin_migration_nr\", cardinality=\"dm3.core.one\"\\)\n        " +
            "association definition view configuration \\[\\]\\},\ntopic type view configuration \\[\\]\\)"));
        assertEquals("dm3.core.composite", topicType.getDataTypeUri());
    }

    @Test
    public void createWithoutComposite() {
        Topic topic = dms.createTopic(new TopicData("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
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
    }

    @Test
    public void createWithComposite() {
        Topic topic = dms.createTopic(new TopicData("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
            "dm3.core.plugin", new Composite("{dm3.core.plugin_migration_nr: 23}")), null);
        //
        int nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(23, nr);
        //
        topic.setChildTopicValue("dm3.core.plugin_migration_nr", new TopicValue(42));
        //
        nr = topic.getChildTopicValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(42, nr);
    }
}
