package de.deepamehta.core.service;

import java.io.InputStream;
import java.io.IOException;



public interface Plugin {

    InputStream getResourceAsStream(String name) throws IOException;
}
