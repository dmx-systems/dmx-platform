package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.SecurityHandler;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.felix.http.api.ExtHttpService;

import javax.servlet.Filter;

import java.util.logging.Logger;



/**
 * Base class for plugin developers to derive their plugins from.
 */
public class PluginActivator implements BundleActivator, PluginContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private BundleContext bundleContext;
    private Plugin plugin;

    // Consumed service
    protected DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    private String bundleName;

    // -------------------------------------------------------------------------------------------------- Public Methods

    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        this.bundleContext = context;
        bundleName = (String) bundleContext.getBundle().getHeaders().get("Bundle-Name");
        new ServiceTracker(bundleContext, DeepaMehtaService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object service = super.addingService(reference);
                if (service instanceof DeepaMehtaService) {
                    setCoreService((DeepaMehtaService) service);
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                super.removedService(reference, service);
                if (service == dms) {
                    stop();
                }
            }

        }.open();
    }

    @Override
    public void stop(BundleContext context) {
        stop();
    }

    // ************************************
    // *** PluginContext Implementation ***
    // ************************************



    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    // ===

    public String toString() {
        return bundleName;
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
        //
        try {
            plugin = dms.createPlugin(this);
            plugin.start();
        } catch (Exception e) {
            logger.severe("Starting " + this + " failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here.
            // It would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    /**
     * @param   securityHandler     Optional. If null no security is provided.
     */
    protected void publishDirectory(String directoryPath, String uriNamespace, SecurityHandler securityHandler) {
        plugin.publishDirectory(directoryPath, uriNamespace, securityHandler);
    }

    protected void registerFilter(Filter filter) {
        try {
            ServiceReference sRef = bundleContext.getServiceReference(ExtHttpService.class.getName());
            if (sRef != null) {
                ExtHttpService service = (ExtHttpService) bundleContext.getService(sRef);
                // Dictionary initParams = null, int ranking = 0, HttpContext context = null
                service.registerFilter(filter, "/.*", null, 0, null);
            } else {
                throw new RuntimeException("ExtHttpService not available");
            }
        } catch (Exception e) {
            throw new RuntimeException("Registering filter " + filter + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void stop() {
        try {
            if(plugin != null) { // do not call it twice
                plugin.stop();
            }
            plugin = null;
            dms = null;
        } catch (Exception e) {
            logger.severe("Stopping " + this + " failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here.
            // It would not print out the stacktrace.
        }
    }

}
