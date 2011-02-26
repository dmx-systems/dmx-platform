package de.deepamehta.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.autoWrap;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.cleanCaches;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanPom;

import de.deepamehta.core.model.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.CoreService;

@RunWith(JUnit4TestRunner.class)
public abstract class CoreServiceTest {

    @Inject
    private BundleContext bundleContext;

    private CoreService sut;

    @Before
    public void setup() throws Exception {
        sut = retrieveCoreService();
    }

    @Configuration
    public static Option[] configuration() {
        return options(cleanCaches(), autoWrap(), //
                systemProperty("deepamehta3.database.path").value("dm3-db"), //
                scanPom("mvn:de.deepamehta/deepamehta3-third-party/0.4.5-SNAPSHOT/pom"), //
                scanPom("mvn:de.deepamehta/deepamehta3-distribution/0.4.5-SNAPSHOT/pom"));
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
        assertEquals(created.getProperties().toMap(), readed.getProperties().toMap());

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
}
