package de.deepamehta.core.service.listener;

import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;



public interface ServiceGoneListener extends Listener {

    void serviceGone(PluginService service);
}
