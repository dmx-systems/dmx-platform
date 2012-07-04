package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.impl.service.PluginImpl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;



/**
 * Base class for plugin developers to derive their plugins from.
 */
public class PluginActivator implements BundleActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PluginImpl plugin;

    // Consumed services
    protected DeepaMehtaService dms;

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        this.plugin = new PluginImpl(context);
    }

    @Override
    public void stop(BundleContext context) {
        plugin.stop();
    }
}
