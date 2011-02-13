package de.deepamehta.plugins.client;

import de.deepamehta.core.service.Plugin;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;



public class ClientPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void allPluginsReadyHook() {
        String webclientUrl = null;
        try {
            String port = System.getProperty("org.osgi.service.http.port");
            webclientUrl = "http://localhost:" + port + "/de.deepamehta.3-client/index.html";
            logger.info("### Launching webclient (" + webclientUrl + ")");
            //
            Desktop.getDesktop().browse(new URI(webclientUrl));
            //
        } catch (Exception e) {
            logger.warning("### Webclient can't be launched automatically (" + e + ")");
            logger.info("### Please launch webclient manually: " + webclientUrl);
        }
    }
}
