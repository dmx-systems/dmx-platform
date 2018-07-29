package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;



public interface WebsocketTextMessageListener extends EventListener {

    void websocketTextMessage(String message);
}
