package systems.dmx.core.service;

import java.io.InputStream;



public interface Plugin {

    /**
     * Accesses a static resource of the plugin. A static resource is some data (images, audio, text, etc) that is
     * contained in the plugin bundle (that is the jar file).
     *
     * @param   name    The resource name: a "/"-separated path name, relative to the plugin bundle's root directory.
     *                  It may or may not begin with "/" (it makes no difference).
     *
     * @return  An InputStream to read the static resource content.
     *
     * @throws  RuntimeException    If no such static resource is contained in the plugin bundle.
     */
    InputStream getStaticResource(String name);

    /**
     * Checks if this plugin bundle contains a static resource with the given name.
     *
     * @param   name    The resource name: a "/"-separated path name, relative to the plugin bundle's root directory.
     *                  It may or may not begin with "/" (it makes no difference).
     */
    boolean hasStaticResource(String name);
}
