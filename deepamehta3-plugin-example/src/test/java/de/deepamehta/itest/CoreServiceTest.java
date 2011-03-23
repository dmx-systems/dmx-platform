package de.deepamehta.itest;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.CoreService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.autoWrap;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.cleanCaches;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanPom;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;

@RunWith(JUnit4TestRunner.class)
public abstract class CoreServiceTest {

    @Inject
    private BundleContext bundleContext;

    private CoreService sut;
    private Logger logger = Logger.getLogger(getClass().getName());

    @Before
    public void setup() throws Exception {
        sut = retrieveCoreService();
    }

    @Configuration
    public static Option[] configuration() {
        return options(cleanCaches(), autoWrap(), //
                systemProperty("org.osgi.service.http.port").value("8086"), //
                systemProperty("deepamehta3.database.path").value("dm3-db"), //
                scanPom("mvn:de.deepamehta/deepamehta3-third-party/0.4.5/pom"), //
                scanPom("mvn:de.deepamehta/deepamehta3-bundles/0.4.5/pom"));
    }

    private CoreService retrieveCoreService() throws InterruptedException {
        ServiceTracker tracker = new ServiceTracker(bundleContext, CoreService.class.getName(), null);
        tracker.open();
        CoreService core = (CoreService) tracker.waitForService(5000);
        tracker.close();
        assertNotNull(core);
        return core;
    }

    private static final String TOPICTYPE = "de/deepamehta/core/topictype/Person";

    private static final String PROPERTY = "de/deepamehta/core/property/Name";

    @Test
    public void crudExample() throws Exception {
        ClientContext ctx = null;

        Properties properties = new Properties();
        properties.put(PROPERTY, "IntegrationTestContact");

        // create
        Topic created = sut.createTopic(TOPICTYPE, properties, ctx);
        long id = created.id;

        // read
        Topic readed = sut.getTopic(id, ctx);
        // TODO implement equals()
        assertEquals(id, readed.id);
        assertEquals(created.getProperties(), readed.getProperties());

        // update
        String newValue = "ChangedTestContact";
        properties.put(PROPERTY, newValue);
        sut.setTopicProperties(id, properties);
        Topic updated = sut.getTopic(id, ctx);
        assertEquals(newValue, updated.getProperty(PROPERTY).toString());

        // delete
        sut.deleteTopic(id);

        try {
            sut.getTopic(id, ctx);
            fail("exception not thrown");
        } catch (Exception e) {
            assertEquals("Node[" + updated.id + "]", e.getCause().getMessage());
        }
    }

    private static final String COPIED_TOPICTYPE_URI = "de/deepamehta/core/topictype/PersonCopy";
    private static final String COPIED_TOPICTYPE_LABEL = "Person Copy";

    @Test
    public void copyTopicType() {
        ClientContext ctx = null;
        TopicType srcTopicType = sut.getTopicType(TOPICTYPE, ctx);
        PropValue value = srcTopicType.getProperty("topic_label_field_uri", null);
        assertNull("topic_label_field_uri of type Person is expected to be null but is " + value, value.toString());
        //
        int fieldCount = srcTopicType.getDataFields().size();
        logger.info("### " + srcTopicType + " has " + fieldCount + " data fields");
        //
        JSONObject json = srcTopicType.toJSON();
        TopicType dstTopicType = new TopicType(json);
        dstTopicType.setTypeUri(COPIED_TOPICTYPE_URI);
        dstTopicType.setLabel(COPIED_TOPICTYPE_LABEL);
        sut.createTopicType(dstTopicType.getProperties(), dstTopicType.getDataFields(), ctx);
        //
        TopicType readType = sut.getTopicType(COPIED_TOPICTYPE_URI, ctx);
        assertEquals(fieldCount, readType.getDataFields().size());
    }
}
