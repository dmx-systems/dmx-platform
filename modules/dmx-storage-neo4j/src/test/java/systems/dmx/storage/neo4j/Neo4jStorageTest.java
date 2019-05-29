package systems.dmx.storage.neo4j;

import systems.dmx.core.impl.ModelFactoryImpl;
import systems.dmx.core.impl.PersistenceLayer;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RelatedAssociationModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.core.util.JavaUtils;

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
import static java.util.Arrays.asList;
import java.util.List;
import java.util.logging.Logger;



public class Neo4jStorageTest {

    private DMXStorage storage;
    private ModelFactoryImpl mf;

    private long assocId;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        mf = new ModelFactoryImpl();
        storage = new Neo4jStorageFactory().newDMXStorage(createTempDirectory("neo4j-test-"), mf);
        new PersistenceLayer(storage);  // Note: the ModelFactory doesn't work when no PersistenceLayer is created
        setupContent();
    }

    @After
    public void shutdown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    // ---

    @Test
    public void fetchAssociation() {
        AssociationModel assoc = storage.fetchAssociation(assocId);
        assertNotNull(assoc);
        //
        RoleModel roleModel1 = assoc.getRoleModel("dmx.core.type");
        assertNotNull(roleModel1);
        //
        RoleModel roleModel2 = assoc.getRoleModel("dmx.core.instance");
        assertNotNull(roleModel2);
    }

    @Test
    public void traverse() {
        TopicModel topic = storage.fetchTopic("uri", "dmx.core.data_type");
        assertNotNull(topic);
        //
        List<? extends RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(),
            "dmx.core.instantiation", "dmx.core.instance", "dmx.core.type", "dmx.core.meta_type");
        assertEquals(1, topics.size());
        //
        TopicModel type = topics.get(0);
        assertEquals("dmx.core.topic_type", type.getUri());
        assertEquals("Topic Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseBidirectional() {
        TopicModel topic = storage.fetchTopic("uri", "dmx.core.topic_type");
        assertNotNull(topic);
        //
        List<? extends RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(),
            "dmx.core.instantiation", "dmx.core.type", "dmx.core.instance", "dmx.core.topic_type");
        assertEquals(1, topics.size());
        //
        TopicModel type = topics.get(0);
        assertEquals("dmx.core.data_type", type.getUri());
        assertEquals("Data Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseWithWideFilter() {
        TopicModel topic = storage.fetchTopic("uri", "dmx.core.data_type");
        assertNotNull(topic);
        //
        List<? extends RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(), null, null, null,
            null);
        assertEquals(1, topics.size());
    }

    @Test
    public void deleteAssociation() {
        DMXTransaction tx = storage.beginTx();
        try {
            TopicModel topic = storage.fetchTopic("uri", "dmx.core.data_type");
            assertNotNull(topic);
            //
            List<? extends RelatedTopicModel> topics = storage.fetchTopicRelatedTopics(topic.getId(),
                "dmx.core.instantiation", "dmx.core.instance", "dmx.core.type", "dmx.core.meta_type");
            assertEquals(1, topics.size());
            //
            AssociationModel assoc = topics.get(0).getRelatingAssociation();
            assertNotNull(assoc);
            //
            storage.deleteAssociation(assoc.getId());
            //
            topics = storage.fetchTopicRelatedTopics(topic.getId(), "dmx.core.instantiation",
                "dmx.core.instance", "dmx.core.type", "dmx.core.meta_type");
            assertEquals(0, topics.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void deleteAssociationAndFetchAgain() {
        DMXTransaction tx = storage.beginTx();
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

    @Test
    public void testFulltextIndex() {
        List<TopicModel> topics;
        // By default a Lucene index is case-insensitive:
        topics = storage.queryTopics("Dmx"); assertEquals(2, topics.size());
        topics = storage.queryTopics("dmx"); assertEquals(2, topics.size());
        topics = storage.queryTopics("DMX"); assertEquals(2, topics.size());
        // Lucene's default operator is OR:
        topics = storage.queryTopics("collaboration platform");         assertEquals(1, topics.size());
        topics = storage.queryTopics("collaboration plaXXXform");       assertEquals(1, topics.size());
        topics = storage.queryTopics("collaboration AND plaXXXform");   assertEquals(0, topics.size());
        topics = storage.queryTopics("collaboration AND platform");     assertEquals(1, topics.size());
        // Phrases are set in ".."
        topics = storage.queryTopics("\"collaboration platform\"");     assertEquals(0, topics.size());
        topics = storage.queryTopics("\"platform for collaboration\""); assertEquals(1, topics.size());
        // Within phrases wildcards do not work:
        topics = storage.queryTopics("\"platform * collaboration\"");   assertEquals(0, topics.size());
    }

    @Test
    public void testFulltextIndexWithHTML() {
        List<TopicModel> topics;
        // Lucene's Whitespace Analyzer (default for a Neo4j "fulltext" index) regards HTML as belonging to the word
        topics = storage.queryTopics("Haskell");        assertEquals(1, topics.size()); assertUri(topics, "note-4");
        topics = storage.queryTopics("Haskell*");       assertEquals(1, topics.size()); assertUri(topics, "note-4");
        topics = storage.queryTopics("*Haskell*");      assertEquals(2, topics.size());
        topics = storage.queryTopics("<b>Haskell");     assertEquals(0, topics.size());
        topics = storage.queryTopics("<b>Haskell*");    assertEquals(1, topics.size()); assertUri(topics, "note-3");
        topics = storage.queryTopics("<b>Haskell</b>"); assertEquals(1, topics.size()); assertUri(topics, "note-3");
    }

    private void assertUri(List<TopicModel> singletonList, String topicUri) {
        assertEquals(topicUri, singletonList.get(0).getUri());
    }

    @Test
    public void testExactIndexWithQuery() {
        List<? extends TopicModel> topics;
        topics = storage.fetchTopics("uri", "dm?.core.topic_type"); assertEquals(1, topics.size());
        topics = storage.fetchTopics("uri", "*.core.topic_type");   assertEquals(1, topics.size());
        // => in contrast to Lucene docs a wildcard can be used as the first character of a search
        // http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html
        //
        topics = storage.fetchTopics("uri", "dmx.core.*");   assertEquals(2, topics.size());
        topics = storage.fetchTopics("uri", "dmx.*.*");      assertEquals(2, topics.size());
        topics = storage.fetchTopics("uri", "dmx.*.*_type"); assertEquals(2, topics.size());
        // => more than one wildcard can be used in a search
    }

    @Test
    public void testExactIndexWithGet() {
        TopicModel topic;
        topic = storage.fetchTopic("uri", "dmx.core.data_type"); assertNotNull(topic);
        topic = storage.fetchTopic("uri", "dmx.core.*");         assertNull(topic);
        // => DMXStorage's get-singular method supports no wildcards.
        //    That reflects the behavior of the underlying Neo4j Index's get() method.
    }

    // --- Property Index ---

    @Test
    public void propertyIndex() {
        List<? extends TopicModel> topics;
        // Note: The same type must be used for indexing and querying.
        // That is, you can't index a value as a Long and then query the index using an Integer.
        topics = storage.fetchTopicsByProperty("score", 12L);  assertEquals(0, topics.size());
        topics = storage.fetchTopicsByProperty("score", 123L); assertEquals(1, topics.size());
        topics = storage.fetchTopicsByProperty("score", 23L);  assertEquals(2, topics.size());
    }

    @Test
    public void propertyIndexRange() {
        List<? extends TopicModel> topics;
        topics = storage.fetchTopicsByPropertyRange("score", 1L, 1000L);  assertEquals(3, topics.size());
        topics = storage.fetchTopicsByPropertyRange("score", 23L, 23L);   assertEquals(2, topics.size());
        topics = storage.fetchTopicsByPropertyRange("score", 23L, 1234L); assertEquals(4, topics.size());
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupContent() {
        DMXTransaction tx = storage.beginTx();
        try {
            createTopic("dmx.core.topic_type", "dmx.core.meta_type",  "Topic Type");
            createTopic("dmx.core.data_type",  "dmx.core.topic_type", "Data Type");
            //
            assocId = createAssociation("dmx.core.instantiation",
                "dmx.core.topic_type", "dmx.core.type",
                "dmx.core.data_type", "dmx.core.instance"
            );
            //
            // Fulltext indexing
            //
            createTopic("note-1", "dmx.notes.note", "DMX is a platform for knowledge management and collaboration");
            createTopic("note-2", "dmx.notes.note", "Lead developer of DMX is Jörg Richter");
            //
            // Fulltext HTML indexing
            //
            String htmlText = "Java and Oracle is no fun anymore. I'm learning <b>Haskell</b> now.";
            createTopic("note-3", "dmx.notes.note", htmlText);
            createTopic("note-4", "dmx.notes.note", htmlText, true);
            //
            // Property indexing
            //
            createTopic("score", 123L);
            createTopic("score", 23L);
            createTopic("score", 1234L);
            createTopic("score", 23L);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    private long createTopic(String uri, String typeUri, String value) {
        return createTopic(uri, typeUri, value, false);
    }

    private long createTopic(String uri, String typeUri, String value, boolean isHtmlValue) {
        TopicModel topic = mf.newTopicModel(uri, typeUri, new SimpleValue(value));
        assertEquals(-1, topic.getId());
        //
        storage.storeTopic(topic);
        //
        long topicId = topic.getId();
        assertTrue(topicId != -1);
        //
        storage.storeTopicValue(topicId, topic.getSimpleValue(), typeUri, isHtmlValue);
        //
        return topicId;
    }

    private void createTopic(String propUri, Object propValue) {
        long topicId = createTopic(null, "dmx.notes.note", "");
        storage.storeTopicProperty(topicId, propUri, propValue, true);     // addToIndex=true
    }

    // ---

    private long createAssociation(String typeUri, String topicUri1, String roleTypeUri1,
                                                   String topicUri2, String roleTypeUri2) {
        AssociationModel assoc = mf.newAssociationModel(typeUri,
            mf.newTopicRoleModel(topicUri1, roleTypeUri1),
            mf.newTopicRoleModel(topicUri2, roleTypeUri2)
        );
        assertEquals(-1, assoc.getId());
        //
        storage.storeAssociation(assoc);
        //
        long assocId = assoc.getId();
        assertTrue(assocId != -1);
        //
        storage.storeAssociationValue(assocId, new SimpleValue(""), typeUri, false);
        //
        return assocId;
    }

    // ---

    private String createTempDirectory(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".dir");
            String n = f.getAbsolutePath();
            f.delete();
            new File(n).mkdir();
            return n;
        } catch (Exception e) {
            throw new RuntimeException("Creating temporary directory failed", e);
        }
    }
}
