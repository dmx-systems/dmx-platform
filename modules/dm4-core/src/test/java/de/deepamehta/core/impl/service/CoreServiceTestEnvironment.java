package de.deepamehta.core.impl.service;

import de.deepamehta.core.impl.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    protected EmbeddedService dms;
    protected Logger logger = Logger.getLogger(getClass().getName());

    private File dbPath;

    @Before
    public void setup() {
        try {
            logger.info("Creating DB and indexing services");
            dbPath = JavaUtils.createTempDirectory("dm4-");
            DeepaMehtaStorage storage = new Neo4jStorage(dbPath.getAbsolutePath());
            dms = new EmbeddedService(storage, null);
            dms.setupDB();
        } catch (Exception e) {
            throw new RuntimeException("Opening database failed (path=" + dbPath + ")", e);
        }
    }

    @After
    public void tearDown() {
        dms.shutdown();
        dbPath.delete();
    }
}
