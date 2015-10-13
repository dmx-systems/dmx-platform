package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;

import org.osgi.framework.BundleContext;



public interface PluginContext {

    // --- Hooks to be overridden by the plugin developer ---

    void preInstall();

    void init();

    void shutdown();

    void serviceArrived(Object service);

    void serviceGone(Object service);

    // --- Internal ---

    String getPluginName();

    BundleContext getBundleContext();

    void setCoreService(DeepaMehtaService dms);
}
