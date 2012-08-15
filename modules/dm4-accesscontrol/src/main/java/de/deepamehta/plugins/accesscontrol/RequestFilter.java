package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.model.Credentials;
import de.deepamehta.core.Topic;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.util.logging.Logger;



class RequestFilter implements Filter {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String INSTALLATION_TYPE = System.getProperty("dm4.installation.type");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    SecurityContext securityContext;
    private InstallationType installationType;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    RequestFilter(SecurityContext securityContext) {
        try {
            this.securityContext = securityContext;
            this.installationType = InstallationType.valueOf(INSTALLATION_TYPE);
            logger.info("########## Installation type is \"" + installationType + "\"");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("\"" + INSTALLATION_TYPE + "\" is an unexpected installation type");
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                     ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String authHeader = req.getHeader("Authorization");
        HttpSession session = req.getSession(false);    // create=false
        logger.info("#####      " + req.getMethod() + " " + req.getRequestURL() +
            "\n      #####      \"Authorization\"=\"" + authHeader + "\"" + 
            "\n      #####      " + info(session));
        //
        boolean isReadRequest = req.getMethod().equals("GET");
        boolean loginRequired = !installationType.lookup(isReadRequest);
        boolean allowed = false;
        if (loginRequired) {
            if (session != null) {
                allowed = true;
            } else {
                if (authHeader != null) {
                    Credentials cred = new Credentials(authHeader);
                    Topic username = securityContext.login(cred.username, cred.password, req);
                    if (username != null) {
                        allowed = true;
                    }
                }
            }
        } else {
            allowed = true;
        }
        //
        if (allowed) {
            chain.doFilter(request, response);
        } else {
            unauthorized(resp);
        }
    }

    @Override
    public void destroy() {
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### FIXME: there is a copy in AccessControlPlugin
    private String info(HttpSession session) {
        return "session" + (session != null ? " " + session.getId() +
            " (username=\"" + getUsername(session) + "\")" : ": null");
    }
    
    // ### FIXME: there is a principal copy in AccessControlPlugin
    private String getUsername(HttpSession session) {
        Topic username = (Topic) session.getAttribute("username");
        if (username == null) {
            throw new RuntimeException("Session data inconsistency: \"username\" attribute is missing");
        }
        return username.getSimpleValue().toString();
    }

    // ---

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"DeepaMehta\"");
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("Not authorized. Sorry.");     // throws IOException
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private enum InstallationType {

        SINGLE_USER(true, true),
        PRIVATE(false, false),
        PUBLIC(true, false),
        SANDBOX(true, true);    // Note: the difference between SINGLE_USER and SANDBOX is the network interface

        boolean readAllowed;
        boolean writeAllowed;

        InstallationType(boolean readAllowed, boolean writeAllowed) {
            this.readAllowed = readAllowed;
            this.writeAllowed = writeAllowed;
        }

        boolean lookup(boolean isReadRequest) {
            return isReadRequest ? readAllowed : writeAllowed;
        }
    }
}
