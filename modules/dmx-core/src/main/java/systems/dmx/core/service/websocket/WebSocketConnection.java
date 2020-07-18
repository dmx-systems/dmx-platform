package systems.dmx.core.service.websocket;



public interface WebSocketConnection {

    String getClientId();

    /**
     * @return  the username associated with this WebSocket connection, or null if no one is associated (= not logged in).
     */
    String getUsername();
}
