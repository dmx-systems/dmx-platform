package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.PluginInfo;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * ### FIXDOC
 * A cache that holds the registered {@link Plugin} instances.
 * <p>
 * A PluginManager singleton is hold by the {@link EmbeddedService} and is accessed concurrently by all
 * bundle activation threads (as created by the File Install bundle).
 * <p>
 * Iteration over all plugins is synchronized with cache modifications ({@link put}, {@link remove}).
 * To iterate create a {@link PluginManager.Iterator} instance and provide a {@link #body} method.
 */
class PluginManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Registered plugins.
     * Hashed by plugin bundle's symbolic name, e.g. "de.deepamehta.topicmaps".
     */
    static private Map<String, PluginImpl> plugins = new HashMap();

    private BundleContext bundleContext;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginManager(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    PluginImpl getPlugin(String pluginUri) {
        PluginImpl plugin = plugins.get(pluginUri);
        if (plugin == null) {
            throw new RuntimeException("Plugin \"" + pluginUri + "\" not found");
        }
        return plugin;
    }

    Set<PluginInfo> getPluginInfo() {
        Set info = new HashSet();
        for (PluginImpl plugin : plugins.values()) {
            info.add(plugin.getInfo());
        }
        return info;
    }

    // ---

    void registerPlugin(PluginImpl plugin) {
        plugins.put(plugin.getUri(), plugin);
    }

    void unregisterPlugin(String pluginUri) {
        plugins.remove(pluginUri);
    }

    boolean isPluginRegistered(String pluginUri) {
        return plugins.get(pluginUri) != null;
    }

    /**
     * Checks if all DeepaMehta plugin bundles are registered at core.
     * Fires the {@link CoreEvent.ALL_PLUGINS_ACTIVE} event if so.
     * <p>
     * Called from the Plugin class.
     * Not meant to be called by a plugin developer.
     */
    boolean checkAllPluginsActive() {
        Bundle[] bundles = bundleContext.getBundles();
        int plugins = 0;
        int active = 0;
        for (Bundle bundle : bundles) {
            if (isDeepaMehtaPlugin(bundle)) {
                plugins++;
                if (isPluginRegistered(bundle.getSymbolicName())) {
                    active++;
                }
            }
        }
        logger.info("### Bundles total: " + bundles.length +
            ", DeepaMehta plugins: " + plugins + ", Active: " + active);
        return plugins == active;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean isDeepaMehtaPlugin(Bundle bundle) {
        String packages = (String) bundle.getHeaders().get("Import-Package");
        // Note: packages might be null. Not all bundles import packges.
        return packages != null && packages.contains("de.deepamehta.core.service") &&
            !bundle.getSymbolicName().equals("de.deepamehta.core");
    }
}
