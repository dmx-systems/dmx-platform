package systems.dmx.core.impl;

import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.WebSocketsService;
import systems.dmx.core.util.JavaUtils;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;



class WebSocketsServiceImpl implements WebSocketsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final int    WEBSOCKETS_PORT = Integer.getInteger("dmx.websockets.port", 8081);
    private static final String WEBSOCKETS_URL = System.getProperty("dmx.websockets.url", "ws://localhost:8081");
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketsServer server;
    private WebSocketConnectionPool pool = new WebSocketConnectionPool();
    private SendMessageWorker worker = new SendMessageWorker();
    private CoreService dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    // ### TODO: inject event manager only 
    WebSocketsServiceImpl(CoreService dmx) {
        this.dmx = dmx;
        init();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocketsService ***

    @Override
    public void messageToAll(String pluginUri, String message) {
        broadcast(pluginUri, message, null);    // exclude=null
    }

    @Override
    public void messageToAllButOne(HttpServletRequest request, String pluginUri, String message) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        broadcast(pluginUri, message, getConnection(pluginUri));
    }

    @Override
    public void messageToOne(HttpServletRequest request, String pluginUri, String message) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        WebSocketConnection connection = getConnection(pluginUri);
        if (connection != null) {
            queueMessage(connection, message);
        }
    }

    // ---

    @Override
    public String getWebSocketsURL() {
        return WEBSOCKETS_URL;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void init() {
        try {
            logger.info("##### Starting Jetty WebSocket server");
            server = new WebSocketsServer(WEBSOCKETS_PORT);
            server.start();
            worker.start();
            // ### server.join();
            logger.info("Jetty WebSocket server started successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting Jetty WebSocket server failed", e);
        }
    }

    void shutdown() {
        try {
            if (server != null) {
                logger.info("### Stopping Jetty WebSocket server");
                worker.interrupt();
                server.stop();
            } else {
                logger.info("Stopping Jetty WebSocket server SKIPPED -- not yet started");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping Jetty WebSocket server failed", e);
        }
    }

    // ---

    /**
     * @return  the WebSocket connection that is associated with the current request (based on its "dmx_client_id"
     *          cookie), or null if no such cookie exists or if called outside request scope (e.g. while system
     *          startup).
     */
    private WebSocketConnection getConnection(String pluginUri) {
        String clientId = clientId();
        return clientId != null ? pool.getConnection(pluginUri, clientId) : null;
    }

    /**
     * @param   exclude     may be null
     */
    private void broadcast(String pluginUri, String message, WebSocketConnection exclude) {
        Collection<WebSocketConnection> connections = pool.getConnections(pluginUri);
        if (connections != null) {
            for (WebSocketConnection connection : connections) {
                if (connection != exclude) {
                    queueMessage(connection, message);
                }
            }
        }
    }

    private void queueMessage(WebSocketConnection connection, String message) {
        worker.queueMessage(connection, message);
    }

    private String clientId() {
        Cookies cookies = Cookies.get();
        return cookies.has("dmx_client_id") ? cookies.get("dmx_client_id") : null;
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class WebSocketsServer extends Server {

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
                    try {
                        checkProtocol(protocol);
                        return new WebSocketConnection(protocol, clientId(request), pool, dmx);
                    } catch (Exception e) {
                        throw new RuntimeException("Opening a WebSocket connection " +
                            (protocol != null ? "for plugin \"" + protocol + "\" " : "") + "failed", e);
                    }
                }
            });
        }

        private void checkProtocol(String pluginUri) {
            if (pluginUri == null) {
                throw new RuntimeException("A plugin URI is missing in the WebSocket handshake -- Add your " +
                    "plugin's URI as the 2nd argument to the JavaScript WebSocket constructor");
            } else {
                dmx.getPlugin(pluginUri);   // check plugin URI, throws if invalid
            }
        }

        private String clientId(HttpServletRequest request) {
            String clientId = JavaUtils.cookieValue(request, "dmx_client_id");
            if (clientId == null) {
                throw new RuntimeException("Missing \"dmx_client_id\" cookie");
            }
            return clientId;
        }
    }

    private class SendMessageWorker extends Thread {

        private BlockingQueue<QueuedMessage> messages = new LinkedBlockingQueue();

        private SendMessageWorker() {
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    QueuedMessage message = messages.take();
                    yield();
                    // logger.info("----- sending message " + Thread.currentThread().getName());
                    message.connection.sendMessage(message.message);
                }
            } catch (InterruptedException e) {
                logger.info("### SendMessageWorker thread received an InterruptedException -- terminating");
            } catch (Exception e) {
                logger.log(Level.WARNING, "An exception occurred in the SendMessageWorker thread -- terminating:", e);
            }
        }

        private void queueMessage(WebSocketConnection connection, String message) {
            try {
                // logger.info("----- queueing message " + Thread.currentThread().getName());
                messages.put(new QueuedMessage(connection, message));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Queueing a message failed:", e);
            }
        }
    }

    private static class QueuedMessage {

        private WebSocketConnection connection;
        private String message;

        private QueuedMessage(WebSocketConnection connection, String message) {
            this.connection = connection;
            this.message = message;
        }
    }
}
