package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.impl.service.PluginImpl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;



/**
 * Base class for plugin developers to derive their plugins from.
 */
public class PluginActivator implements BundleActivator, PluginContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private BundleContext bundleContext;
    private PluginImpl plugin;

    // Consumed services
    protected DeepaMehtaService dms;

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        this.bundleContext = context;
        this.plugin = new PluginImpl(this);
    }

    @Override
    public void stop(BundleContext context) {
        plugin.stop();
    }



    // ************************************
    // *** PluginContext Implementation ***
    // ************************************



    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
    }
}
