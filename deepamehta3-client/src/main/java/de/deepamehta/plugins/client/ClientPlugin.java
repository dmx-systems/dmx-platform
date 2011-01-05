package de.deepamehta.plugins.client;

import de.deepamehta.core.service.Plugin;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;



public class ClientPlugin extends Plugin {

    private static final String CLIENT_URL = "http://localhost:8080/de.deepamehta.3-client/index.html";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void allPluginsReadyHook() {
        try {
            logger.info("### Launching webclient (" + CLIENT_URL + ")");
            //
            Desktop.getDesktop().browse(new URI(CLIENT_URL));
            //
        } catch (Throwable e) {
            logger.warning("### Webclient can't be launched automatically (" + e + ")");
            logger.info("### Please launch webclient manually: " + CLIENT_URL);
        }
    }
}
