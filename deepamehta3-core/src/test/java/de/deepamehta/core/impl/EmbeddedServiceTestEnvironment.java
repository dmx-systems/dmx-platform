package de.deepamehta.core.impl;

import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import org.junit.Before;

import java.io.File;

public class EmbeddedServiceTestEnvironment {

    protected EmbeddedService dms;
    private File dbPath;

    @Before
    public void setup() throws Exception {
        dbPath = JavaUtils.createTempDirectory("dm3");
        Neo4jStorage storage = new Neo4jStorage(dbPath.getAbsolutePath());
        dms = new EmbeddedService(storage);
        dms.setupDB();
    }

    public void tearDown() {
        dms.shutdown();
        dbPath.delete();
    }

}
