package systems.dmx.core.impl;

import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.accesscontrol.Operation;
import systems.dmx.core.service.websocket.WebSocketConnection;
import systems.dmx.core.service.websocket.WebSocketService;

import org.codehaus.jettison.json.JSONObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;



public class WebSocketServiceImpl implements WebSocketService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String WEBSOCKETS_URL = System.getProperty("dmx.websockets.url", "ws://localhost:8081");
    // Note: the default value is required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default value must match the value defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketConnectionPool pool;       // instantiated in start()
    private SendMessageWorker worker;           // instantiated in start()
    private CoreService dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    WebSocketServiceImpl(CoreService dmx) {
        this.dmx = dmx;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocketService ***

    @Override
    public void sendToOrigin(String message) {
        WebSocketConnectionImpl connection = getConnection();
        if (connection != null) {
            queueMessage(message, connection);
        }
    }

    @Override
    public void sendToAll(String message) {
        queueMessage(message, conn -> true);
    }

    @Override
    public void sendToAllButOrigin(String message) {
        queueMessage(message, isOrigin().negate());
    }

    @Override
    public void sendToReadAllowed(String message, long objectId) {
        // don't send back to origin
        // only send if receiver has READ permission for object
        queueMessage(message, isOrigin().negate().and(isReadAllowed(objectId)));
    }

    @Override
    public void sendToSome(String message, Predicate<WebSocketConnection> connectionFilter) {
        queueMessage(message, connectionFilter);
    }

    // ---

    @Override
    public String getWebSocketURL() {
        return WEBSOCKETS_URL;
    }

    // ---

    public void start() {
        try {
            logger.info("##### Starting WebSocket service");
            pool = new WebSocketConnectionPool();
            worker = new SendMessageWorker();
            worker.start();
            CoreActivator.getHttpService().registerServlet("/websocket", new WebSocketServlet(pool, dmx), null, null);
            logger.info("WebSocket service started successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting WebSocket service failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void stop() {
        try {
            if (pool != null) {
                logger.info("### Stopping WebSocket service (httpService=" + CoreActivator.getHttpService() + ")");
                // CoreActivator.getHttpService().unregister("/websocket");     // HTTP service already gone
                worker.interrupt();
                pool.close();
            } else {
                logger.info("Stopping WebSocket service SKIPPED -- it was not successfully started");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping WebSocket service failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void queueMessage(String message, WebSocketConnectionImpl connection) {
        worker.queueMessage(message, connection);
    }

    private void queueMessage(String message, Predicate<WebSocketConnection> connectionFilter) {
        worker.queueMessage(message, connectionFilter);
    }

    // ---

    private Predicate<WebSocketConnection> isOrigin() {
        // Note: the returned predicate is evaluated in another thread (SendMessageWorker). So to read out the client-id
        // cookie -- which is stored thread-locally -- we call clientId() from *this* thread (instead from predicate)
        // and hold the result in the predicate's closure.
        String clientId = clientId();
        return conn -> {
            boolean isOrigin = conn.getClientId().equals(clientId);
            logger.info(conn.getClientId() + " " + conn.getUsername() + " (isOrigin) -> " + isOrigin);
            return isOrigin;
        };
    }

    private Predicate<WebSocketConnection> isReadAllowed(long objectId) {
        return conn -> {
            boolean isReadAllowed = dmx.getPrivilegedAccess().hasPermission(conn.getUsername(), Operation.READ, objectId);
            logger.info(conn.getClientId() + " " + conn.getUsername() + " (isReadAllowed) -> " + isReadAllowed);
            return isReadAllowed;
        };
    }

    // ---

    /**
     * @return  the WebSocket connection that is associated with the current request (based on "dmx_client_id" cookie),
     *          or null if no such cookie exists or if called outside request scope (e.g. while system startup).
     */
    private WebSocketConnectionImpl getConnection() {
        String clientId = clientId();
        return clientId != null ? pool.getConnection(clientId) : null;
    }

    private String clientId() {
        Cookies cookies = Cookies.get();
        return cookies.has("dmx_client_id") ? cookies.get("dmx_client_id") : null;
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class SendMessageWorker extends Thread {

        private BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue();

        private SendMessageWorker() {
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            boolean stopped = false;
            while (!stopped) {
                MessageTask task = null;
                try {
                    task = messageQueue.take();
                    task.sendMessage();
                    yield();
                } catch (InterruptedException e) {
                    stopped = true;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "An error occurred in the SendMessageWorker while processing a \"" +
                        task.getMessageType() + "\" task (aborting this task):", e);
                }
            }
            logger.info("### Terminating SendMessageWorker");
        }

        private void queueMessage(String message, WebSocketConnectionImpl connection) {
            try {
                messageQueue.put(new MessageTask(message, connection));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Queueing a message failed:", e);
            }
        }

        private void queueMessage(String message, Predicate<WebSocketConnection> connectionFilter) {
            try {
                messageQueue.put(new MessageTask(message, connectionFilter));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Queueing a message failed:", e);
            }
        }
    }

    private class MessageTask {

        private String message;

        private WebSocketConnectionImpl connection;
        private Predicate<WebSocketConnection> connectionFilter;

        /**
         * A send-to-one task.
         */
        private MessageTask(String message, WebSocketConnectionImpl connection) {
            this.message = message;
            this.connection = connection;
        }

        /**
         * A send-to-many task.
         */
        private MessageTask(String message, Predicate<WebSocketConnection> connectionFilter) {
            this.message = message;
            this.connectionFilter = connectionFilter;
        }

        // ---

        private void sendMessage() {
            if (connection != null) {
                _sendMessage(connection);
            } else {
                pool.getAllConnections().stream().filter(connectionFilter).forEach(conn -> _sendMessage(conn));
            }
        }

        private void _sendMessage(WebSocketConnectionImpl conn) {
            conn.sendMessage(message);
        }

        // ---

        private String getMessageType() {
            try {
                return new JSONObject(message).getString("type");
            } catch (Exception e) {
                throw new RuntimeException("JSON parsing error", e);
            }
        }
    }
}
