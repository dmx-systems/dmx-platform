package de.deepamehta.core.service;

import java.io.InputStream;
import java.io.IOException;



public interface Plugin {

    InputStream getResourceAsStream(String name) throws IOException;

    void publishDirectory(String directoryPath, String uriNamespace, SecurityHandler securityHandler);

    void start();

    void stop();

}
