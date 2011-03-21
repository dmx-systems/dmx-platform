package de.deepamehta.plugins.webclient;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Plugin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class WebclientPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



    @Override
    public void allPluginsReadyHook() {
        String webclientUrl = null;
        try {
            String port = System.getProperty("org.osgi.service.http.port");
            webclientUrl = "http://localhost:" + port + "/de.deepamehta.3-webclient/index.html";
            logger.info("### Launching webclient (" + webclientUrl + ")");
            //
            Desktop.getDesktop().browse(new URI(webclientUrl));
            //
        } catch (Exception e) {
            logger.warning("### Webclient can't be launched automatically (" + e + ")");
            logger.info("### Please launch webclient manually: " + webclientUrl);
        }
    }

    @Override
    public Map<String, Object> enrichTopicTypeHook(TopicType topicType, ClientContext clientContext) {
        Map m = new HashMap();
        //
        if (topicType.getUri().equals("dm3.webclient.search")) {
            m.put("icon_src", "/de.deepamehta.3-webclient/images/bucket.png");
        }
        //
        return m;
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
        // ### List<Topic> searchResult = dms.searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
        return null;    // ### createResultTopic(searchTerm, searchResult, clientContext);
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
        return null;    // ### createResultTopic(typeUri, dms.getTopics(typeUri), null);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a search result topic (a bucket).
     */
    /* ### private Topic createResultTopic(String searchTerm, List<Topic> topics, ClientContext clientContext) {
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
    } */
}
