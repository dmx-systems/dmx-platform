package de.deepamehta.core.service.event;

import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;



public interface PluginServiceArrivedListener extends Listener {

    void pluginServiceArrived(PluginService service);
}
