package de.deepamehta.core.impl;

import de.deepamehta.core.service.impl.EmbeddedService;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import org.junit.After;
import org.junit.Before;

import java.io.File;

public class Neo4jTestEnvironment {

    protected DeepaMehtaStorage cut;
    private File dbPath;

    @Before
    public void setup() {
        dbPath = JavaUtils.createTempDirectory("neo4j");
        cut = new Neo4jStorage(dbPath.getAbsolutePath());
        // TODO decouple storage initialization
        new EmbeddedService(cut).setupDB();
    }

    @After
    public void tearDown() {
        cut.shutdown();
        dbPath.delete();
    }

}
