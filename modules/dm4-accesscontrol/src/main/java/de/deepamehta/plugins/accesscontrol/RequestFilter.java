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

    private static final String READ_REQUIRES_LOGIN = System.getProperty("dm4.security.read_requires_login");
    private static final String WRITE_REQUIRES_LOGIN = System.getProperty("dm4.security.write_requires_login");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    SecurityContext securityContext;

    private boolean readRequiresLogin;
    private boolean writeRequiresLogin;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    RequestFilter(SecurityContext securityContext) {
        this.securityContext = securityContext;
        //
        this.readRequiresLogin = Boolean.valueOf(READ_REQUIRES_LOGIN);
        this.writeRequiresLogin = Boolean.valueOf(WRITE_REQUIRES_LOGIN);
        //
        logger.info("########## Security settings:\n                 readRequiresLogin=" + readRequiresLogin +
            "\n                 writeRequiresLogin=" + writeRequiresLogin);
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
        boolean loginRequired = isLoginRequired(req);
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

    private boolean isLoginRequired(HttpServletRequest request) {
        return request.getMethod().equals("GET") ? readRequiresLogin : writeRequiresLogin;
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"DeepaMehta\"");
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("Not authorized. Sorry.");     // throws IOException
    }
}
