package de.deepamehta.core.service.webpublishing;

import java.net.MalformedURLException;
import java.net.URL;



// ### TODO: drop this? Not used anymore.
public interface DirectoryResourceMapper {

    /**
     * Maps a resource name to an URL.
     */
    URL getResource(String name) throws MalformedURLException;
}
