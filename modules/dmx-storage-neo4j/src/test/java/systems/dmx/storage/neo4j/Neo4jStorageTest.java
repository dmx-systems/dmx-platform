package systems.dmx.storage.neo4j;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.impl.AccessLayer;
import systems.dmx.core.impl.AssocModelImpl;
import systems.dmx.core.impl.ModelFactoryImpl;
import systems.dmx.core.impl.RelatedTopicModelImpl;
import systems.dmx.core.impl.TopicModelImpl;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXTransaction;

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

    private DMXStorage db;
    private ModelFactoryImpl mf;

    private long assocId;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        mf = new ModelFactoryImpl();
        db = new Neo4jStorageFactory().newDMXStorage(createTempDirectory("neo4j-test-"), mf);
        new AccessLayer(db);  // Note: the ModelFactory doesn't work when no AccessLayer is created
        setupContent();
    }

    @After
    public void shutdown() {
        if (db != null) {
            db.shutdown();
        }
    }

    // ---

    @Test
    public void fetchAssoc() {
        AssocModelImpl assoc = db.fetchAssoc(assocId);
        assertNotNull(assoc);
        //
        PlayerModel player1 = assoc.getPlayerByRole(TYPE);
        assertNotNull(player1);
        //
        PlayerModel player2 = assoc.getPlayerByRole(INSTANCE);
        assertNotNull(player2);
    }

    @Test
    public void traverse() {
        TopicModelImpl topic = db.fetchTopic("uri", DATA_TYPE);
        assertNotNull(topic);
        //
        List<RelatedTopicModelImpl> topics = db.fetchTopicRelatedTopics(topic.getId(),
            INSTANTIATION, INSTANCE, TYPE, META_TYPE);
        assertEquals(1, topics.size());
        //
        TopicModelImpl type = topics.get(0);
        assertEquals(TOPIC_TYPE, type.getUri());
        assertEquals("Topic Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseBidirectional() {
        TopicModelImpl topic = db.fetchTopic("uri", TOPIC_TYPE);
        assertNotNull(topic);
        //
        List<RelatedTopicModelImpl> topics = db.fetchTopicRelatedTopics(topic.getId(),
            INSTANTIATION, TYPE, INSTANCE, TOPIC_TYPE);
        assertEquals(1, topics.size());
        //
        TopicModelImpl type = topics.get(0);
        assertEquals(DATA_TYPE, type.getUri());
        assertEquals("Data Type", type.getSimpleValue().toString());
    }

    @Test
    public void traverseWithWideFilter() {
        TopicModelImpl topic = db.fetchTopic("uri", DATA_TYPE);
        assertNotNull(topic);
        //
        List<RelatedTopicModelImpl> topics = db.fetchTopicRelatedTopics(topic.getId(), null, null, null, null);
        assertEquals(1, topics.size());
    }

    @Test
    public void deleteAssoc() {
        DMXTransaction tx = db.beginTx();
        try {
            TopicModelImpl topic = db.fetchTopic("uri", DATA_TYPE);
            assertNotNull(topic);
            //
            List<RelatedTopicModelImpl> topics = db.fetchTopicRelatedTopics(topic.getId(),
                INSTANTIATION, INSTANCE, TYPE, META_TYPE);
            assertEquals(1, topics.size());
            //
            AssocModelImpl assoc = topics.get(0).getRelatingAssoc();
            assertNotNull(assoc);
            //
            db.deleteAssoc(assoc.getId());
            //
            topics = db.fetchTopicRelatedTopics(topic.getId(), INSTANTIATION,
                INSTANCE, TYPE, META_TYPE);
            assertEquals(0, topics.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void deleteAssocAndFetchAgain() {
        DMXTransaction tx = db.beginTx();
        try {
            AssocModelImpl assoc = db.fetchAssoc(assocId);
            assertNotNull(assoc);
            //
            db.deleteAssoc(assoc.getId());
            assoc = db.fetchAssoc(assocId);  // throws IllegalStateException
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void testFulltextIndex() {
        List<TopicModelImpl> topics;
        // By default a Lucene index is case-insensitive:
        topics = db.queryTopicsFulltext("Dmx"); assertEquals(2, topics.size());
        topics = db.queryTopicsFulltext("dmx"); assertEquals(2, topics.size());
        topics = db.queryTopicsFulltext("DMX"); assertEquals(2, topics.size());
        // Lucene's default operator is OR:
        topics = db.queryTopicsFulltext("collaboration platform");         assertEquals(1, topics.size());
        topics = db.queryTopicsFulltext("collaboration plaXXXform");       assertEquals(1, topics.size());
        topics = db.queryTopicsFulltext("collaboration AND plaXXXform");   assertEquals(0, topics.size());
        topics = db.queryTopicsFulltext("collaboration AND platform");     assertEquals(1, topics.size());
        // Phrases are set in ".."
        topics = db.queryTopicsFulltext("\"collaboration platform\"");     assertEquals(0, topics.size());
        topics = db.queryTopicsFulltext("\"platform for collaboration\""); assertEquals(1, topics.size());
        // Within phrases wildcards do not work:
        topics = db.queryTopicsFulltext("\"platform * collaboration\"");   assertEquals(0, topics.size());
    }

    @Test
    public void testFulltextIndexWithHTML() {
        List<TopicModelImpl> topics;
        // Lucene's Whitespace Analyzer (default for a Neo4j "fulltext" index) regards HTML as belonging to the word
        topics = db.queryTopicsFulltext("Haskell");        assertEquals(1, topics.size()); assertUri(topics, "note-4");
        topics = db.queryTopicsFulltext("Haskell*");       assertEquals(1, topics.size()); assertUri(topics, "note-4");
        topics = db.queryTopicsFulltext("*Haskell*");      assertEquals(2, topics.size());
        topics = db.queryTopicsFulltext("<b>Haskell");     assertEquals(0, topics.size());
        topics = db.queryTopicsFulltext("<b>Haskell*");    assertEquals(1, topics.size()); assertUri(topics, "note-3");
        topics = db.queryTopicsFulltext("<b>Haskell</b>"); assertEquals(1, topics.size()); assertUri(topics, "note-3");
    }

    private void assertUri(List<TopicModelImpl> singletonList, String topicUri) {
        assertEquals(topicUri, singletonList.get(0).getUri());
    }

    @Test
    public void testExactIndexWithQuery() {
        List<TopicModelImpl> topics;
        topics = db.queryTopics("uri", "dm?.core.topic_type"); assertEquals(1, topics.size());
        topics = db.queryTopics("uri", "*.core.topic_type");   assertEquals(1, topics.size());
        // => in contrast to Lucene docs a wildcard can be used as the first character of a search
        // http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html
        //
        topics = db.queryTopics("uri", "dmx.core.*");   assertEquals(2, topics.size());
        topics = db.queryTopics("uri", "dmx.*.*");      assertEquals(2, topics.size());
        topics = db.queryTopics("uri", "dmx.*.*_type"); assertEquals(2, topics.size());
        // => more than one wildcard can be used in a search
    }

    @Test
    public void testExactIndexWithGet() {
        TopicModelImpl topic;
        topic = db.fetchTopic("uri", DATA_TYPE); assertNotNull(topic);
        topic = db.fetchTopic("uri", "dmx.core.*");         assertNull(topic);
        // => DMXStorage's get-singular method supports no wildcards.
        //    That reflects the behavior of the underlying Neo4j Index's get() method.
    }

    // --- Iterables ---

    @Test
    public void fetchAllTopics() {
        Iterable<TopicModelImpl> topics = db.fetchAllTopics();
        int count = 0;
        for (TopicModelImpl topic : topics) {
            count++;
        }
        assertEquals(10, count);
        // reuse iterable
        count = 0;
        for (TopicModelImpl topic : topics) {
            count++;
        }
        assertEquals(10, count);
    }

    @Test
    public void fetchAllAssocs() {
        Iterable<AssocModelImpl> assocs = db.fetchAllAssocs();
        int count = 0;
        for (AssocModelImpl assoc : assocs) {
            count++;
        }
        assertEquals(1, count);
        // reuse iterable
        count = 0;
        for (AssocModelImpl assoc : assocs) {
            count++;
        }
        assertEquals(1, count);
    }

    // --- Property Index ---

    @Test
    public void propertyIndex() {
        List<TopicModelImpl> topics;
        // Note: The same type must be used for indexing and querying.
        // That is, you can't index a value as a Long and then query the index using an Integer.
        topics = db.fetchTopicsByProperty("score", 12L);  assertEquals(0, topics.size());
        topics = db.fetchTopicsByProperty("score", 123L); assertEquals(1, topics.size());
        topics = db.fetchTopicsByProperty("score", 23L);  assertEquals(2, topics.size());
    }

    @Test
    public void propertyIndexRange() {
        List<TopicModelImpl> topics;
        topics = db.fetchTopicsByPropertyRange("score", 1L, 1000L);  assertEquals(3, topics.size());
        topics = db.fetchTopicsByPropertyRange("score", 23L, 23L);   assertEquals(2, topics.size());
        topics = db.fetchTopicsByPropertyRange("score", 23L, 1234L); assertEquals(4, topics.size());
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupContent() {
        DMXTransaction tx = db.beginTx();
        try {
            createTopic(TOPIC_TYPE, META_TYPE,  "Topic Type");
            createTopic(DATA_TYPE,  TOPIC_TYPE, "Data Type");
            //
            assocId = createAssoc(INSTANTIATION,
                TOPIC_TYPE, TYPE,
                DATA_TYPE, INSTANCE
            );
            //
            // Fulltext indexing
            //
            createTopic("note-1", "dmx.notes.note", "DMX is a platform for collaboration and knowledge management");
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
        TopicModelImpl topic = mf.newTopicModel(uri, typeUri, new SimpleValue(value));
        assertEquals(-1, topic.getId());
        //
        db.storeTopic(topic);
        //
        long topicId = topic.getId();
        assertTrue(topicId != -1);
        //
        db.storeTopicValue(topicId, topic.getSimpleValue(), typeUri, isHtmlValue);
        //
        return topicId;
    }

    private void createTopic(String propUri, Object propValue) {
        long topicId = createTopic(null, "dmx.notes.note", "");
        db.storeTopicProperty(topicId, propUri, propValue, true);     // addToIndex=true
    }

    // ---

    private long createAssoc(String typeUri, String topicUri1, String roleTypeUri1,
                                             String topicUri2, String roleTypeUri2) {
        AssocModelImpl assoc = mf.newAssocModel(typeUri,
            mf.newTopicPlayerModel(topicUri1, roleTypeUri1),
            mf.newTopicPlayerModel(topicUri2, roleTypeUri2)
        );
        assertEquals(-1, assoc.getId());
        //
        db.storeAssoc(assoc);
        //
        long assocId = assoc.getId();
        assertTrue(assocId != -1);
        //
        db.storeAssocValue(assocId, new SimpleValue(""), typeUri, false);
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
