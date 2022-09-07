package systems.dmx.core.impl;

import systems.dmx.core.service.CoreService;
import systems.dmx.core.util.JavaUtils;

import org.eclipse.jetty.websocket.WebSocket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.logging.Logger;



public class WebSocketServlet extends org.eclipse.jetty.websocket.WebSocketServlet {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketConnectionPool pool;
    private CoreService dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    // ### TODO: inject event manager only 
    WebSocketServlet(WebSocketConnectionPool pool, CoreService dmx) {
        this.pool = pool;
        this.dmx = dmx;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        try {
            return new WebSocketConnectionImpl(clientId(request), session(request), pool, dmx);
        } catch (Exception e) {
            throw new RuntimeException("Opening a WebSocket connection failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String clientId(HttpServletRequest request) {
        String clientId = JavaUtils.cookieValue(request, "dmx_client_id");
        if (clientId == null) {
            throw new RuntimeException("Missing \"dmx_client_id\" cookie in connection upgrade request");
        }
        return clientId;
    }

    private HttpSession session(HttpServletRequest request) {
        // logger.info("request=" + JavaUtils.requestDump(request));
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No (valid) session associated with connection upgrade request");
        }
        return session;
    }
}
