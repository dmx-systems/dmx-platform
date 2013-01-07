package de.deepamehta.storage.neo4j;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class Neo4jStorageTest {

    private DeepaMehtaStorage storage;
    private long assocId;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        storage = new Neo4jStorageFactory().createInstance(createTempDirectory("neo4j"));
        setupContent();
    }


    @Test
    public void fetchAssociation() {
        AssociationModel assoc = storage.fetchAssociation(assocId);
        assertNotNull(assoc);
        //
        RoleModel roleModel1 = assoc.getRoleModel("dm4.core.type");
        assertNotNull(roleModel1);
        //
        RoleModel roleModel2 = assoc.getRoleModel("dm4.core.instance");
        assertNotNull(roleModel2);
    }

    @Test
    public void traverse() {
        TopicModel topic = storage.fetchTopic("uri", "dm4.core.data_type");
        assertNotNull(topic);
        //
        Set<RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(), "dm4.core.instantiation",
            "dm4.core.instance", "dm4.core.type", "dm4.core.meta_type");
        assertEquals(1, topics.size());
        //
        TopicModel type = topics.iterator().next();
        assertEquals("dm4.core.topic_type", type.getUri());
        assertEquals("Topic Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseBidirectional() {
        TopicModel topic = storage.fetchTopic("uri", "dm4.core.topic_type");
        assertNotNull(topic);
        //
        Set<RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(), "dm4.core.instantiation",
            "dm4.core.type", "dm4.core.instance", "dm4.core.topic_type");
        assertEquals(1, topics.size());
        //
        TopicModel type = topics.iterator().next();
        assertEquals("dm4.core.data_type", type.getUri());
        assertEquals("Data Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseWithWideFilter() {
        TopicModel topic = storage.fetchTopic("uri", "dm4.core.data_type");
        assertNotNull(topic);
        //
        Set<RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(), null, null, null, null);
        assertEquals(1, topics.size());
    }

    @Test
    public void deleteAssociation() {
        DeepaMehtaTransaction tx = storage.beginTx();
        try {
            TopicModel topic = storage.fetchTopic("uri", "dm4.core.data_type");
            assertNotNull(topic);
            //
            Set<RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(), "dm4.core.instantiation",
                "dm4.core.instance", "dm4.core.type", "dm4.core.meta_type");
            assertEquals(1, topics.size());
            //
            AssociationModel assoc = topics.iterator().next().getRelatingAssociation();
            assertNotNull(assoc);
            //
            storage.deleteAssociation(assoc.getId());
            //
            topics = storage.fetchTopicRelatedTopics(topic.getId(), "dm4.core.instantiation",
                "dm4.core.instance", "dm4.core.type", "dm4.core.meta_type");
            assertEquals(0, topics.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void deleteAssociationAndFetchAgain() {
        DeepaMehtaTransaction tx = storage.beginTx();
        try {
            AssociationModel assoc = storage.fetchAssociation(assocId);
            assertNotNull(assoc);
            //
            storage.deleteAssociation(assoc.getId());
            assoc = storage.fetchAssociation(assocId);  // throws IllegalStateException
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    /*@Test
    public void testFulltextIndex() {
        List<TopicModel> topics;
        // By default a Lucene index is case-insensitive:
        topics = storage.queryMehtaNodes("DeepaMehta"); assertEquals(2, topics.size());
        topics = storage.queryMehtaNodes("deepamehta"); assertEquals(2, topics.size());
        topics = storage.queryMehtaNodes("DEEPAMEHTA"); assertEquals(2, topics.size());
        // Lucene's default operator is OR:
        topics = storage.queryMehtaNodes("collaboration platform");         assertEquals(1, topics.size());
        topics = storage.queryMehtaNodes("collaboration plaXXXform");       assertEquals(1, topics.size());
        topics = storage.queryMehtaNodes("collaboration AND plaXXXform");   assertEquals(0, topics.size());
        topics = storage.queryMehtaNodes("collaboration AND platform");     assertEquals(1, topics.size());
        // Phrases are set in ".."
        topics = storage.queryMehtaNodes("\"collaboration platform\"");     assertEquals(0, topics.size());
        topics = storage.queryMehtaNodes("\"platform for collaboration\""); assertEquals(1, topics.size());
        // Within phrases wildcards do not work:
        topics = storage.queryMehtaNodes("\"platform * collaboration\"");   assertEquals(0, topics.size());
    }*/

    /*@Test
    public void testExactIndexWithWildcards() {
        List<TopicModel> topics;
        topics = storage.getMehtaNodes("uri", "dm?.core.topic_type"); assertEquals(1, topics.size());
        topics = storage.getMehtaNodes("uri", "*.core.topic_type");   assertEquals(1, topics.size());
        // => in contrast to Lucene docs a wildcard can be used as the first character of a search
        // http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html
        //
        topics = storage.getMehtaNodes("uri", "dm4.core.*");   assertEquals(2, topics.size());
        topics = storage.getMehtaNodes("uri", "dm4.*.*");      assertEquals(2, topics.size());
        topics = storage.getMehtaNodes("uri", "dm4.*.*_type"); assertEquals(2, topics.size());
        // => more than one wildcard can be used in a search
    }*/

    /*@Test
    public void testNegativeResults() {
        TopicModel topic;
        topic = storage.getMehtaNode("uri", "dm4.core.data_type"); assertNotNull(topic);
        topic = storage.getMehtaNode("uri", "dm4.core.*");         assertNull(topic);
        // => DeepaMehtaStorage's get-singular method supports no wildcards.
        //    That reflects the behavior of the underlying Neo4j Index's get() method.
    }*/

    @After
    public void shutdown() {
        storage.shutdown();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupContent() {
        DeepaMehtaTransaction tx = storage.beginTx();
        try {
            createTopic("dm4.core.topic_type", "dm4.core.meta_type",  "Topic Type");
            createTopic("dm4.core.data_type",  "dm4.core.topic_type", "Data Type");
            //
            AssociationModel assoc = new AssociationModel(
                "dm4.core.instantiation",
                new TopicRoleModel("dm4.core.topic_type", "dm4.core.type"),
                new TopicRoleModel("dm4.core.data_type",  "dm4.core.instance"));
            assertTrue(assoc.getId() == -1);
            //
            storage.storeAssociation(assoc);
            assocId = assoc.getId();
            logger.info("### assocId=" + assocId);
            assertTrue(assocId != -1);
            //
            storage.storeAssociationValue(assocId, new SimpleValue(""), asList(IndexMode.OFF), null);
            //
            /*String text1 = "DeepaMehta is a platform for collaboration and knowledge management";
            String text2 = "Lead developer of DeepaMehta is Jörg Richter";
            //
            TopicModel topic3 = storage.storeTopic();
            topic3.setString("uri", "note-1");
            topic3.setString("value", text1);
            topic3.indexAttribute(IndexMode.FULLTEXT, text1, null);
            //
            TopicModel topic4 = storage.storeTopic();
            topic4.setString("uri", "note-2");
            topic4.setString("value", text2);
            topic4.indexAttribute(IndexMode.FULLTEXT, text2, null);*/
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private void createTopic(String uri, String typeUri, String value) {
        TopicModel topic = new TopicModel(uri, typeUri, new SimpleValue(value));
        assertEquals(-1, topic.getId());
        //
        storage.storeTopic(topic);
        //
        long topicId = topic.getId();
        assertTrue(topicId != -1);
        //
        storage.storeTopicValue(topicId, topic.getSimpleValue(), asList(IndexMode.OFF), null);
    }

    // ---

    private String createTempDirectory(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".dir");
            String n = f.getAbsolutePath();
            f.delete();
            new File(n).mkdir();
            return n;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
