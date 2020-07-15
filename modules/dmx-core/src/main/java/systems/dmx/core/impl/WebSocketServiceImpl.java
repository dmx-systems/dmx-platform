package systems.dmx.core.impl;

import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.CoreService;
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

    // ### TODO: inject event manager only 
    WebSocketServiceImpl(CoreService dmx) {
        this.dmx = dmx;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** WebSocketService ***

    @Override
    public void sendToOrigin(String message) {
        WebSocketConnectionImpl connection = getConnection();
        if (connection != null) {
            queueMessage(connection, message);
        }
    }

    @Override
    public void sendToAll(String message) {
        broadcast(message, conn -> true);
    }

    @Override
    public void sendToAllButOrigin(String message) {
        broadcast(message, conn -> !conn.getClientId().equals(clientId()));
    }

    @Override
    public void sendToSome(String message, Predicate<WebSocketConnection> connectionFilter) {
        broadcast(message, connectionFilter);
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
            CoreActivator.getHttpService().registerServlet("/websocket", new WebSocketServlet(pool, dmx), null, null);
            worker = new SendMessageWorker();
            worker.start();
            logger.info("WebSocket service started successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting WebSocket service failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void stop() {
        try {
            if (worker != null) {
                logger.info("### Stopping WebSocket service (httpService=" + CoreActivator.getHttpService() + ")");
                // CoreActivator.getHttpService().unregister("/websocket");     // HTTP service already gone
                worker.interrupt();
            } else {
                logger.info("Stopping WebSocket service SKIPPED -- it was not successfully started");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping WebSocket service failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * @return  the WebSocket connection that is associated with the current request (based on "dmx_client_id" cookie),
     *          or null if no such cookie exists or if called outside request scope (e.g. while system startup).
     */
    private WebSocketConnectionImpl getConnection() {
        String clientId = clientId();
        return clientId != null ? pool.getConnection(clientId) : null;
    }

    private void broadcast(String message, Predicate<WebSocketConnection> connectionFilter) {
        pool.getAllConnections().stream().filter(connectionFilter).forEach(conn -> queueMessage(conn, message));
    }

    private void queueMessage(WebSocketConnectionImpl connection, String message) {
        worker.queueMessage(connection, message);
    }

    private String clientId() {
        Cookies cookies = Cookies.get();
        return cookies.has("dmx_client_id") ? cookies.get("dmx_client_id") : null;
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

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
                logger.info("### Terminating SendMessageWorker thread");
            } catch (Exception e) {
                logger.log(Level.WARNING, "An exception occurred in the SendMessageWorker thread -- terminating:", e);
            }
        }

        private void queueMessage(WebSocketConnectionImpl connection, String message) {
            try {
                // logger.info("----- queueing message " + Thread.currentThread().getName());
                messages.put(new QueuedMessage(connection, message));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Queueing a message failed:", e);
            }
        }
    }

    private static class QueuedMessage {

        private WebSocketConnectionImpl connection;
        private String message;

        private QueuedMessage(WebSocketConnectionImpl connection, String message) {
            this.connection = connection;
            this.message = message;
        }
    }
}
