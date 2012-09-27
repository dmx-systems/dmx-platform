package de.deepamehta.core.service.event;

import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;



public interface PluginServiceGoneListener extends Listener {

    void pluginServiceGone(PluginService service);
}
