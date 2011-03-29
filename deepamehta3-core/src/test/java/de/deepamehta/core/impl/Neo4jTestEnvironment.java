package de.deepamehta.core.impl;

import de.deepamehta.core.impl.EmbeddedService;
import de.deepamehta.core.storage.Storage;
import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import org.junit.After;
import org.junit.Before;

import java.io.File;



public class Neo4jTestEnvironment {

    protected Storage cut;
    private File dbPath;

    @Before
    public void setup() {
        dbPath = JavaUtils.createTempDirectory("neo4j");
        cut = new Neo4jStorage(dbPath.getAbsolutePath());
        new EmbeddedService(cut, null).setupDB();
    }

    @After
    public void tearDown() {
        cut.shutdown();
        dbPath.delete();
    }
}
