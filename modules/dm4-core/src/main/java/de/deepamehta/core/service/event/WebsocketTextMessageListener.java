package de.deepamehta.core.service.event;

import de.deepamehta.core.service.EventListener;



public interface WebsocketTextMessageListener extends EventListener {

    void websocketTextMessage(String message);
}
