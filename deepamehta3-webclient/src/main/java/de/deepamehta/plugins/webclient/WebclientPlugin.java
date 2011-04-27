package de.deepamehta.plugins.webclient;

import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicValue;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        logger.info("searchTerm=\"" + searchTerm + "\", fieldUri=\"" + fieldUri + "\", wholeWord=" + wholeWord +
            ", clientContext=" + clientContext);
        Set<Topic> topics = dms.searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
        Set<Topic> wholeTopics = filterWholeTopics(topics);
        logger.info(topics.size() + " topics found, " + wholeTopics.size() + " after whole topic filtering");
        return createResultTopic(searchTerm, wholeTopics, clientContext);
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

    private Set<Topic> filterWholeTopics(Set<Topic> topics) {
        Set<Topic> wholeTopics = new LinkedHashSet();
        for (Topic topic : topics) {
            Set<Topic> parentTopics = topic.getRelatedTopics(null, "dm3.core.part", "dm3.core.whole", null, false);
            if (parentTopics.isEmpty()) {
                wholeTopics.add(topic);
            } else {
                wholeTopics.addAll(filterWholeTopics(parentTopics));
            }
        }
        return wholeTopics;
    }

    // ---

    /**
     * Creates a search result topic (a bucket).
     */
    private Topic createResultTopic(String searchTerm, Set<Topic> topics, ClientContext clientContext) {
        Topic searchTopic = dms.createTopic(new TopicData("dm3.webclient.search"), clientContext);
        searchTopic.setChildTopicValue("dm3.webclient.search_term", new TopicValue(searchTerm));
        // associate search result topics
        logger.info("Associating " + topics.size() + " search result topics");
        for (Topic topic : topics) {
            logger.info("Associating " + topic);
            AssociationData assocData = new AssociationData("dm3.webclient.search_result_item");
            assocData.addTopicRole(new TopicRole(searchTopic.getId(), "dm3.webclient.search"));
            assocData.addTopicRole(new TopicRole(topic.getId(), "dm3.webclient.search_result_item"));
            dms.createAssociation(assocData, clientContext);
        }
        return searchTopic;
    }
}
