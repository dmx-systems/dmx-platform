package de.deepamehta.core.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FunctionlessActivator implements BundleActivator {

    private String bundleName;

    @Override
    public void start(BundleContext context) {
        bundleName = (String) context.getBundle().getHeaders().get("Bundle-Name");
    }

    @Override
    public void stop(BundleContext context) {
    }

    public String toString() {
        return bundleName;
    }

}
