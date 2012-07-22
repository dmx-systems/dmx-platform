package de.deepamehta.core.service.listener;

import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;



public interface PluginServiceArrivedListener extends Listener {

    void pluginServiceArrived(PluginService service);
}
