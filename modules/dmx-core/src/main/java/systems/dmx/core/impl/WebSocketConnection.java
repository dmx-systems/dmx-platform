package systems.dmx.core.impl;

import systems.dmx.core.service.CoreService;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import javax.servlet.http.HttpSession;

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
            String clientId;
            HttpSession session;
    private WebSocketConnectionPool pool;
    private CoreService dmx;

    /**
     * The underlying Jetty WebSocket connection.
     */
    private Connection connection;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    WebSocketConnection(String pluginUri, String clientId, HttpSession session, WebSocketConnectionPool pool,
                                                                                CoreService dmx) {
        logger.info("### Associating WebSocket connection (client ID " + clientId + ") with " + info(session));
        this.pluginUri = pluginUri;
        this.clientId = clientId;
        this.session = session;
        this.pool = pool;
        this.dmx = dmx;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocket ***

    @Override
    public void onOpen(Connection connection) {
        logger.info("Opening a WebSocket connection for plugin \"" + pluginUri + "\" (client ID " + clientId + ")");
        this.connection = connection;
        pool.add(this);
    }

    @Override
    public void onClose(int code, String message) {
        logger.info("Closing a WebSocket connection of plugin \"" + pluginUri + "\" (client ID " + clientId + ")");
        pool.remove(this);
    }

    // *** WebSocket.OnTextMessage ***

    @Override
    public void onMessage(String message) {
        try {
            dmx.dispatchEvent(pluginUri, CoreEvent.WEBSOCKET_TEXT_MESSAGE, message);
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

    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Logging ===

    private String info(HttpSession session) {
        return "session" + (session != null ? " " + session.getId() +
            " (username=" + username(session) + ")" : ": null");
    }

    private String username(HttpSession session) {
        return dmx.getPrivilegedAccess().username(session);
    }
}
