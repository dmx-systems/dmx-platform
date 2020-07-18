package systems.dmx.core.impl;

import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.accesscontrol.Operation;
import systems.dmx.core.service.websocket.WebSocketConnection;
import systems.dmx.core.service.websocket.WebSocketService;

import javax.servlet.http.HttpServletRequest;

import java.util.Collection;
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
            if (isOrigin) {
                logger.info(conn.getClientId() + " " + conn.getUsername() + " (origin) -> " + false);
            }
            return isOrigin;
        };
    }

    private Predicate<WebSocketConnection> isReadAllowed(long objectId) {
        return conn -> {
            boolean isReadAllowed = dmx.getPrivilegedAccess().hasPermission(conn.getUsername(), Operation.READ, objectId);
            logger.info(conn.getClientId() + " " + conn.getUsername() + " -> " + isReadAllowed);
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
            try {
                while (true) {
                    messageQueue.take().send();
                    yield();
                }
            } catch (InterruptedException e) {
                logger.info("Terminating SendMessageWorker");
            } catch (Exception e) {
                logger.log(Level.WARNING, "An exception occurred in the SendMessageWorker -- terminating:", e);
            }
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

        private void send() {
            if (connection != null) {
                _send(connection);
            } else {
                pool.getAllConnections().stream().filter(connectionFilter).forEach(conn -> _send(conn));
            }
        }

        private void _send(WebSocketConnectionImpl conn) {
            conn.sendMessage(message);
        }
    }
}
