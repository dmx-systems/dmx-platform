package systems.dmx.core.impl;

import systems.dmx.core.service.CoreService;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * A WebSocket connection that is bound to a DMX plugin.
 * When a message arrives on the connection the plugin is notified via a WEBSOCKET_TEXT_MESSAGE event.
 * <p>
 * Once the actual WebSocket connection is opened or closed the WebSocketConnection is added/removed to a pool.
 */
class WebSocketConnection implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    String pluginUri;
    String sessionId;

    /**
     * The underlying Jetty WebSocket connection.
     */
    private Connection connection;

    private WebSocketConnectionPool pool;
    private CoreService dm4;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    WebSocketConnection(String pluginUri, String sessionId, WebSocketConnectionPool pool, CoreService dm4) {
        this.pluginUri = pluginUri;
        this.sessionId = sessionId;
        this.pool = pool;
        this.dm4 = dm4;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocket ***

    @Override
    public void onOpen(Connection connection) {
        logger.info("Opening a WebSocket connection for plugin \"" + pluginUri + "\" (session " + sessionId + ")");
        this.connection = connection;
        pool.add(this);
    }

    @Override
    public void onClose(int code, String message) {
        logger.info("Closing a WebSocket connection of plugin \"" + pluginUri + "\" (session " + sessionId + ")");
        pool.remove(this);
    }

    // *** WebSocket.OnTextMessage ***

    @Override
    public void onMessage(String message) {
        try {
            dm4.dispatchEvent(pluginUri, CoreEvent.WEBSOCKET_TEXT_MESSAGE, message);
        } catch (Exception e) {
            // Note: we don't rethrow to Jetty here. It would not log the exception's cause. DM's exception
            // mapper would not kick in either as the plugin is called from Jetty directly, not Jersey.
            logger.log(Level.SEVERE, "An error occurred while dispatching a WebSocket message to plugin \"" +
                pluginUri + "\":", e);
        }
    }

    // *** WebSocket.OnBinaryMessage ***

    @Override
    public void onMessage(byte[] data, int offset, int length) {
        // ### TODO
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void sendMessage(String message) {
        try {
            connection.sendMessage(message);
        } catch (Exception e) {
            pool.remove(this);
            logger.log(Level.SEVERE, "Sending message via " + this + " failed -- connection removed", e);
        }
    }
}
