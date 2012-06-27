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
     * Hashed by plugin bundle's symbolic name, e.g. "de.deepamehta.topicmaps".
     */
    static private Map<String, Plugin> plugins = new HashMap();

    Plugin get(String pluginUri) {
        Plugin plugin = plugins.get(pluginUri);
        if (plugin == null) {
            throw new RuntimeException("Plugin \"" + pluginUri + "\" not found");
        }
        return plugin;
    }

    void put(Plugin plugin) {
        synchronized(plugins) {
            plugins.put(plugin.getUri(), plugin);
        }
    }

    void remove(String pluginUri) {
        synchronized(plugins) {
            plugins.remove(pluginUri);
        }
    }

    boolean contains(String pluginUri) {
        synchronized(plugins) {
            return plugins.get(pluginUri) != null;
        }
    }

    static abstract class Iterator {

        Iterator() {
            synchronized(plugins) {
                for (Plugin plugin : plugins.values()) {
                    body(plugin);
                }
            }
        }

        abstract void body(Plugin plugin);
    }
}
