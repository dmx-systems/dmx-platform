package systems.dmx.core.impl;

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
        ModelBuilder mb = new ModelBuilder(); mb.pl = dmx.pl;
        TopicModelImpl topic = mb.topicModel().build();
        assertEquals(0, topic.id);
        assertSame(null, topic.uri);
        assertSame(null, topic.typeUri);
        assertSame(null, topic.value);
        assertNotNull(topic.childTopics);
    }

    @Test
    public void topicModel() {
        ModelBuilder mb = new ModelBuilder(); mb.pl = dmx.pl;
        TopicModelImpl topic = mb.topicModel().id(1234).uri("dmx.core").build();
        assertEquals(1234, topic.id);
        assertSame("dmx.core", topic.uri);
    }
}
