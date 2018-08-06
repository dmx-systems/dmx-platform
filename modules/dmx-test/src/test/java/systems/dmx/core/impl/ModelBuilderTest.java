package systems.dmx.core.impl;

import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class ModelBuilderTest extends CoreServiceTestEnvironment {

    @Test
    public void topicModelDefaults() {
        ModelBuilderImpl mb = new ModelBuilderImpl(); mb.pl = dmx.pl;
        TopicModel topic = mb.topicModel().build();
        assertEquals(0, topic.getId());
        assertSame(null, topic.getUri());
        assertSame(null, topic.getTypeUri());
        assertSame(null, topic.getSimpleValue());
        assertNotNull(topic.getChildTopicsModel());
    }

    @Test
    public void topicModel() {
        ModelBuilderImpl mb = new ModelBuilderImpl(); mb.pl = dmx.pl;
        TopicModel topic = mb.topicModel().id(1234).uri("dmx.core").build();
        assertEquals(1234, topic.getId());
        assertSame("dmx.core", topic.getUri());
    }

    @Test
    public void topicTypeModel() {
        ModelBuilderImpl mb = new ModelBuilderImpl(); mb.pl = dmx.pl;
        TopicTypeModel topicType = mb.topicTypeModel().id(1234).dataType("dmx.core.text").build();
        assertEquals(1234, topicType.getId());
        assertSame("dmx.core.text", topicType.getDataTypeUri());
    }
}
