package de.deepamehta.core.service;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import java.util.HashMap;
import java.util.Map;



/**
 * Cookies obtained from the request headers.
 */
public class Cookies {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, String> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Cookies(HttpHeaders httpHeaders) {
        try {
            for (Cookie cookie : httpHeaders.getCookies().values()) {
                values.put(cookie.getName(), cookie.getValue());
            }
        } catch (IllegalStateException e) {
            // happens if getCookies() is called outside the scope of a request
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Returns the value of the cookie for the given name, or throws an exception if no such cookie exists.
     */
    public String get(String name) {
        String value = values.get(name);
        //
        if (value == null) {
            throw new RuntimeException("Missing \"" + name + "\" cookie (cookies=" + this + ")");
        }
        //
        return value;
    }

    /**
     * Convenience method to access a long value of the cookie for the given name, or throws an exception
     * if no such cookie exists.
     */
    public long getLong(String name) {
        try {
            return Long.parseLong(get(name));
        } catch (Exception e) {
            throw new RuntimeException("Getting a long value for the \"" + name + "\" cookie failed", e);
        }
    }

    // ---

    /**
     * Checks if there is a cookie with the given name.
     */
    public boolean has(String name) {
        return values.get(name) != null;
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }
}
