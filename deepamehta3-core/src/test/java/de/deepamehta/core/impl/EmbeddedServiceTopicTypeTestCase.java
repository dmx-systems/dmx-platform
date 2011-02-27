package de.deepamehta.core.impl;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.Storage;
import de.deepamehta.core.storage.Transaction;
import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class EmbeddedServiceTopicTypeTestCase {

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    @Before
    public void setup() {
        String dbPath = JavaUtils.createTempDirectory("neo4j");
        logger.info("Creating temporary test database at " + dbPath);
        dms = new EmbeddedService(new Neo4jStorage(dbPath));
        dms.setupDB();
    }

    @Test
    public void testTopicType() {
        createTopicType();
        readTopicType();
        modifyTopicType();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createTopicType() {
        DataField nameField = new DataField("Name", "text");
        nameField.setUri("de/deepamehta/core/property/Name");
        nameField.setRendererClass("TitleRenderer");
        nameField.setIndexingMode("FULLTEXT");
        //
        DataField descriptionField = new DataField("Description", "html");
        descriptionField.setUri("de/deepamehta/core/property/Description");
        descriptionField.setRendererClass("BodyTextRenderer");
        descriptionField.setIndexingMode("FULLTEXT");
        //
        List dataFields = new ArrayList();
        dataFields.add(nameField);
        dataFields.add(descriptionField);
        //
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/TypeURI", "de/deepamehta/core/topictype/Workspace");
        properties.put("de/deepamehta/core/property/TypeLabel", "Workspace");
        properties.put("icon_src", "/de.deepamehta.3-workspaces/images/star.png");
        properties.put("js_renderer_class", "PlainDocument");
        //
        TopicType topicType = dms.createTopicType(properties, dataFields, null);  // clientContext=null
        //
        assertEquals("de.deepamehta.core.storage.neo4j.Neo4jTopicType", topicType.getClass().getName());
    }

    private void readTopicType() {
        TopicType topicType = dms.getTopicType("de/deepamehta/core/topictype/Workspace", null);
        assertEquals("de.deepamehta.core.storage.neo4j.Neo4jTopicType", topicType.getClass().getName());
        assertEquals(2, topicType.getDataFields().size());
        //
        String typeUri = topicType.getProperty("de/deepamehta/core/property/TypeURI").toString();
        assertEquals("de/deepamehta/core/topictype/Workspace", typeUri);
    }

    private void modifyTopicType() {
        DataField dateCreatedField = new DataField("Date Created", "number");
        dateCreatedField.setUri("de/deepamehta/core/property/DateCreated");
        dateCreatedField.setEditable(false);
        dateCreatedField.setRendererClass("TimestampFieldRenderer");
        dateCreatedField.setIndexingMode("FULLTEXT_KEY");
        //
        DataField dateModifiedField = new DataField("Date Modified", "number");
        dateModifiedField.setUri("de/deepamehta/core/property/DateModified");
        dateModifiedField.setEditable(false);
        dateModifiedField.setRendererClass("TimestampFieldRenderer");
        dateModifiedField.setIndexingMode("FULLTEXT_KEY");
        //
        TopicType topicType = dms.getTopicType("de/deepamehta/core/topictype/Workspace", null);
        //
        // FIXME: let topicType.addDataField() open an transaction
        Transaction tx = dms.beginTx();
        try {
            topicType.addDataField(dateCreatedField);
            topicType.addDataField(dateModifiedField);
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Adding data fields failed", e);
        } finally {
            tx.finish();
        }
        //
        assertEquals(4, topicType.getDataFields().size());
    }
}
