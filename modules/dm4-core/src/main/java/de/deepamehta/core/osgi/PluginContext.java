package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.PluginService;

import org.osgi.framework.BundleContext;



public interface PluginContext {

    // --- Hooks to be overridden by the plugin developer ---

    void init();

    void shutdown();

    void postInstall();

    void serviceArrived(PluginService service);

    void serviceGone(PluginService service);

    // --- Internal ---

    String getPluginName();

    BundleContext getBundleContext();

    void setCoreService(DeepaMehtaService dms);
}
