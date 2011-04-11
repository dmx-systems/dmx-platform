package de.deepamehta.plugins.client;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.Plugin;

import java.awt.Desktop;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class ClientPlugin extends Plugin {

    // ------------------------------------------------------------------------------------------------------- Constants

    public static final String CLIENT_INDEX_HTML = "/de.deepamehta.3-client/index.html";

    // ---------------------------------------------------------------------------------------------- Instance Variables


    private Logger logger = Logger.getLogger(getClass().getName());

    private boolean webclientUrlOpened = false;

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getWebclientUrl() {

        String port = System.getProperty("org.osgi.service.http.port");
        if(port == null ) {
            port = "8080"; // default
        }

        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return  "http://" + host + ":" + port + CLIENT_INDEX_HTML;
    }

    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



    @Override
    public void allPluginsReadyHook() {
        if(webclientUrlOpened == false) {
            String webclientUrl = getWebclientUrl();

            try {
                logger.info("### Launching webclient (" + webclientUrl + ")");
                Desktop.getDesktop().browse(new URI(webclientUrl));
                webclientUrlOpened = true;
            } catch (Exception e) {
                logger.warning("### Webclient can't be launched automatically (" + e + ")");
                logger.info("### Please launch webclient manually: " + webclientUrl);
            }
        }
    }



    // **********************
    // *** Plugin Service ***
    // **********************

    // Note: the client service is provided as REST service only (OSGi service not required for the moment).



    /**
     * Performs a fulltext search and creates a search result topic (a bucket).
     */
    @GET
    @Path("/search")
    public Topic searchTopics(@QueryParam("search") String searchTerm,
                              @QueryParam("field")  String fieldUri,
                              @QueryParam("wholeword") boolean wholeWord,
                              @HeaderParam("Cookie") ClientContext clientContext) {
        logger.info("searchTerm=" + searchTerm + ", fieldUri=" + fieldUri + ", wholeWord=" + wholeWord +
            ", cookie=" + clientContext);
        List<Topic> searchResult = dms.searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
        return createResultTopic(searchTerm, searchResult, clientContext);
    }

    /**
     * Performs a by-type search and creates a search result topic (a bucket).
     * <p>
     * Note: this resource method is actually part of the Type Search plugin.
     * TODO: proper modularization. Either let the Type Search plugin provide its own REST resource (with
     * another namespace again) or make the Type Search plugin an integral part of the Client plugin.
     */
    @GET
    @Path("/search/by_type/{typeUri}")
    public Topic getTopics(@PathParam("typeUri") String typeUri) {
        logger.info("typeUri=" + typeUri);
        return createResultTopic(typeUri, dms.getTopics(typeUri), null);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a search result topic (a bucket).
     */
    private Topic createResultTopic(String searchTerm, List<Topic> topics, ClientContext clientContext) {
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/SearchTerm", searchTerm);
        Topic resultTopic = dms.createTopic("de/deepamehta/core/topictype/SearchResult", properties, clientContext);
        // associate result topics
        logger.fine("Relating " + topics.size() + " result topics");
        for (Topic topic : topics) {
            logger.fine("Relating " + topic);
            dms.createRelation("SEARCH_RESULT", resultTopic.id, topic.id, null);
        }
        return resultTopic;
    }
}
