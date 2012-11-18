package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.PluginService;

import org.osgi.framework.BundleContext;



public interface PluginContext {

    void init();

    void postInstall();

    void serviceArrived(PluginService service);

    void serviceGone(PluginService service);

    // ---

    BundleContext getBundleContext();

    void setCoreService(DeepaMehtaService dms);
}
