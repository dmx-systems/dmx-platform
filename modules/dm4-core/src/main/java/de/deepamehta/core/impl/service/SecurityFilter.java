package de.deepamehta.core.impl.service;

import de.deepamehta.core.Topic;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;

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

import com.sun.jersey.core.util.Base64;

import java.io.IOException;
import java.util.logging.Logger;



class SecurityFilter implements Filter {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String INSTALLATION_TYPE = System.getProperty("dm4.installation.type");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    BundleContext context;
    private InstallationType installationType;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    SecurityFilter(BundleContext context) {
        try {
            this.context = context;
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
        HttpSession session = req.getSession(false);
        logger.info("#####      " + req.getRequestURL() +
            "\n      #####      \"Authorization\"=" + authHeader +
            "\n      #####      session=" + session);
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
                    Topic username = getAccessControlService().login(cred.username, cred.password);
                    if (username != null) {
                        logger.info("#####      Logging in with " + cred + " SUCCESSFUL!");
                        allowed = true;
                    } else {
                        logger.info("#####      Logging in with " + cred + " FAILED!");
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

    private AccessControlService getAccessControlService() {
        // ### TODO: should we use a service tracker instead?
        ServiceReference sRef = context.getServiceReference(AccessControlService.class.getName());
        if (sRef != null) {
            // ### TODO: should we unget the service after each use?
            return (AccessControlService) context.getService(sRef);
        } else {
            throw new RuntimeException("AccessControlService not available");
        }
    }

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

    private class Credentials {

        String username;
        String password;

        Credentials(String authHeader) {
            authHeader = authHeader.substring("Basic ".length());
            String[] values = new String(Base64.base64Decode(authHeader)).split(":");
            this.username = values[0];
            this.password = values.length > 1 ? values[1] : "";
        }

        public String toString() {
            return "username=\"" + username + "\", password=\""+ password + "\"";
        }
    }
}
