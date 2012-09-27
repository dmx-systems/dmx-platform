package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void getTopicType() {
        TopicType topicType = dms.getTopicType("dm4.core.plugin", null);  // clientState=null
        assertEquals("dm4.core.plugin",     topicType.getUri());
        assertEquals("dm4.core.topic_type", topicType.getTypeUri());
        assertEquals("dm4.core.composite",  topicType.getDataTypeUri());
        assertEquals(3,                     topicType.getAssocDefs().size());
        AssociationDefinition assocDef =    topicType.getAssocDef("dm4.core.plugin_migration_nr");
        assertEquals("dm4.core.plugin",              assocDef.getWholeTopicTypeUri());
        assertEquals("dm4.core.plugin_migration_nr", assocDef.getPartTopicTypeUri());
        assertEquals("dm4.core.plugin_migration_nr", assocDef.getUri());
        assertEquals("dm4.core.one",                 assocDef.getWholeCardinalityUri());
        assertEquals("dm4.core.one",                 assocDef.getPartCardinalityUri());
        assertEquals("dm4.core.composition_def",     assocDef.getTypeUri());
        assertEquals("dm4.core.whole",               assocDef.getWholeRoleTypeUri());
        assertEquals("dm4.core.part",                assocDef.getPartRoleTypeUri());
        Topic t1 = assocDef.getTopic("dm4.core.whole_type");
        Topic t2 = assocDef.getTopic("dm4.core.part_type");
        assertEquals("dm4.core.plugin",              t1.getUri());
        assertEquals("dm4.core.topic_type",          t1.getTypeUri());
        assertEquals("dm4.core.plugin_migration_nr", t2.getUri());
        assertEquals("dm4.core.topic_type",          t2.getTypeUri());
    }

    @Test
    public void createWithoutComposite() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("de.deepamehta.notes", "dm4.core.plugin",
                new SimpleValue("DeepaMehta 4 Notes")), null);  // clientState=null
            //
            topic.setChildTopicValue("dm4.core.plugin_migration_nr", new SimpleValue(23));
            //
            int nr = topic.getChildTopicValue("dm4.core.plugin_migration_nr").intValue();
            assertEquals(23, nr);
            //
            topic.setChildTopicValue("dm4.core.plugin_migration_nr", new SimpleValue(42));
            //
            nr = topic.getChildTopicValue("dm4.core.plugin_migration_nr").intValue();
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
            Topic topic = dms.createTopic(new TopicModel("de.deepamehta.notes", "dm4.core.plugin",
                new CompositeValue().put("dm4.core.plugin_migration_nr", 23)), null);
            //
            assertTrue(topic.getCompositeValue().has("dm4.core.plugin_migration_nr"));
            //
            int nr = topic.getChildTopicValue("dm4.core.plugin_migration_nr").intValue();
            assertEquals(23, nr);
            //
            topic.setChildTopicValue("dm4.core.plugin_migration_nr", new SimpleValue(42));
            //
            nr = topic.getChildTopicValue("dm4.core.plugin_migration_nr").intValue();
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
