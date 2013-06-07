package de.deepamehta.plugins.accesscontrol;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



class RequestFilter implements Filter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    SecurityContext securityContext;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    RequestFilter(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** Filter Implementation ***

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                     ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            //
            securityContext.checkRequest(req);  // throws AccessControlException
            //
            chain.doFilter(request, response);
            //
        } catch (AccessControlException e) {
            switch (e.getStatusCode()) {
            case HttpServletResponse.SC_UNAUTHORIZED:
                unauthorized(resp);
                break;
            case HttpServletResponse.SC_FORBIDDEN:
                forbidden(resp);
                break;
            default:
                // Note: when file logging is activated the ServletException does not appear in the log file but on the
                // console. Apparently the servlet container does not log the ServletException via the Java Logging API.
                // So we log it explicitly (and it appears twice on the console if file logging is not activated).
                logger.log(Level.SEVERE, "Unexpected AccessControlException", e);
                throw new ServletException("Unexpected AccessControlException", e);
            }
        } catch (Exception e) {
            // Note: when file logging is activated the ServletException does not appear in the log file but on the
            // console. Apparently the servlet container does not log the ServletException via the Java Logging API.
            // So we log it explicitly (and it appears twice on the console if file logging is not activated).
            logger.log(Level.SEVERE, "Request filtering failed", e);
            throw new ServletException("Request filtering failed", e);
        }
    }

    @Override
    public void destroy() {
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void unauthorized(HttpServletResponse response) throws IOException {
        // Note: "xBasic" is a contrived authentication scheme to suppress the browser's login dialog.
        // http://loudvchar.blogspot.ca/2010/11/avoiding-browser-popup-for-401.html
        String authScheme = securityContext.useBrowserLoginDialog() ? "Basic" : "xBasic";
        String realm = securityContext.getAuthenticationRealm();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", authScheme + " realm=" + realm);
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("You're not authorized. Sorry.");  // throws IOException
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("Access is forbidden. Sorry.");    // throws IOException
    }
}
