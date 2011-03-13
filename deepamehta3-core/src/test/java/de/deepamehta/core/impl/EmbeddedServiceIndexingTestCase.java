package de.deepamehta.core.impl;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;



public class EmbeddedServiceIndexingTestCase extends EmbeddedServiceTestEnvironment {

    private long topicId;

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
        List<DataField> dataFields = new ArrayList<DataField>();
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
