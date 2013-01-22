package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    @Test
    public void typeDefinition() {
        TopicType topicType = dms.getTopicType("dm4.core.plugin", null);  // clientState=null
        assertEquals("dm4.core.plugin",     topicType.getUri());
        assertEquals("dm4.core.topic_type", topicType.getTypeUri());
        assertEquals("dm4.core.composite",  topicType.getDataTypeUri());
        assertEquals(3,                     topicType.getAssocDefs().size());
        AssociationDefinition assocDef =    topicType.getAssocDef("dm4.core.plugin_migration_nr");
        assertEquals("dm4.core.composition_def",     assocDef.getTypeUri());
        assertEquals("dm4.core.plugin",              assocDef.getWholeTypeUri());
        assertEquals("dm4.core.plugin_migration_nr", assocDef.getPartTypeUri());
        assertEquals("dm4.core.one",                 assocDef.getWholeCardinalityUri());
        assertEquals("dm4.core.one",                 assocDef.getPartCardinalityUri());
        assertEquals("dm4.core.whole",               assocDef.getWholeRoleTypeUri());   // ### TODO: drop this
        assertEquals("dm4.core.part",                assocDef.getPartRoleTypeUri());    // ### TODO: drop this
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

    @Test
    public void assocDefSequence() {
        Type type = dms.getTopicType("dm4.core.plugin", null);  // clientState=null
        //
        // find assoc def 1/3
        RelatedAssociation assocDef = type.getRelatedAssociation("dm4.core.aggregation", "dm4.core.type",
            "dm4.core.sequence_start", null, false, false);     // othersAssocTypeUri=null
        logger.info("### assoc def ID 1/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // find assoc def 2/3
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor");
        logger.info("### assoc def ID 2/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // find assoc def 3/3
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor");
        logger.info("### assoc def ID 3/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // there is no other
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor");
        assertNull(assocDef);
    }

    @Test
    @Ignore     // ### TODO
    public void retypeAssociation() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic type = dms.getTopic("uri", new SimpleValue("dm4.core.plugin"), false, null);
            ResultSet<RelatedTopic> partTypes = type.getRelatedTopics(asList("dm4.core.aggregation_def",
                "dm4.core.composition_def"), "dm4.core.whole_type", "dm4.core.part_type", null, false, false, 0, null);
            assertEquals(3, partTypes.getSize());
            //
            // invalidate assoc
            Association assoc = partTypes.getIterator().next().getRelatingAssociation();
            assoc.setTypeUri("dm4.core.association");
            //
            // re-execute query
            partTypes = type.getRelatedTopics(asList("dm4.core.aggregation_def",
                "dm4.core.composition_def"), "dm4.core.whole_type", "dm4.core.part_type", null, false, false, 0, null);
            assertEquals(2, partTypes.getSize());
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Test retypeAssociation() failed", e);
        } finally {
            tx.finish();
        }
    }
}
