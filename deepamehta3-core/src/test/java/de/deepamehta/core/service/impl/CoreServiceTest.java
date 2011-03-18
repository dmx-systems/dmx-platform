package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void topicTypeDef() {
        TopicTypeDefinition typeDef = dms.getTopicTypeDefinition("dm3.core.plugin");
        logger.info(typeDef.toString());
        assertEquals("topic type definition 43 \"Plugin\" (uri=\"dm3.core.plugin\", typeUri=\"dm3.core.topic_type\", " +
            "dataTypeUri=\"null\", assocDefs={dm3.core.plugin_migration_nr=\n    association definition " +
            "(assocTypeUri=\"null\")\n        whole: (type=\"dm3.core.plugin\", role=\"dm3.core.plugin\", " +
            "cardinality=\"null\")\n        part: (type=\"dm3.core.plugin_migration_nr\", " +
            "role=\"dm3.core.plugin_migration_nr\", cardinality=\"dm3.core.one\")})", typeDef.toString());
    }

    @Test
    public void setValue() {
        Topic topic = dms.createTopic(new TopicData("de.deepamehta.3-notes", "DeepaMehta 3 Notes", "dm3.core.plugin",
            new Composite("{dm3.core.plugin_migration_nr: 0}")), null);     // FIXME: clientContext=null
        topic.setValue("dm3.core.plugin_migration_nr", 23);
        //
        int nr = (Integer) topic.getValue("dm3.core.plugin_migration_nr");
        assertEquals(23, nr);
        //
        topic.setValue("dm3.core.plugin_migration_nr", 42);
        //
        nr = (Integer) topic.getValue("dm3.core.plugin_migration_nr");
        assertEquals(42, nr);
    }
}
