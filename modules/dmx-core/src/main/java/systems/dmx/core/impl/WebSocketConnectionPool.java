package systems.dmx.core.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



class WebSocketConnectionPool {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * 1st hash: plugin URI
     * 2nd hash: session ID
     */
    private Map<String, Map<String, WebSocketConnection>> pool = new ConcurrentHashMap();

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

    WebSocketConnection getConnection(String pluginUri, String sessionId) {
        Map<String, WebSocketConnection> connections = pool.get(pluginUri);
        if (connections == null) {
            throw new RuntimeException("No WebSocket connection open for plugin \"" + pluginUri + "\"");
        }
        WebSocketConnection connection = connections.get(sessionId);
        if (connection == null) {
            throw new RuntimeException("No WebSocket connection open for session \"" + sessionId + "\" (plugin \"" +
                pluginUri + "\")");
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
        connections.put(connection.sessionId, connection);
    }

    void remove(WebSocketConnection connection) {
        String pluginUri = connection.pluginUri;
        boolean removed = getConnections(pluginUri).remove(connection);
        if (!removed) {
            throw new RuntimeException("Removing a connection of plugin \"" + pluginUri + "\" failed");
        }
    }
}
