package systems.dmx.core.service;

import javax.servlet.http.HttpServletRequest;



public interface WebSocketsService {

    void messageToAll(String pluginUri, String message);

    // ### TODO: drop "request" parameter?
    void messageToAllButOne(HttpServletRequest request, String pluginUri, String message);

    // ### TODO: drop "request" parameter?
    void messageToOne(HttpServletRequest request, String pluginUri, String message);

    // ---

    String getWebSocketsURL();
}
