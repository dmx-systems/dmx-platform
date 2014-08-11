package de.deepamehta.core.impl;

import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.PluginInfo;

import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Activates and deactivates plugins and keeps a pool of activated plugins.
 * The pool of activated plugins is a shared resource. All access to it is synchronized.
 * <p>
 * A PluginManager singleton is hold by the {@link EmbeddedService} and is accessed concurrently
 * by all bundle activation threads (as created e.g. by the File Install bundle).
 */
class PluginManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The pool of activated plugins.
     *
     * Hashed by plugin bundle's symbolic name, e.g. "de.deepamehta.topicmaps".
     */
    private Map<String, PluginImpl> activatedPlugins = new HashMap();

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginManager(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Activates a plugin.
     * Called once the plugin's requirements are met (see PluginImpl.checkRequirementsForActivation()).
     * <p>
     * Once the plugin is activated checks if <i>all</i> installed plugins are activated now, and if so, fires the
     * {@link CoreEvent.ALL_PLUGINS_ACTIVE} core event.
     * <p>
     * If the plugin is already activated, nothing is performed. This happens e.g. when a dependent plugin is redeployed.
     * <p>
     * Note: this method is synchronized. While a plugin is activated no other plugin must be activated. Otherwise
     * the "type introduction" mechanism might miss some types. Consider this unsynchronized scenario: plugin B
     * starts running its migrations just in the moment between plugin A's type introduction and event listener
     * registration. Plugin A might miss some of the types created by plugin B.
     */
    synchronized void activatePlugin(PluginImpl plugin) {
        // Note: we must not activate a plugin twice.
        if (!_isPluginActivated(plugin.getUri())) {
            plugin.activate();
            addToActivatedPlugins(plugin);
            //
            if (checkAllPluginsActivated()) {
                logger.info("########## All Plugins Active ##########");
                dms.fireEvent(CoreEvent.ALL_PLUGINS_ACTIVE);
            }
        } else {
            logger.info("Activation of " + plugin + " ABORTED -- already activated");
        }
    }

    synchronized void deactivatePlugin(PluginImpl plugin) {
        // Note: if plugin activation failed its listeners are not registered and it is not in the pool of activated
        // plugins. Unregistering the listeners and removing from pool would fail.
        String pluginUri = plugin.getUri();
        if (_isPluginActivated(pluginUri)) {
            plugin.unregisterListeners();
            removeFromActivatedPlugins(pluginUri);
        } else {
            logger.info("Deactivation of " + plugin + " ABORTED -- it was not successfully activated");
        }
    }

    // ---

    synchronized boolean isPluginActivated(String pluginUri) {
        return _isPluginActivated(pluginUri);
    }

    // ---

    synchronized PluginImpl getPlugin(String pluginUri) {
        PluginImpl plugin = activatedPlugins.get(pluginUri);
        if (plugin == null) {
            throw new RuntimeException("Plugin \"" + pluginUri + "\" not found");
        }
        return plugin;
    }

    synchronized List<PluginInfo> getPluginInfo() {
        List info = new ArrayList();
        for (PluginImpl plugin : activatedPlugins.values()) {
            info.add(plugin.getInfo());
        }
        return info;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Checks if all installed plugins are activated.
     */
    private boolean checkAllPluginsActivated() {
        Bundle[] bundles = dms.bundleContext.getBundles();
        int plugins = 0;
        int activated = 0;
        for (Bundle bundle : bundles) {
            if (isDeepaMehtaPlugin(bundle)) {
                plugins++;
                if (_isPluginActivated(bundle.getSymbolicName())) {
                    activated++;
                }
            }
        }
        logger.info("### Bundles total: " + bundles.length +
            ", DeepaMehta plugins: " + plugins + ", Activated: " + activated);
        return plugins == activated;
    }

    /**
     * Plugin detection: checks if an arbitrary bundle is a DeepaMehta plugin.
     */
    private boolean isDeepaMehtaPlugin(Bundle bundle) {
        try {
            String activatorClassName = (String) bundle.getHeaders().get("Bundle-Activator");
            if (activatorClassName != null) {
                Class activatorClass = bundle.loadClass(activatorClassName);    // throws ClassNotFoundException
                return PluginActivator.class.isAssignableFrom(activatorClass);
            } else {
                // Note: 3rd party bundles may have no activator
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Plugin detection failed for bundle " + bundle, e);
        }
    }

    // ---

    private void addToActivatedPlugins(PluginImpl plugin) {
        activatedPlugins.put(plugin.getUri(), plugin);
    }

    private void removeFromActivatedPlugins(String pluginUri) {
        if (activatedPlugins.remove(pluginUri) == null) {
            throw new RuntimeException("Removing plugin \"" + pluginUri + "\" from pool of activated plugins failed: " +
                "not found in " + activatedPlugins);
        }
    }

    private boolean _isPluginActivated(String pluginUri) {
        return activatedPlugins.get(pluginUri) != null;
    }
}
