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



class RequestFilter implements Filter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    SecurityContext securityContext;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RequestFilter(SecurityContext securityContext) {
        this.securityContext = securityContext;
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
                throw new ServletException("Unexpected AccessControlException", e);
            }
        }
    }

    @Override
    public void destroy() {
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"DeepaMehta\"");
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("You're not authorized. Sorry.");  // throws IOException
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Content-Type", "text/html");    // for text/plain (default) Safari provides no Web Console
        response.getWriter().println("Access is forbidden. Sorry.");    // throws IOException
    }
}
