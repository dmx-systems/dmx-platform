package systems.dmx.core.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;



class WebSocketConnectionPool {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * key: client ID
     */
    private Map<String, WebSocketConnectionImpl> pool = new ConcurrentHashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    WebSocketConnectionPool() {
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    WebSocketConnectionImpl getConnection(String clientId) {
        WebSocketConnectionImpl connection = pool.get(clientId);
        if (connection == null) {
            logger.warning("No WebSocket connection open for client ID " + clientId);
        }
        return connection;
    }

    Collection<WebSocketConnectionImpl> getAllConnections() {
        return pool.values();
    }

    void addConnection(WebSocketConnectionImpl connection) {
        pool.put(connection.clientId, connection);
    }

    void removeConnection(WebSocketConnectionImpl connection) {
        boolean removed = pool.remove(connection.clientId) != null;
        if (!removed) {
            throw new RuntimeException("Can't remove WebSocket connection " + connection.clientId + " from pool");
        }
    }

    void close() {
        logger.info("Closing " + pool.size() + " WebSocket connections");
        getAllConnections().forEach(WebSocketConnectionImpl::close);
    }
}
