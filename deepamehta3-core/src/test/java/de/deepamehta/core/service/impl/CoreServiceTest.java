package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.TopicTypeDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void test() {
        TopicTypeDefinition typeDef = dms.getTopicTypeDefinition("dm3.core.plugin");
        logger.info(typeDef.toString());
        assertEquals("topic type definition 43 \"Plugin\" (uri=\"dm3.core.plugin\", typeUri=\"dm3.core.topic_type\", " +
            "dataTypeUri=\"null\", assocDefs=[\n    association type definition (assocTypeUri=\"null\")\n" +
            "        whole: (type=\"dm3.core.plugin\", role=\"dm3.core.plugin\", cardinality=\"null\")\n" +
            "        part: (type=\"dm3.core.plugin_migration_nr\", role=\"dm3.core.plugin_migration_nr\", " +
            "cardinality=\"dm3.core.one\")])", typeDef.toString());
    }
}
