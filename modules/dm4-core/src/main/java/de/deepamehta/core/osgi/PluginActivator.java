package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.SecurityHandler;
import de.deepamehta.core.impl.PluginImpl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.logging.Logger;



/**
 * Base class for all DeepaMehta plugins.
 * All DeepaMehta plugins are derived from this class, directly or indirectly.
 */
public class PluginActivator implements BundleActivator, PluginContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected DeepaMehtaService dms;
    protected Bundle bundle;

    private BundleContext bundleContext;
    private PluginImpl plugin;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        this.bundleContext = context;
        this.bundle = context.getBundle();
        this.plugin = new PluginImpl(this);
        //
        try {
            // Note: logging "this" requires "plugin" to be initialzed already
            logger.info("========== Starting " + this + " ==========");
            plugin.start();
        } catch (Exception e) {
            logger.severe("Starting " + this + " failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext context) {
        try {
            logger.info("========== Stopping " + this + " ==========");
            plugin.stop();
        } catch (Exception e) {
            logger.severe("Stopping " + this + " failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
        }
    }



    // ************************************
    // *** PluginContext Implementation ***
    // ************************************



    @Override
    public void init() {
    }

    @Override
    public void postInstall() {
    }

    @Override
    public void serviceArrived(PluginService service) {
    }

    @Override
    public void serviceGone(PluginService service) {
    }

    // ---

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
    }



    // ===

    public String toString() {
        return plugin.toString();
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    /**
     * @param   securityHandler     Optional. If null no security is provided.
     */
    protected void publishDirectory(String directoryPath, String uriNamespace, SecurityHandler securityHandler) {
        plugin.publishDirectory(directoryPath, uriNamespace, securityHandler);
    }
}
