package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
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
import java.util.List;



public class CoreServiceTest extends CoreServiceTestEnvironment {

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
    public void assocDefSequence() {
        Type type = dms.getTopicType("dm4.core.plugin");
        //
        // find assoc def 1/3
        RelatedAssociation assocDef = type.getRelatedAssociation("dm4.core.aggregation", "dm4.core.type",
            "dm4.core.sequence_start", null);     // othersAssocTypeUri=null
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
        List<RelatedAssociation> assocs;
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.instantiation");
        assertEquals(48, assocs.size());
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.composition_def");
        assertEquals(5, assocs.size());
        //
        assocs = getAssociationInstancesByTraversal("dm4.core.aggregation_def");
        assertEquals(0, assocs.size());
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
            Association assoc = childTypes.getItems().get(0).getRelatingAssociation();
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
            Association assoc = childTypes.getItems().get(0).getRelatingAssociation();
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
            Topic topic = topics.getItems().get(0);
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
        List<RelatedAssociation> assocs;
        try {
            setupTestAssociations();
            //
            t0 = getTopicByUri("dm4.test.t0");
            //
            // execute query
            assocs = getTestAssociations(t0);
            assertEquals(3, assocs.size());     // we have 3 associations
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
            assertEquals(2, assocs.size());     // now we have 2 associations
            // ### Note: the Lucene index update *is* visible within the transaction *if* the test content is
            // ### created within the same transaction!
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        assocs = getTestAssociations(t0);
        assertEquals(2, assocs.size());         // we still have 2 associations
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
            Topic topic = topics.getItems().get(0);
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

    private List<RelatedAssociation> getAssociationInstancesByTraversal(String assocTypeUri) {
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

    private List<RelatedAssociation> getTestAssociations(Topic topic) {
        return topic.getRelatedAssociations("dm4.core.association",
            "dm4.core.default", "dm4.core.default", "dm4.core.association");
    }
}
