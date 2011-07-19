package de.deepamehta.plugins.webclient;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Plugin;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
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

    // ------------------------------------------------------------------------------------------------------- Constants

    public static final String WEBCLIENT_URL = "/de.deepamehta.webclient/index.html";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private boolean webclientLaunched = false;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void allPluginsReadyHook() {
        if (webclientLaunched == false) {
            String webclientUrl = getWebclientUrl();
            try {
                logger.info("### Launching webclient (url=\"" + webclientUrl + "\")");
                Desktop.getDesktop().browse(new URI(webclientUrl));
                webclientLaunched = true;
            } catch (Exception e) {
                logger.warning("### Launching webclient failed (" + e + ")");
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
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            logger.info("searchTerm=\"" + searchTerm + "\", fieldUri=\"" + fieldUri + "\", wholeWord=" + wholeWord +
                ", clientContext=" + clientContext);
            Set<Topic> singleTopics = dms.searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
            Set<Topic> searchableUnits = findSearchableUnits(singleTopics);
            logger.info(singleTopics.size() + " single topics found, " + searchableUnits.size() + " searchable units");
            Topic searchTopic = createSearchTopic(searchTerm, searchableUnits, clientContext);
            tx.success();
            return searchTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Searching topics failed", e);
        } finally {
            tx.finish();
        }
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
        return null;    // ### createSearchTopic(typeUri, dms.getTopics(typeUri), null);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Set<Topic> findSearchableUnits(Set<Topic> topics) {
        Set<Topic> searchableUnits = new LinkedHashSet();
        for (Topic topic : topics) {
            if (isSearchableUnit(topic)) {
                searchableUnits.add(topic);
            } else {
                Set<Topic> parentTopics = toTopicSet(topic.getRelatedTopics((List) null,
                    "dm4.core.part", "dm4.core.whole", null, false, false));
                if (parentTopics.isEmpty()) {
                    searchableUnits.add(topic);
                } else {
                    searchableUnits.addAll(findSearchableUnits(parentTopics));
                }
            }
        }
        return searchableUnits;
    }

    /**
     * Creates a search result topic (a bucket).
     */
    private Topic createSearchTopic(String searchTerm, Set<Topic> topics, ClientContext clientContext) {
        // CompositeValue comp = new CompositeValue("{dm4.webclient.search_term: \"" + searchTerm + "\"}");
        Topic searchTopic = dms.createTopic(new TopicModel("dm4.webclient.search" /*, comp */), clientContext);
        searchTopic.setChildTopicValue("dm4.webclient.search_term", new SimpleValue(searchTerm));
        //
        // associate result topics
        logger.fine("Associating " + topics.size() + " result topics to search topic (ID " + searchTopic.getId() + ")");
        for (Topic topic : topics) {
            logger.fine("Associating " + topic);
            AssociationModel assocModel = new AssociationModel("dm4.webclient.search_result_item");
            assocModel.setRoleModel1(new TopicRoleModel(searchTopic.getId(), "dm4.webclient.search"));
            assocModel.setRoleModel2(new TopicRoleModel(topic.getId(), "dm4.webclient.search_result_item"));
            dms.createAssociation(assocModel, clientContext);
        }
        return searchTopic;
    }

    // ---

    private boolean isSearchableUnit(Topic topic) {
        TopicType topicType = dms.getTopicType(topic.getTypeUri(), null);           // FIXME: clientContext=null
        Boolean isSearchableUnit = (Boolean) getViewConfig(topicType, "is_searchable_unit");
        return isSearchableUnit != null ? isSearchableUnit.booleanValue() : false;  // default is false
    }

    /**
     * Read out a view configuration setting.
     * <p>
     * Compare to client-side counterpart: function get_view_config() in webclient.js
     *
     * @param   topicType   The topic type whose view configuration is read out.
     * @param   setting     Last component of the setting URI, e.g. "icon_src".
     *
     * @return  The setting value, or <code>null</code> if there is no such setting
     */
    private Object getViewConfig(TopicType topicType, String setting) {
        return topicType.getViewConfig("dm4.webclient.view_config", "dm4.webclient." + setting);
    }

    // ---

    private Set<Topic> toTopicSet(Set<RelatedTopic> relTopics) {
        Set<Topic> topics = new LinkedHashSet();
        for (Topic topic : relTopics) {
            topics.add(topic);
        }
        return topics;
    }

    // ---

    private String getWebclientUrl() {
        String host = "localhost";
        String port = System.getProperty("org.osgi.service.http.port");
        return  "http://" + host + ":" + port + WEBCLIENT_URL;
    }
}
