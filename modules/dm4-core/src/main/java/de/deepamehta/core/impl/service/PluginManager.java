package de.deepamehta.core.impl.service;

import de.deepamehta.core.activator.Core;
import de.deepamehta.core.service.PluginInfo;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Activates and deactivates plugins. Keeps a pool of activated plugins.
 * <p>
 * A PluginManager singleton is hold by the {@link EmbeddedService} and is accessed concurrently by all
 * bundle activation threads (as created by the File Install bundle).
 */
class PluginManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The pool of activated plugins.
     *
     * Hashed by plugin bundle's symbolic name, e.g. "de.deepamehta.topicmaps".
     */
    private Map<String, PluginImpl> activatedPlugins = new HashMap<String, PluginImpl>();

    /**
     * Context of the DeepaMehta 4 Core bundle.
     */
    private BundleContext bundleContext;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   bundleContext   The context of the DeepaMehta 4 Core bundle.
     */
    PluginManager(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Finally activates a plugin. Called once the plugin's requirements are met (see
     * PluginImpl.checkRequirementsForActivation()).
     *
     * Activation comprises:
     * - install the plugin in the database (includes migrations, post-install event, type introduction)
     * - initialize the plugin
     * - register the plugin's listeners
     * - add the plugin to the pool of activated plugins
     *
     * If this plugin is already activated, nothing is performed and false is returned.
     * Otherwise true is returned.
     *
     * Note: this method is synchronized. While a plugin is activated no other plugin must be activated. Otherwise
     * the "type introduction" mechanism might miss some types. Consider this unsynchronized scenario: plugin B
     * starts running its migrations just in the moment between plugin A's type introduction and listener registration.
     * Plugin A might miss some of the types created by plugin B.
     */
    synchronized boolean activatePlugin(PluginImpl plugin) {
        try {
            // Note: we must not activate a plugin twice.
            // This would happen e.g. if a dependency plugin is redeployed.
            if (isPluginActivated(plugin.getUri())) {
                logger.info("Activation of " + plugin + " ABORTED -- already activated");
                return false;
            }
            //
            logger.info("----- Activating " + plugin + " -----");
            plugin.installPluginInDB();
            plugin.initializePlugin();
            plugin.registerListeners();
            // Note: registering the listeners is deferred until the plugin is installed in the database and the
            // POST_INSTALL_PLUGIN event is delivered (see PluginImpl.installPluginInDB()).
            // Consider the Access Control plugin: it can't set a topic's creator before the "admin" user is created.
            addToActivatedPlugins(plugin);
            logger.info("----- Activation of " + plugin + " complete -----");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Activation of " + plugin + " failed", e);
        }
    }

    void deactivatePlugin(PluginImpl plugin) {
        plugin.unregisterListeners();
        removeFromActivatedPlugins(plugin.getUri());
    }

    // ---

    /**
     * Checks if all plugins are activated.
     */
    boolean checkAllPluginsActivated() {
        Bundle[] bundles = bundleContext.getBundles();
        int plugins = 0;
        int activated = 0;
        for (Bundle bundle : bundles) {
            if (isDeepaMehtaPlugin(bundle)) {
                plugins++;
                if (isPluginActivated(bundle.getSymbolicName())) {
                    activated++;
                }
            }
        }
        logger.info("### Bundles total: " + bundles.length +
            ", DeepaMehta plugins: " + plugins + ", Activated: " + activated);
        return plugins == activated;
    }

    boolean isPluginActivated(String pluginUri) {
        return activatedPlugins.get(pluginUri) != null;
    }

    // ---

    PluginImpl getPlugin(String pluginUri) {
        PluginImpl plugin = activatedPlugins.get(pluginUri);
        if (plugin == null) {
            throw new RuntimeException("Plugin \"" + pluginUri + "\" not found");
        }
        return plugin;
    }

    Set<PluginInfo> getPluginInfo() {
        Set<PluginInfo> info = new HashSet<PluginInfo>();
        for (PluginImpl plugin : activatedPlugins.values()) {
            info.add(plugin.getInfo());
        }
        return info;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addToActivatedPlugins(PluginImpl plugin) {
        activatedPlugins.put(plugin.getUri(), plugin);
    }

    private void removeFromActivatedPlugins(String pluginUri) {
        activatedPlugins.remove(pluginUri);
    }

    // ---

    /**
     * Checks if an arbitrary bundle is a DeepaMehta plugin.
     */
    private boolean isDeepaMehtaPlugin(Bundle bundle) {
        String packages = (String) bundle.getHeaders().get("Import-Package");
        if (packages == null) {
            return false; // ignore system bundle
        }

        Object activator = bundle.getHeaders().get("Bundle-Activator");
        if (activator != null && ((String) activator).equals(Core.class.getName())) {
            return false; // ignore core bundle
        } else if (packages.contains("de.deepamehta.core.service")) {
            return true; // plugin service
        } else if (packages.contains("de.deepamehta.core.osgi")) {
            return true; // plugin without service implementation
        } else {
            return false; // 3rd party bundles
        }
    }
}
