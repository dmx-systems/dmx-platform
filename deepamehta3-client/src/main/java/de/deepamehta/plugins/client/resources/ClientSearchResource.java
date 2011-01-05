package de.deepamehta.plugins.client.resources;

import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.osgi.Activator;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/search")
@Consumes("application/json")
@Produces("application/json")
public class ClientSearchResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CoreService dms = Activator.getService();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @GET
    public JSONObject searchTopics(@QueryParam("search") String searchTerm,
                                   @QueryParam("field")  String fieldUri,
                                   @QueryParam("wholeword") boolean wholeWord,
                                   @HeaderParam("Cookie") String cookie) throws JSONException {
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("searchTerm=" + searchTerm + ", fieldUri=" + fieldUri + ", wholeWord=" + wholeWord +
            ", cookie=" + clientContext);
        List<Topic> searchResult = dms.searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
        return createResultTopic(searchTerm, searchResult, clientContext).toJSON();
    }

    // Note: this resource method is actually part of the Type Search plugin.
    // TODO: proper modulariuation. Either let the Type Search plugin provide its own REST resource (with
    // another namespace again) or make the Type Search plugin an integral part of the Client plugin.
    @GET
    @Path("/by_type/{typeUri}")
    public JSONObject getTopics(@PathParam("typeUri") String typeUri) throws JSONException {
        logger.info("typeUri=" + typeUri);
        return createResultTopic(typeUri, dms.getTopics(typeUri), null).toJSON();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a search result topic (a bucket).
     */
    private Topic createResultTopic(String searchTerm, List<Topic> topics, Map clientContext) {
        Map properties = new HashMap();
        properties.put("de/deepamehta/core/property/SearchTerm", searchTerm);
        Topic resultTopic = dms.createTopic("de/deepamehta/core/topictype/SearchResult", properties, clientContext);
        // associate result topics
        logger.fine("Relating " + topics.size() + " result topics");
        for (Topic topic : topics) {
            logger.fine("Relating " + topic);
            dms.createRelation("SEARCH_RESULT", resultTopic.id, topic.id, new HashMap());
        }
        return resultTopic;
    }
}
