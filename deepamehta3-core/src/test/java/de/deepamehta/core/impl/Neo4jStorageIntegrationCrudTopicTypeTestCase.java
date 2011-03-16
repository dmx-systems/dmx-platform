package de.deepamehta.core.impl;

import de.deepamehta.core.model.DataField;
// import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.DeepaMehtaTransaction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class Neo4jStorageIntegrationCrudTopicTypeTestCase extends Neo4jTestEnvironment {

    @Test
    public void crudTopicType() throws Exception {
/*
        // fields
        List<DataField> fields = new DataFieldLiteral() //
                .add("Name", "text", "test/name", "TitleRenderer", "KEY") //
                .add("Description", "html", "test/description", "BodyTextRenderer", "FULLTEXT") //
                .add("Updated", "text", "test/update", "TextRenderer", "OFF").getList();

        // properties
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/TypeURI", "test/topicTypeTest");
        properties.put("de/deepamehta/core/property/TypeLabel", "topic type test");

        // create topic type
        DeepaMehtaTransaction tx = cut.beginTx();
        cut.createTopicType(properties, fields.subList(0, 2));
        tx.success();
        tx.finish();

        // read topic type
        TopicType actual = cut.getTopicType("test/topicTypeTest");
        assertEquals(2, actual.getDataFields().size());
        DataField nameField = actual.getDataField(0);
        assertEquals("text", nameField.getDataType());
        assertEquals("Name", nameField.getLabel());

        // update (neo4j attached topic type, updates directly)
        TopicType updateTopic = cut.getTopicType("test/topicTypeTest");
        tx = cut.beginTx();
        updateTopic.addDataField(fields.get(2));
        tx.success();
        tx.finish();

        // read again
        actual = cut.getTopicType("test/topicTypeTest");
        assertEquals(3, actual.getDataFields().size());
        assertEquals("Updated", actual.getDataField("test/update").getLabel());

        // delete
        // TODO implement topic type deletion
*/
    }
}
