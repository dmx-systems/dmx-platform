package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.TopicTypeDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void getTopicTypeDef() {
        TopicTypeDefinition typeDef = dms.getTopicTypeDefinition("dm3.core.plugin");
        logger.info(typeDef.toString());
        assertTrue(typeDef.toString().matches("topic type definition \\d+ \"Plugin\" \\(uri=\"dm3.core.plugin\", " +
            "typeUri=\"dm3.core.topic_type\", dataTypeUri=\"null\", assocDefs=\\{dm3.core.plugin_migration_nr=\n    " +
            "association definition \\(assocTypeUri=\"null\"\\)\n        whole: \\(type=\"dm3.core.plugin\", role=\"" +
            "dm3.core.plugin\", cardinality=\"null\"\\)\n        part: \\(type=\"dm3.core.plugin_migration_nr\", " +
            "role=\"dm3.core.plugin_migration_nr\", cardinality=\"dm3.core.one\"\\)\\}\\)"));
    }

    @Test
    public void createWithoutComposite() {
        Topic topic = dms.createTopic(new TopicData("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
            "dm3.core.plugin", null), null);    // composite=null, clientContext=null
        //
        topic.setValue("dm3.core.plugin_migration_nr", new TopicValue(23));
        //
        int nr = topic.getValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(23, nr);
        //
        topic.setValue("dm3.core.plugin_migration_nr", new TopicValue(42));
        //
        nr = topic.getValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(42, nr);
    }

    @Test
    public void createWithComposite() {
        Topic topic = dms.createTopic(new TopicData("de.deepamehta.3-notes", new TopicValue("DeepaMehta 3 Notes"),
            "dm3.core.plugin", new Composite("{dm3.core.plugin_migration_nr: 23}")), null);
        //
        int nr = topic.getValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(23, nr);
        //
        topic.setValue("dm3.core.plugin_migration_nr", new TopicValue(42));
        //
        nr = topic.getValue("dm3.core.plugin_migration_nr").intValue();
        assertEquals(42, nr);
    }
}
