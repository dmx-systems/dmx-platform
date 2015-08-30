package de.deepamehta.core.service;

import java.net.MalformedURLException;
import java.net.URL;



public interface DirectoryResourceMapper {

    /**
     * Maps a resource name to an URL.
     */
    URL getResource(String name) throws MalformedURLException;
}
