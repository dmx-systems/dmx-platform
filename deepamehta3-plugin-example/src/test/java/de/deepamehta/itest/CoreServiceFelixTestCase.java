package de.deepamehta.itest;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanPom;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;



@RunWith(JUnit4TestRunner.class)
public class CoreServiceFelixTestCase extends CoreServiceTest {

    @Configuration
    public static Option[] felixConfiguration() {
        return options(felix(), scanPom("mvn:de.deepamehta/felix-bundles/0.5-SNAPSHOT/pom"));
    }
}
