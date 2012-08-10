package de.deepamehta.core.impl.service;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import java.util.logging.Logger;



class RequestFilter implements Filter {

    private Logger logger = Logger.getLogger(getClass().getName());

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                     ServletException {
        logger.info("#####      " + ((HttpServletRequest) request).getRequestURL());
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
