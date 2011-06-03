package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.Plugin;

import java.util.HashMap;
import java.util.Map;



/**
 * A cache that holds the registered {@link Plugin} instances.
 * <p>
 * A PluginCache singleton is hold by the {@link EmbeddedService} and is accessed concurrently by all
 * bundle activation threads (as created by the File Install bundle).
 * <p>
 * Iteration over all plugins is synchronized with cache modifications ({@link put}, {@link remove}).
 * To iterate create a {@link PluginCache.Iterator} instance and provide a {@link #body} method.
 */
class PluginCache {

    /**
     * Registered plugins.
     * Hashed by plugin bundle's symbolic name, e.g. "de.deepamehta.3-topicmaps".
     */
    static private Map<String, Plugin> plugins = new HashMap();

    Plugin get(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new RuntimeException("Plugin \"" + pluginId + "\" not found");
        }
        return plugin;
    }

    void put(Plugin plugin) {
        synchronized(plugins) {
            plugins.put(plugin.getId(), plugin);
        }
    }

    void remove(String pluginId) {
        synchronized(plugins) {
            plugins.remove(pluginId);
        }
    }

    boolean contains(String pluginId) {
        synchronized(plugins) {
            return plugins.get(pluginId) != null;
        }
    }

    static class Iterator {

        Iterator() {
            synchronized(plugins) {
                for (Plugin plugin : plugins.values()) {
                    body(plugin);
                }
            }
        }

        void body(Plugin plugin) {
        }
    }
}
