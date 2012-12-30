package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.ConnectedMehtaNode;
import de.deepamehta.mehtagraph.MehtaGraphIndexMode;
import de.deepamehta.mehtagraph.MehtaObjectRole;
import de.deepamehta.mehtagraph.spi.MehtaEdge;
import de.deepamehta.mehtagraph.spi.MehtaGraph;
import de.deepamehta.mehtagraph.spi.MehtaGraphTransaction;
import de.deepamehta.mehtagraph.spi.MehtaNode;
import de.deepamehta.mehtagraph.spi.MehtaObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class MehtaGraphTest {

    private MehtaGraph mg;
    private long edgeId;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        mg = new Neo4jMehtaGraphFactory().createInstance(createTempDirectory("neo4j"));
        setupContent();
    }


    @Test
    public void testEdge() {
        MehtaEdge edge = mg.getMehtaEdge(edgeId);
        assertNotNull(edge);
        MehtaObject node1 = edge.getMehtaObject("dm4.core.type");
        assertNotNull(node1);
        MehtaObject node2 = edge.getMehtaObject("dm4.core.instance");
        assertNotNull(node2);
    }

    @Test
    public void testTraversal() {
        MehtaNode node = mg.getMehtaNode("uri", "dm4.core.data_type");
        assertNotNull(node);
        MehtaNode topicType = getType(node);
        logger.info("### topicType=" + topicType);
        assertEquals("dm4.core.topic_type", topicType.getString("uri"));
        assertEquals("Topic Type", topicType.getString("value"));
    }

    @Test
    public void testFulltextIndex() {
        List<MehtaNode> nodes;
        // By default a Lucene index is case-insensitive:
        nodes = mg.queryMehtaNodes("DeepaMehta"); assertEquals(2, nodes.size());
        nodes = mg.queryMehtaNodes("deepamehta"); assertEquals(2, nodes.size());
        nodes = mg.queryMehtaNodes("DEEPAMEHTA"); assertEquals(2, nodes.size());
        // Lucene's default operator is OR:
        nodes = mg.queryMehtaNodes("collaboration platform");         assertEquals(1, nodes.size());
        nodes = mg.queryMehtaNodes("collaboration plaXXXform");       assertEquals(1, nodes.size());
        nodes = mg.queryMehtaNodes("collaboration AND plaXXXform");   assertEquals(0, nodes.size());
        nodes = mg.queryMehtaNodes("collaboration AND platform");     assertEquals(1, nodes.size());
        // Phrases are set in ".."
        nodes = mg.queryMehtaNodes("\"collaboration platform\"");     assertEquals(0, nodes.size());
        nodes = mg.queryMehtaNodes("\"platform for collaboration\""); assertEquals(1, nodes.size());
        // Within phrases wildcards do not work:
        nodes = mg.queryMehtaNodes("\"platform * collaboration\"");   assertEquals(0, nodes.size());
    }

    @Test
    public void testExactIndexWithWildcards() {
        List<MehtaNode> nodes;
        nodes = mg.getMehtaNodes("uri", "dm?.core.topic_type"); assertEquals(1, nodes.size());
        nodes = mg.getMehtaNodes("uri", "*.core.topic_type");   assertEquals(1, nodes.size());
        // => in contrast to Lucene docs a wildcard can be used as the first character of a search
        // http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html
        //
        nodes = mg.getMehtaNodes("uri", "dm4.core.*");   assertEquals(2, nodes.size());
        nodes = mg.getMehtaNodes("uri", "dm4.*.*");      assertEquals(2, nodes.size());
        nodes = mg.getMehtaNodes("uri", "dm4.*.*_type"); assertEquals(2, nodes.size());
        // => more than one wildcard can be used in a search
    }

    @Test
    public void testNegativeResults() {
        MehtaNode node;
        node = mg.getMehtaNode("uri", "dm4.core.data_type"); assertNotNull(node);
        node = mg.getMehtaNode("uri", "dm4.core.*");         assertNull(node);
        // => MehtaGraph's get-singular method supports no wildcards.
        //    That reflects the behavior of the underlying Neo4j Index's get() method.
    }

    @After
    public void shutdown() {
        mg.shutdown();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private MehtaNode getType(MehtaNode node) {
        ConnectedMehtaNode type = node.getConnectedMehtaNode("dm4.core.instance", "dm4.core.type");
        assertNotNull(type);
        return type.getMehtaNode();
    }

    private void setupContent() {
        MehtaGraphTransaction tx = mg.beginTx();
        try {
            MehtaNode node1 = mg.createMehtaNode();
            node1.setString("uri", "dm4.core.topic_type");
            node1.setString("value", "Topic Type");
            node1.indexAttribute(MehtaGraphIndexMode.KEY, "uri", "dm4.core.topic_type", null);
            //
            MehtaNode node2 = mg.createMehtaNode();
            node2.setString("uri", "dm4.core.data_type");
            node2.setString("value", "Data Type");
            node2.indexAttribute(MehtaGraphIndexMode.KEY, "uri", "dm4.core.data_type", null);
            //
            MehtaEdge edge = mg.createMehtaEdge(new MehtaObjectRole(node1, "dm4.core.type"),
                                                new MehtaObjectRole(node2, "dm4.core.instance"));
            edgeId = edge.getId();
            //
            String text1 = "DeepaMehta is a platform for collaboration and knowledge management";
            String text2 = "Lead developer of DeepaMehta is Jörg Richter";
            //
            MehtaNode node3 = mg.createMehtaNode();
            node3.setString("uri", "note-1");
            node3.setString("value", text1);
            node3.indexAttribute(MehtaGraphIndexMode.FULLTEXT, text1, null);
            //
            MehtaNode node4 = mg.createMehtaNode();
            node4.setString("uri", "note-2");
            node4.setString("value", text2);
            node4.indexAttribute(MehtaGraphIndexMode.FULLTEXT, text2, null);
            //
            tx.success();
        } finally {
            tx.finish();
        }
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
