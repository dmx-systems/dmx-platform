package systems.dmx.core.service;

import com.sun.jersey.spi.container.ContainerRequest;

import javax.ws.rs.core.Cookie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;



/**
 * Cookies contained in the thread-local ContainerRequest.
 * To obtain a Cookies object call Cookies.get().
 */
public class Cookies {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, String> values = new HashMap();

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static final ThreadLocal<ContainerRequest> threadLocalRequest = new ThreadLocal();

    // ---------------------------------------------------------------------------------------------------- Constructors

    private Cookies() {
    }

    private Cookies(Collection<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            values.put(cookie.getName(), cookie.getValue());
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
            throw new RuntimeException("Missing \"" + name + "\" cookie (cookies=" + values + ")");
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

    /**
     * Returns a Cookies object representing the cookies contained in the thread-local ContainerRequest.
     * If called outside request scope an empty Cookies object is returned.
     */
    public static Cookies get() {
        ContainerRequest request = threadLocalRequest.get();
        if (request != null) {
            return new Cookies(request.getCookies().values());
        } else {
            return new Cookies();
        }
    }

    // ### TODO: define public Cookies interface and hide this internal method
    public static void set(ContainerRequest request) {
        threadLocalRequest.set(request);
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }
}
