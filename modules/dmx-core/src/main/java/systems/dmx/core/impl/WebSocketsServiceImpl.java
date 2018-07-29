package systems.dmx.core.impl;

import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.WebSocketsService;
import systems.dmx.core.util.JavaUtils;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;



class WebSocketsServiceImpl implements WebSocketsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final int    WEBSOCKETS_PORT = Integer.getInteger("dm4.websockets.port", 8081);
    private static final String WEBSOCKETS_URL = System.getProperty("dm4.websockets.url", "ws://localhost:8081");
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketsServer server;
    private WebSocketConnectionPool pool = new WebSocketConnectionPool();
    private SendMessageWorker worker = new SendMessageWorker();
    private CoreService dm4;

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
    public void messageToAllButOne(HttpServletRequest request, String pluginUri, String message) {
        WebSocketConnection connection = getConnection(request, pluginUri);
        if (connection != null) {
            broadcast(pluginUri, message, connection);
        }
    }

    @Override
    public void messageToOne(HttpServletRequest request, String pluginUri, String message) {
        WebSocketConnection connection = getConnection(request, pluginUri);
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
            logger.info("##### Starting Jetty WebSocket server #####");
            server = new WebSocketsServer(WEBSOCKETS_PORT);
            server.start();
            worker.start();
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
     * @return  the WebSocket connection that is associated with the given request (based on its session cookie),
     *          or null if called outside request scope (e.g. while system startup).
     *
     * @throws  RuntimeException    if no valid session is associated with the request.
     */
    private WebSocketConnection getConnection(HttpServletRequest request, String pluginUri) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new RuntimeException("No valid session is associated with the request");
            }
            return pool.getConnection(pluginUri, session.getId());
        } catch (IllegalStateException e) {
            // Note: this happens if "request" is accessed outside request scope, e.g. while system startup.
            return null;
        }
    }

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
            // TODO: drop anonymous connections
            return sessionId != null ? sessionId : "anonymous-" + counter++;
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
