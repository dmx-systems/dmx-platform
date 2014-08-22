package de.deepamehta.core.service;

import java.io.InputStream;



public interface Plugin {

    /**
     * Accesses a static resource of the plugin. A static resource is some data (images, audio, text, etc) that is
     * contained in the plugin OSGi bundle (that is the jar file).
     *
     * @param   name    The resource name: a '/'-separated path name that identifies the resource.
     *                  A '/' at the beginning is optional (it makes no difference).
     *
     * @return  An InputStream to read the resource content.
     *
     * @throws  RuntimeException    if no such resource can be found.
     */
    InputStream getStaticResource(String name);

    /**
     * Checks if this plugin provides a static resource with the given name.
     */
    boolean hasStaticResource(String name);
}
