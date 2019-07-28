package systems.dmx.core.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;



class WebSocketConnectionPool {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * 1st hash: plugin URI
     * 2nd hash: client ID
     */
    private Map<String, Map<String, WebSocketConnection>> pool = new ConcurrentHashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    WebSocketConnectionPool() {
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Returns the open WebSocket connections associated to the given plugin, or <code>null</code> if there are none.
     */
    Collection<WebSocketConnection> getConnections(String pluginUri) {
        Map connections = pool.get(pluginUri);
        return connections != null ? connections.values() : null;
    }

    WebSocketConnection getConnection(String pluginUri, String clientId) {
        Map<String, WebSocketConnection> connections = pool.get(pluginUri);
        if (connections == null) {
            logger.warning("No WebSocket connection open for plugin \"" + pluginUri + "\"");
            return null;
        }
        WebSocketConnection connection = connections.get(clientId);
        if (connection == null) {
            logger.warning("No WebSocket connection open for client \"" + clientId + "\" (plugin \"" + pluginUri +
                "\")");
        }
        return connection;
    }

    void add(WebSocketConnection connection) {
        String pluginUri = connection.pluginUri;
        Map connections = pool.get(pluginUri);
        if (connections == null) {
            connections = new ConcurrentHashMap();
            pool.put(pluginUri, connections);
        }
        connections.put(connection.clientId, connection);
    }

    void remove(WebSocketConnection connection) {
        String pluginUri = connection.pluginUri;
        boolean removed = getConnections(pluginUri).remove(connection);
        if (!removed) {
            throw new RuntimeException("Removing a connection of plugin \"" + pluginUri + "\" failed");
        }
    }
}
