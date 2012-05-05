package de.deepamehta.core.impl.service;

import de.deepamehta.core.impl.storage.MGStorageBridge;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.util.JavaUtils;

import de.deepamehta.mehtagraph.MehtaGraph;
import de.deepamehta.mehtagraph.MehtaGraphFactory;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    protected DeepaMehtaService dms;
    protected Logger logger = Logger.getLogger(getClass().getName());

    private File dbPath;

    @Before
    public void setup() {
        try {
            logger.info("Creating DB and indexing services");
            dbPath = JavaUtils.createTempDirectory("dm4-");
            MehtaGraph mehtaGraph = MehtaGraphFactory.createInstance(dbPath.getAbsolutePath());
            dms = new EmbeddedService(new MGStorageBridge(mehtaGraph), null);
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
