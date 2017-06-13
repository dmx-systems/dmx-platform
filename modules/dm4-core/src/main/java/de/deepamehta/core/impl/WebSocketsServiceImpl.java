package de.deepamehta.core.impl;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.WebSocketsService;
import de.deepamehta.core.util.JavaUtils;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;



@Path("/websockets")
@Produces("application/json")
class WebSocketsServiceImpl implements WebSocketsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final int WEBSOCKETS_PORT = Integer.getInteger("dm4.websockets.port", 8081);
    private static final String WEBSOCKETS_URL = System.getProperty("dm4.websockets.url", "ws://localhost:8081");
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketsServer server;
    private WebSocketConnectionPool pool = new WebSocketConnectionPool();
    private CoreService dm4;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    // ### TODO: inject event manager only 
    WebSocketsServiceImpl(CoreService dm4) {
        this.dm4 = dm4;
        init();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocketsService ***

    @Override
    public void messageToAll(String pluginUri, String message) {
        broadcast(pluginUri, message, null);    // exclude=null
    }

    @Override
    public void messageToAllButOne(String pluginUri, String message) {
        broadcast(pluginUri, message, getConnection(pluginUri));
    }

    @Override
    public void messageToOne(String pluginUri, String message) {
        getConnection(pluginUri).sendMessage(message);
    }

    // *** REST Resource (not part of OSGi service) ***

    @GET
    public JSONEnabled getConfig() {
        return new JSONEnabled() {
            @Override
            public JSONObject toJSON() {
                try {
                    return new JSONObject().put("dm4.websockets.url", WEBSOCKETS_URL);
                } catch (JSONException e) {
                    throw new RuntimeException("Serializing the WebSockets configuration failed", e);
                }
            }
        };
    }

    // ---

    private void init() {
        try {
            logger.info("##### Starting Jetty WebSocket server #####");
            server = new WebSocketsServer(WEBSOCKETS_PORT);
            server.start();
            // ### server.join();
            logger.info("### Jetty WebSocket server started successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting Jetty WebSocket server failed", e);
        }
    }

    void shutdown() {
        try {
            if (server != null) {
                logger.info("##### Stopping Jetty WebSocket server #####");
                server.stop();
            } else {
                logger.info("Stopping Jetty WebSocket server ABORTED -- not yet started");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping Jetty WebSocket server failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Returns the WebSocket connection that is associated to the current request, based on the request's session ID.
     */
    private WebSocketConnection getConnection(String pluginUri) {
        if (request == null) {
            throw new RuntimeException("No request is injected");
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No valid session is associated with this request");
        }
        return pool.getConnection(pluginUri, session.getId());
    }

    private void broadcast(String pluginUri, String message, WebSocketConnection exclude) {
        Collection<WebSocketConnection> connections = pool.getConnections(pluginUri);
        if (connections != null) {
            for (WebSocketConnection connection : connections) {
                if (connection != exclude) {
                    connection.sendMessage(message);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class WebSocketsServer extends Server {

        private int counter = 0;     // counts anonymous connections

        private WebSocketsServer(int port) {
            // add connector
            Connector connector = new SelectChannelConnector();
            connector.setPort(port);
            addConnector(connector);
            //
            // set handler
            setHandler(new WebSocketHandler() {
                @Override
                public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
                    checkProtocol(protocol);
                    return new WebSocketConnection(protocol, sessionId(request), pool, dm4);
                }
            });
        }

        private void checkProtocol(String pluginUri) {
            try {
                if (pluginUri == null) {
                    throw new RuntimeException("A plugin URI is missing in the WebSocket handshake -- Add your " +
                        "plugin's URI as the 2nd argument to the JavaScript WebSocket constructor");
                } else {
                    dm4.getPlugin(pluginUri);   // check plugin URI, throws if invalid
                }
            } catch (Exception e) {
                throw new RuntimeException("Opening a WebSocket connection " +
                    (pluginUri != null ? "for plugin \"" + pluginUri + "\" " : "") + "failed", e);
            }
        }

        private String sessionId(HttpServletRequest request) {
            String sessionId = JavaUtils.cookieValue(request, "JSESSIONID");
            return sessionId != null ? sessionId : "anonymous-" + counter++;
        }
    }
}
