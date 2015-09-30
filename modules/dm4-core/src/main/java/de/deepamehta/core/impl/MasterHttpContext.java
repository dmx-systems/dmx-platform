package de.deepamehta.core.impl;

import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



class MasterHttpContext implements HttpContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, HttpContext> httpContexts = new HashMap();

    private ThreadLocal<HttpServletRequest> threadLocalRequest = new ThreadLocal();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** HttpContext Implementation ***
    // **********************************



    @Override
    public URL getResource(String name) {
        String uriNamespace = threadLocalRequest.get().getServletPath();
        String resourceName = resourceName(name, uriNamespace);
        return httpContexts.get(uriNamespace).getResource(resourceName);
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        threadLocalRequest.set(request);
        //
        String uriNamespace = uriNamespace(request);
        return httpContexts.get(uriNamespace).handleSecurity(request, response);
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void add(String uriNamespace, HttpContext httpContext) {
        if (httpContexts.get(uriNamespace) != null) {
            throw new RuntimeException("Master context already contains a HttpContext for URI namespace \"" +
                uriNamespace + "\"");
        }
        httpContexts.put(uriNamespace, httpContext);
    }

    void remove(String uriNamespace) {
        if (httpContexts.get(uriNamespace) == null) {
            throw new RuntimeException("Master context contains no HttpContext for URI namespace \"" +
                uriNamespace + "\"");
        }
        httpContexts.remove(uriNamespace);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String resourceName(String name, String uriNamespace) {
        // logger.info("### name=\"" + name + "\", uriNamespace=\"" + uriNamespace + "\"");
        if (name.startsWith(uriNamespace)) {
            name = name.substring(uriNamespace.length());
        }
        return name;
    }

    private String uriNamespace(HttpServletRequest request) {
        String uriNamespace = request.getServletPath();
        if (uriNamespace.equals("")) {
            uriNamespace = "/*";
        }
        return uriNamespace;
    }
}
