package systems.dmx.core.osgi;

import systems.dmx.core.service.CoreService;

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

    void setCoreService(CoreService dmx);
}
