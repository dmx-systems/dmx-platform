package de.deepamehta.core.impl;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.Transaction;
import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class EmbeddedServiceIndexingTestCase {

    private EmbeddedService dms;
    private long topicId;

    private Logger logger = Logger.getLogger(getClass().getName());

    @Before
    public void setup() {
        String dbPath = JavaUtils.createTempDirectory("neo4j");
        logger.info("Creating temporary test database at " + dbPath);
        dms = new EmbeddedService(new Neo4jStorage(dbPath));
    }

    @Test
    public void indexing() {
        // Transaction tx = dms.beginTx();  // enable to test nested transactions
        // try {
            createTopicType();              // ### Operation 1
            createTopic();                  // ### Operation 2
            retrieveTopicById();            // ### Operation 3 (doesn't involve Lucene index)
            retrieveTopicByProperty();      // ### Operation 4 (involves Lucene index)
        /*  tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Test indexing() failed", e);
        } finally {
            tx.finish();
        } */
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createTopicType() {
        DataField usernameField = new DataField("Username", "text");
        usernameField.setUri("de/deepamehta/core/property/username");
        usernameField.setIndexingMode("KEY");
        //
        DataField passwordField = new DataField("Password", "text");
        passwordField.setUri("de/deepamehta/core/property/password");
        //
        List dataFields = new ArrayList();
        dataFields.add(usernameField);
        dataFields.add(passwordField);
        //
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/TypeURI", "de/deepamehta/core/topictype/user");
        properties.put("de/deepamehta/core/property/TypeLabel", "User");
        properties.put("js_renderer_class", "PlainDocument");
        //
        TopicType topicType = dms.createTopicType(properties, dataFields, null);  // clientContext=null
        //
        assertEquals("de.deepamehta.core.storage.neo4j.Neo4jTopicType", topicType.getClass().getName());
    }

    private void createTopic() {
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/username", "admin");
        properties.put("de/deepamehta/core/property/password", "12345");
        topicId = dms.createTopic("de/deepamehta/core/topictype/user", properties, null).id;     // clientContext=null
    }

    private void retrieveTopicById() {
        Topic topic = dms.getTopic(topicId, null);
        //
        assertEquals("admin", topic.getProperty("de/deepamehta/core/property/username").toString());
        assertEquals("12345", topic.getProperty("de/deepamehta/core/property/password").toString());
    }

    private void retrieveTopicByProperty() {
        Topic topic = dms.getTopic("de/deepamehta/core/property/username", new PropValue("admin"));
        //
        assertNotNull("Retrieving \"admin\" topic by property failed", topic);
        assertEquals("admin", topic.getProperty("de/deepamehta/core/property/username").toString());
        assertEquals("12345", topic.getProperty("de/deepamehta/core/property/password").toString());
    }
}
