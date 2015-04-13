package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ResultList;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Test
    public void typeDefinition() {
        TopicType topicType = dms.getTopicType("dm4.core.plugin");
        assertEquals("dm4.core.plugin",     topicType.getUri());
        assertEquals("dm4.core.topic_type", topicType.getTypeUri());
        assertEquals("dm4.core.composite",  topicType.getDataTypeUri());
        assertEquals(3,                     topicType.getAssocDefs().size());
        AssociationDefinition assocDef =    topicType.getAssocDef("dm4.core.plugin_migration_nr");
        assertEquals("dm4.core.composition_def",     assocDef.getTypeUri());
        assertEquals("dm4.core.plugin",              assocDef.getParentTypeUri());
        assertEquals("dm4.core.plugin_migration_nr", assocDef.getChildTypeUri());
        assertEquals("dm4.core.one",                 assocDef.getParentCardinalityUri());
        assertEquals("dm4.core.one",                 assocDef.getChildCardinalityUri());
        Topic t1 = assocDef.getTopic("dm4.core.parent_type");
        Topic t2 = assocDef.getTopic("dm4.core.child_type");
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
                new SimpleValue("DeepaMehta 4 Notes")));
            //
            topic.getChildTopics().set("dm4.core.plugin_migration_nr", 23);
            //
            int nr = topic.getChildTopics().getTopic("dm4.core.plugin_migration_nr").getSimpleValue().intValue();
            assertEquals(23, nr);
            //
            topic.getChildTopics().set("dm4.core.plugin_migration_nr", 42);
            //
            nr = topic.getChildTopics().getTopic("dm4.core.plugin_migration_nr").getSimpleValue().intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void createWithComposite() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("de.deepamehta.notes", "dm4.core.plugin",
                new ChildTopicsModel().put("dm4.core.plugin_migration_nr", 23)
            ));
            //
            assertTrue(topic.getChildTopics().has("dm4.core.plugin_migration_nr"));
            //
            int nr = topic.getChildTopics().getTopic("dm4.core.plugin_migration_nr").getSimpleValue().intValue();
            assertEquals(23, nr);
            //
            topic.getChildTopics().set("dm4.core.plugin_migration_nr", 42);
            //
            nr = topic.getChildTopics().getTopic("dm4.core.plugin_migration_nr").getSimpleValue().intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void onDemandChildTopicLoading() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            dms.createTopic(new TopicModel("de.deepamehta.notes", "dm4.core.plugin",
                new ChildTopicsModel().put("dm4.core.plugin_migration_nr", 23)
            ));
            //
            Topic topic = dms.getTopic("uri", new SimpleValue("de.deepamehta.notes"));
            ChildTopics comp = topic.getChildTopics();
            assertFalse(comp.has("dm4.core.plugin_migration_nr"));              // child topic is not yet loaded
            //
            Topic childTopic = comp.getTopic("dm4.core.plugin_migration_nr");
            assertEquals(23, childTopic.getSimpleValue().intValue());           // child topic is loaded on-demand
            assertTrue(comp.has("dm4.core.plugin_migration_nr"));               // child topic is now loaded
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void onDemandChildTopicLoadingWithConvenienceAccessor() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            dms.createTopic(new TopicModel("de.deepamehta.notes", "dm4.core.plugin",
                new ChildTopicsModel().put("dm4.core.plugin_migration_nr", 23)
            ));
            //
            Topic topic = dms.getTopic("uri", new SimpleValue("de.deepamehta.notes"));
            ChildTopics comp = topic.getChildTopics();
            assertFalse(comp.has("dm4.core.plugin_migration_nr"));              // child topic is not yet loaded
            //
            assertEquals(23, comp.getInt("dm4.core.plugin_migration_nr"));      // child topic is loaded on-demand
            assertTrue(comp.has("dm4.core.plugin_migration_nr"));               // child topic is now loaded
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void changeLabelWithSetChildTopics() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("dm4.core.plugin"));
            assertEquals("", topic.getSimpleValue().toString());
            //
            topic.setChildTopics(new ChildTopicsModel().put("dm4.core.plugin_name", "My Plugin"));
            assertEquals("My Plugin", topic.getChildTopics().getString("dm4.core.plugin_name"));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            Topic fetchedTopic = dms.getTopic(topic.getId());
            assertEquals("My Plugin", fetchedTopic.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void changeLabelWithChildTopicsSet() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("dm4.core.plugin"));
            assertEquals("", topic.getSimpleValue().toString());
            //
            topic.getChildTopics().set("dm4.core.plugin_name", "My Plugin");
            assertEquals("My Plugin", topic.getChildTopics().getString("dm4.core.plugin_name"));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            Topic fetchedTopic = dms.getTopic(topic.getId());
            assertEquals("My Plugin", fetchedTopic.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void setLabelChildWhileChildsAreNotLoaded() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("dm4.core.plugin", new ChildTopicsModel()
                .put("dm4.core.plugin_name", "My Plugin")
                .put("dm4.core.plugin_symbolic_name", "dm4.test.my_plugin")
                .put("dm4.core.plugin_migration_nr", 1)
            ));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            topic = dms.getTopic(topic.getId());                            // Note: the childs are not loaded
            assertEquals("My Plugin", topic.getSimpleValue().toString());   // the label is intact
            topic.getChildTopics().set("dm4.core.plugin_name", "HuHu");     // setting child used for labeling
            assertEquals("HuHu", topic.getSimpleValue().toString());        // the label is recalculated
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void setNonlabelChildWhileChildsAreNotLoaded() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic topic = dms.createTopic(new TopicModel("dm4.core.plugin", new ChildTopicsModel()
                .put("dm4.core.plugin_name", "My Plugin")
                .put("dm4.core.plugin_symbolic_name", "dm4.test.my_plugin")
                .put("dm4.core.plugin_migration_nr", 1)
            ));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            topic = dms.getTopic(topic.getId());                            // Note: the childs are not loaded
            assertEquals("My Plugin", topic.getSimpleValue().toString());   // the label is intact
            topic.getChildTopics().set("dm4.core.plugin_migration_nr", 3);  // setting child NOT used for labeling
            assertEquals("My Plugin", topic.getSimpleValue().toString());   // the label is still intact
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void assocDefSequence() {
        Type type = dms.getTopicType("dm4.core.plugin");
        //
        // find assoc def 1/3
        RelatedAssociation assocDef = type.getRelatedAssociation("dm4.core.aggregation", "dm4.core.type",
            "dm4.core.sequence_start", null);   // othersAssocTypeUri=null
        logger.info("### assoc def ID 1/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // find assoc def 2/3
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor",
            null);                              // othersAssocTypeUri=null
        logger.info("### assoc def ID 2/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // find assoc def 3/3
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor",
            null);                              // othersAssocTypeUri=null
        logger.info("### assoc def ID 3/3 = " + assocDef.getId() +
            ", relating assoc ID = " + assocDef.getRelatingAssociation().getId());
        assertNotNull(assocDef);
        //
        // there is no other
        assocDef = assocDef.getRelatedAssociation("dm4.core.sequence", "dm4.core.predecessor", "dm4.core.successor",
            null);                              // othersAssocTypeUri=null
        assertNull(assocDef);
    }

    // ---

    @Test
    public void insertAssocDefAtPos0() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Type type = dms.getTopicType("dm4.core.plugin");
            //
            dms.createTopicType(new TopicTypeModel("dm4.test.name", "Name", "dm4.core.text"));
            // insert assoc def at pos 0
            type.addAssocDefBefore(new AssociationDefinitionModel("dm4.core.composition_def",
                "dm4.core.plugin", "dm4.test.name", "dm4.core.one", "dm4.core.one"), "dm4.core.plugin_name");
            //
            Collection<AssociationDefinition> assocDefs = type.getAssocDefs();
            assertSame(4, assocDefs.size());
            //
            Iterator<AssociationDefinition> i = assocDefs.iterator();
            assertEquals("dm4.test.name", i.next().getChildTypeUri());          // new assoc def is at pos 0
            assertEquals("dm4.core.plugin_name", i.next().getChildTypeUri());   // former pos 0 is now at pos 1
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void insertAssocDefAtPos1() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Type type = dms.getTopicType("dm4.core.plugin");
            //
            dms.createTopicType(new TopicTypeModel("dm4.test.name", "Name", "dm4.core.text"));
            // insert assoc def at pos 1
            type.addAssocDefBefore(new AssociationDefinitionModel("dm4.core.composition_def",
                "dm4.core.plugin", "dm4.test.name", "dm4.core.one", "dm4.core.one"), "dm4.core.plugin_symbolic_name");
            //
            Collection<AssociationDefinition> assocDefs = type.getAssocDefs();
            assertSame(4, assocDefs.size());
            //
            Iterator<AssociationDefinition> i = assocDefs.iterator();
            assertEquals("dm4.core.plugin_name", i.next().getChildTypeUri());           // pos 0 is unchanged
            assertEquals("dm4.test.name", i.next().getChildTypeUri());                  // new assoc def is at pos 1
            assertEquals("dm4.core.plugin_symbolic_name", i.next().getChildTypeUri());  // former pos 1 is now at pos 2
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void getTopicsByType() { 
        Topic type = getTopicByUri("dm4.core.data_type");
        ResultList<RelatedTopic> topics1 = getTopicInstancesByTraversal(type);
        assertEquals(5, topics1.getSize());
        List<Topic> topics2 = getTopicInstances("dm4.core.data_type");
        assertEquals(5, topics2.size());
    }

    @Test
    public void getAssociationsByType() { 
        ResultList<RelatedAssociation> assocs;
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.instantiation");
        assertEquals(49, assocs.getSize());
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.composition_def");
        assertEquals(5, assocs.getSize());
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.aggregation_def");
        assertEquals(2, assocs.getSize());
    }

    // ---

    @Test
    public void retypeAssociation() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic type;
        ResultList<RelatedTopic> childTypes;
        try {
            type = getTopicByUri("dm4.core.plugin");
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.getSize());
            //
            // retype assoc
            Association assoc = childTypes.get(0).getRelatingAssociation();
            assertEquals("dm4.core.composition_def", assoc.getTypeUri());
            assoc.setTypeUri("dm4.core.association");
            assertEquals("dm4.core.association", assoc.getTypeUri());
            assoc = dms.getAssociation(assoc.getId());
            assertEquals("dm4.core.association", assoc.getTypeUri());
            //
            // re-execute query
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.getSize());
            // ### Note: the Lucene index update is not visible within the transaction!
            // ### That's contradictory to the Neo4j documentation!
            // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        childTypes = getChildTypes(type);
        assertEquals(2, childTypes.getSize());
        // ### Note: the Lucene index update is only visible once the transaction is committed!
        // ### That's contradictory to the Neo4j documentation!
        // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
    }

    @Test
    public void retypeAssociationRoles() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic type;
        ResultList<RelatedTopic> childTypes;
        try {
            type = getTopicByUri("dm4.core.plugin");
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.getSize());
            //
            // retype assoc roles
            Association assoc = childTypes.get(0).getRelatingAssociation();
            assoc.getRole1().setRoleTypeUri("dm4.core.default");
            assoc.getRole2().setRoleTypeUri("dm4.core.default");
            //
            // re-execute query
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.getSize());
            // ### Note: the Lucene index update is not visible within the transaction!
            // ### That's contradictory to the Neo4j documentation!
            // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        childTypes = getChildTypes(type);
        assertEquals(2, childTypes.getSize());
        // ### Note: the Lucene index update is only visible once the transaction is committed!
        // ### That's contradictory to the Neo4j documentation!
        // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
    }

    @Test
    public void retypeTopicAndTraverse() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic t0;
        ResultList<RelatedTopic> topics;
        try {
            setupTestTopics();
            //
            t0 = getTopicByUri("dm4.test.t0");
            //
            // execute query
            topics = getTestTopics(t0);
            assertEquals(3, topics.getSize());  // we have 3 topics
            //
            // retype the first topic
            Topic topic = topics.get(0);
            assertEquals("dm4.core.plugin", topic.getTypeUri());
            topic.setTypeUri("dm4.core.data_type");
            assertEquals("dm4.core.data_type", topic.getTypeUri());
            topic = dms.getTopic(topic.getId());
            assertEquals("dm4.core.data_type", topic.getTypeUri());
            //
            // re-execute query
            topics = getTestTopics(t0);
            assertEquals(2, topics.getSize());  // now we have 2 topics
            // ### Note: the Lucene index update *is* visible within the transaction *if* the test content is
            // ### created within the same transaction!
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        topics = getTestTopics(t0);
        assertEquals(2, topics.getSize());      // we still have 2 topics
    }

    @Test
    public void retypeAssociationAndTraverse() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic t0;
        ResultList<RelatedAssociation> assocs;
        try {
            setupTestAssociations();
            //
            t0 = getTopicByUri("dm4.test.t0");
            //
            // execute query
            assocs = getTestAssociations(t0);
            assertEquals(3, assocs.getSize());  // we have 3 associations
            //
            // retype the first association
            Association assoc = assocs.get(0);
            assertEquals("dm4.core.association", assoc.getTypeUri());
            assoc.setTypeUri("dm4.core.composition");
            assertEquals("dm4.core.composition", assoc.getTypeUri());
            assoc = dms.getAssociation(assoc.getId());
            assertEquals("dm4.core.composition", assoc.getTypeUri());
            //
            // re-execute query
            assocs = getTestAssociations(t0);
            assertEquals(2, assocs.getSize());  // now we have 2 associations
            // ### Note: the Lucene index update *is* visible within the transaction *if* the test content is
            // ### created within the same transaction!
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        assocs = getTestAssociations(t0);
        assertEquals(2, assocs.getSize());      // we still have 2 associations
    }

    @Test
    public void retypeTopicAndTraverseInstantiations() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic type;
        ResultList<RelatedTopic> topics;
        try {
            type = getTopicByUri("dm4.core.data_type");
            topics = getTopicInstancesByTraversal(type);
            assertEquals(5, topics.getSize());
            //
            // retype topic
            Topic topic = topics.get(0);
            assertEquals("dm4.core.data_type", topic.getTypeUri());
            topic.setTypeUri("dm4.core.index_mode");
            assertEquals("dm4.core.index_mode", topic.getTypeUri());
            topic = dms.getTopic(topic.getId());
            assertEquals("dm4.core.index_mode", topic.getTypeUri());
            //
            // re-execute query
            topics = getTopicInstancesByTraversal(type);
            assertEquals(4, topics.getSize());
            // ### Note: in contrast to the above 4 tests this time the Lucene index update *is* visible
            // ### within the transaction! This suggests the following hypothesis:
            // ###     index.remove(entity) operation *is* visible within the transaction
            // ###     index.remove(entity, key) operation is *not* visible within the transaction
            // ### For the moment this seems to be a Neo4j oddity. Needs to be confirmed.
            //
            // ### Update: meanwhile that hypothesis is falsified.
            // ### Actually the latter 3 test are in contrast to the former 2 ones.
            // ### One possible difference is whether the test content is created in the same transaction or not.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        topics = getTopicInstancesByTraversal(type);
        assertEquals(4, topics.getSize());
        // ### Note: the Lucene index update was already visible within the transaction!
    }

    // ---

    @Test
    public void updateAggregationOne() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic comp1, item1, item2;
        try {
            // 1) define composite type
            // child types
            dms.createTopicType(new TopicTypeModel("dm4.test.name", "Name", "dm4.core.text"));
            dms.createTopicType(new TopicTypeModel("dm4.test.item", "Item", "dm4.core.text"));
            // parent type
            dms.createTopicType(new TopicTypeModel("dm4.test.composite", "Composite", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def",
                    "dm4.test.composite", "dm4.test.name", "dm4.core.one", "dm4.core.one"
                ))
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.composite", "dm4.test.item", "dm4.core.many", "dm4.core.one"
                ))
            );
            // 2) create example child instances
            item1 = dms.createTopic(new TopicModel("dm4.test.item", new SimpleValue("Item 1")));
            item2 = dms.createTopic(new TopicModel("dm4.test.item", new SimpleValue("Item 2")));
            // 3) create composite instance
            comp1 = dms.createTopic(new TopicModel("dm4.test.composite", new ChildTopicsModel()
                .put("dm4.test.name", "Composite 1")
                // ### .putRef("dm4.test.item", item1.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        // check memory
        assertEquals("Composite 1", comp1.getChildTopics().getString("dm4.test.name"));
        assertFalse(                comp1.getChildTopics().has("dm4.test.item"));
        comp1.loadChildTopics();
        assertFalse(                comp1.getChildTopics().has("dm4.test.item"));
        assertEquals(2, dms.getTopics("dm4.test.item", 0).getSize());
        //
        // update and check again
        tx = dms.beginTx();
        try {
            comp1.update(new TopicModel(comp1.getId(), new ChildTopicsModel()
                .putRef("dm4.test.item", item2.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertEquals("Composite 1", comp1.getChildTopics().getString("dm4.test.name"));
        assertTrue(                 comp1.getChildTopics().has("dm4.test.item"));
        assertEquals("Item 2",      comp1.getChildTopics().getString("dm4.test.item"));
        assertEquals(item2.getId(), comp1.getChildTopics().getTopic("dm4.test.item").getId());
        assertEquals(2, dms.getTopics("dm4.test.item", 0).getSize());
        //
        // update and check again
        tx = dms.beginTx();
        try {
            comp1.update(new TopicModel(comp1.getId(), new ChildTopicsModel()
                .putRef("dm4.test.item", item1.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertEquals("Composite 1", comp1.getChildTopics().getString("dm4.test.name"));
        assertTrue(                 comp1.getChildTopics().has("dm4.test.item"));
        assertEquals("Item 1",      comp1.getChildTopics().getString("dm4.test.item"));
        assertEquals(item1.getId(), comp1.getChildTopics().getTopic("dm4.test.item").getId());
        assertEquals(2, dms.getTopics("dm4.test.item", 0).getSize());
    }

    @Test
    public void updateAggregationOneFacet() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic name, item1, item2;
        try {
            // 1) define facet
            dms.createTopicType(new TopicTypeModel("dm4.test.item", "Item", "dm4.core.text"));
            dms.createTopicType(new TopicTypeModel("dm4.test.item_facet", "Item Facet", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.item_facet", "dm4.test.item", "dm4.core.many", "dm4.core.one"
                ))
            );
            // 2) create example facet values
            item1 = dms.createTopic(new TopicModel("dm4.test.item", new SimpleValue("Item 1")));
            item2 = dms.createTopic(new TopicModel("dm4.test.item", new SimpleValue("Item 2")));
            // 3) define simple type + instance
            dms.createTopicType(new TopicTypeModel("dm4.test.name", "Name", "dm4.core.text"));
            name = dms.createTopic(new TopicModel("dm4.test.name", new SimpleValue("Name 1")));
            //
            tx.success();
        } finally {
            tx.finish();
        }
        //
        AssociationDefinition assocDef = dms.getTopicType("dm4.test.item_facet").getAssocDef("dm4.test.item");
        //
        // update facet
        tx = dms.beginTx();
        try {
            name.updateChildTopic(new TopicReferenceModel(item1.getId()), assocDef);
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertTrue(                 name.getChildTopics().has("dm4.test.item"));
        Topic facetValue = (Topic)  name.getChildTopics().get("dm4.test.item");
        assertEquals("Item 1",      facetValue.getSimpleValue().toString());
        assertEquals(item1.getId(), facetValue.getId());
        assertEquals(2, dms.getTopics("dm4.test.item", 0).getSize());
        //
        // update facet again
        tx = dms.beginTx();
        try {
            name.updateChildTopic(new TopicReferenceModel(item2.getId()), assocDef);
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertTrue(                 name.getChildTopics().has("dm4.test.item"));
        facetValue = (Topic)        name.getChildTopics().get("dm4.test.item");
        assertEquals("Item 2",      facetValue.getSimpleValue().toString());
        assertEquals(item2.getId(), facetValue.getId());
        assertEquals(2, dms.getTopics("dm4.test.item", 0).getSize());
    }

    // ---

    @Test
    public void createManyChildRefViaModel() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic parent1, child1;
        try {
            // 1) define composite type
            // child type
            dms.createTopicType(new TopicTypeModel("dm4.test.child", "Child", "dm4.core.text"));
            // parent type
            dms.createTopicType(new TopicTypeModel("dm4.test.parent", "Parent", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.parent", "dm4.test.child", "dm4.core.many", "dm4.core.many"
                ))
            );
            // 2) create example child instance
            child1 = dms.createTopic(new TopicModel("dm4.test.child", new SimpleValue("Child 1")));
            // 3) create composite instance
            // Note: addRef() must be used (instead of putRef()) as child is defined as "many".
            parent1 = dms.createTopic(new TopicModel("dm4.test.parent", new ChildTopicsModel()
                .addRef("dm4.test.child", child1.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> childs = parent1.getChildTopics().getTopics("dm4.test.child");
        assertEquals(1, childs.size());
        assertEquals(child1.getId(), childs.get(0).getId());
        assertEquals("Child 1", childs.get(0).getSimpleValue().toString());
    }

    @Test
    public void createManyChildRefViaObject() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic parent1, child1;
        try {
            // 1) define parent type (with Aggregation-Many child definition)
            dms.createTopicType(new TopicTypeModel("dm4.test.child", "Child", "dm4.core.text"));
            dms.createTopicType(new TopicTypeModel("dm4.test.parent", "Parent", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.parent", "dm4.test.child", "dm4.core.many", "dm4.core.many"
                ))
            );
            // 2) create child instance
            child1 = dms.createTopic(new TopicModel("dm4.test.child", new SimpleValue("Child 1")));
            // 3) create composite instance
            parent1 = dms.createTopic(new TopicModel("dm4.test.parent"));
            parent1.getChildTopics().addRef("dm4.test.child", child1.getId());
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> childs = parent1.getChildTopics().getTopics("dm4.test.child");
        assertEquals(1, childs.size());
        assertEquals(child1.getId(), childs.get(0).getId());
        assertEquals("Child 1", childs.get(0).getSimpleValue().toString());
    }

    @Test
    public void createManyChildViaObject() {
        DeepaMehtaTransaction tx = dms.beginTx();
        Topic parent1;
        try {
            // 1) define composite type
            // child type
            dms.createTopicType(new TopicTypeModel("dm4.test.child", "Child", "dm4.core.text"));
            // parent type
            dms.createTopicType(new TopicTypeModel("dm4.test.parent", "Parent", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.parent", "dm4.test.child", "dm4.core.many", "dm4.core.many"
                ))
            );
            // 2) create composite instance
            parent1 = dms.createTopic(new TopicModel("dm4.test.parent"));
            parent1.getChildTopics().add("dm4.test.child", "Child 1");
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> childs = parent1.getChildTopics().getTopics("dm4.test.child");
        assertEquals(1, childs.size());
        assertEquals("Child 1", childs.get(0).getSimpleValue().toString());
    }

    // ---

    @Test
    public void createAndUpdateAggregationOne() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) define parent type (with Aggregation-One child definition)
            dms.createTopicType(new TopicTypeModel("dm4.test.child", "Child", "dm4.core.text"));
            dms.createTopicType(new TopicTypeModel("dm4.test.parent", "Parent", "dm4.core.composite")
                .addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    "dm4.test.parent", "dm4.test.child", "dm4.core.many", "dm4.core.one"
                ))
            );
            // 2) create parent instance
            Topic parent1 = dms.createTopic(new TopicModel("dm4.test.parent", new ChildTopicsModel()
                .put("dm4.test.child", "Child 1")
            ));
            //
            assertEquals("Child 1", parent1.getChildTopics().getTopic("dm4.test.child").getSimpleValue().toString());
            // 3) update child topics
            parent1.getChildTopics().set("dm4.test.child", "Child 2");
            //
            assertEquals("Child 2", parent1.getChildTopics().getTopic("dm4.test.child").getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void deleteTopic() {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            dms.createTopic(new TopicModel("dm4.test.t0", "dm4.core.plugin"));
            //
            Topic t0 = getTopicByUri("dm4.test.t0");
            assertNotNull(t0);
            //
            t0.delete();
            t0 = getTopicByUri("dm4.test.t0");
            assertNull(t0);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic getTopicByUri(String uri) {
        return dms.getTopic("uri", new SimpleValue(uri));
    }

    private List<Topic> getTopicInstances(String topicTypeUri) {
        return dms.getTopics("type_uri", new SimpleValue(topicTypeUri));
    }

    private ResultList<RelatedTopic> getTopicInstancesByTraversal(Topic type) {
        return type.getRelatedTopics("dm4.core.instantiation",
            "dm4.core.type", "dm4.core.instance", type.getUri(), 0);
    }

    private ResultList<RelatedAssociation> getAssociationInstancesByTraversal(String assocTypeUri) {
        return getTopicByUri(assocTypeUri).getRelatedAssociations("dm4.core.instantiation",
            "dm4.core.type", "dm4.core.instance", assocTypeUri);
    }

    private ResultList<RelatedTopic> getChildTypes(Topic type) {
        return type.getRelatedTopics(asList("dm4.core.aggregation_def", "dm4.core.composition_def"),
            "dm4.core.parent_type", "dm4.core.child_type", "dm4.core.topic_type", 0);
    }

    // ---

    private void setupTestTopics() {
        Topic t0 = dms.createTopic(new TopicModel("dm4.test.t0", "dm4.core.plugin"));
        Topic t1 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Topic t2 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Topic t3 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        createAssociation(t0, t1);
        createAssociation(t0, t2);
        createAssociation(t0, t3);
    }

    private void setupTestAssociations() {
        Topic t0 = dms.createTopic(new TopicModel("dm4.test.t0", "dm4.core.plugin"));
        Topic t1 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Topic t2 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Topic t3 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Topic t4 = dms.createTopic(new TopicModel("dm4.core.plugin"));
        Association a1 = createAssociation(t1, t2);
        Association a2 = createAssociation(t2, t3);
        Association a3 = createAssociation(t3, t4);
        createAssociation(t0, a1);
        createAssociation(t0, a2);
        createAssociation(t0, a3);
    }

    // ---

    private Association createAssociation(Topic topic1, Topic topic2) {
        return dms.createAssociation(new AssociationModel("dm4.core.association",
            new TopicRoleModel(topic1.getId(), "dm4.core.default"),
            new TopicRoleModel(topic2.getId(), "dm4.core.default")
        ));
    }

    private Association createAssociation(Topic topic, Association assoc) {
        return dms.createAssociation(new AssociationModel("dm4.core.association",
            new TopicRoleModel(topic.getId(), "dm4.core.default"),
            new AssociationRoleModel(assoc.getId(), "dm4.core.default")
        ));
    }

    // ---

    private ResultList<RelatedTopic> getTestTopics(Topic topic) {
        return topic.getRelatedTopics("dm4.core.association",
            "dm4.core.default", "dm4.core.default", "dm4.core.plugin", 0);
    }

    private ResultList<RelatedAssociation> getTestAssociations(Topic topic) {
        return topic.getRelatedAssociations("dm4.core.association",
            "dm4.core.default", "dm4.core.default", "dm4.core.association");
    }
}
