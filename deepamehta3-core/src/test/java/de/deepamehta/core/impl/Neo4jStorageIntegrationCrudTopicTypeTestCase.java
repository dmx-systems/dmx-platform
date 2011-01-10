package de.deepamehta.core.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.Storage;
import de.deepamehta.core.storage.Transaction;
import de.deepamehta.core.storage.neo4j.Neo4jStorage;
import de.deepamehta.core.util.JavaUtils;

public class Neo4jStorageIntegrationCrudTopicTypeTestCase {

    private Storage cut;

    @Before
    public void setup() {
        String f = JavaUtils.createTempDirectory("neo4j");
        cut = new Neo4jStorage(f);
        // initialize storage
        new EmbeddedService(cut); // TODO move db-init into storage impl
    }

    @Test
    public void crudTopicType() throws Exception {
        // create topic type
        DataFieldLiterals fields = new DataFieldLiterals() //
                .addDataField("Name", "text", "test/name", "TitleRenderer", "KEY") //
                .addDataField("Description", "html", "test/description", "BodyTextRenderer", "FULLTEXT") //
                .addDataField("Updated", "text", "test/update", "TextRenderer", "OFF");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("de/deepamehta/core/property/TypeURI", "test/topicTypeTest");
        properties.put("de/deepamehta/core/property/TypeLabel", "topic type test");

        Transaction tx = cut.beginTx();
        cut.createTopicType(properties, fields.getList().subList(0, 2));
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
        updateTopic.addDataField(fields.getList().get(2));

        // tx.success();
        // tx.finish();

        // read again
        actual = cut.getTopicType("test/topicTypeTest");
        assertEquals(3, actual.getDataFields().size());
        assertEquals("Updated", actual.getDataField("test/update").getLabel());

        // delete
        // TODO implement topic type deletion
    }
}

class DataFieldLiterals {
    List<DataField> dataFields = new ArrayList<DataField>();

    public DataFieldLiterals addDataField(String label, String dataType, String uri, String renderer, String indexMode) {
        DataField field = new DataField(label, dataType);
        field.setUri(uri);
        field.setRendererClass(renderer);
        field.setIndexingMode(indexMode);
        dataFields.add(field);
        return this;
    }

    public List<DataField> getList() {
        return dataFields;
    }

}
