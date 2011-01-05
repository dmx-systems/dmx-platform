package de.deepamehta.server.resources;

import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.osgi.Activator;
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
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/topic")
@Consumes("application/json")
@Produces("application/json")
public class TopicResource {

    private Logger logger = Logger.getLogger(getClass().getName());

    @GET
    @Path("/{id}")
    public JSONObject getTopic(@PathParam("id") long id, @HeaderParam("Cookie") String cookie) {
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("Cookie: " + clientContext);
        return Activator.getService().getTopic(id, clientContext).toJSON();
    }

    @GET
    @Path("/{id}/related_topics")
    public JSONArray getRelatedTopics(@PathParam("id") long id,
                                      @QueryParam("include_topic_types") List includeTopicTypes,
                                      @QueryParam("include_rel_types")   List includeRelTypes,
                                      @QueryParam("exclude_rel_types")   List excludeRelTypes) throws JSONException {
        logger.info("Topic " + id + ", include topic types="    + includeTopicTypes + 
                                    ", include relation types=" + includeRelTypes   + 
                                    ", exclude relation types=" + excludeRelTypes);
        return JSONHelper.relatedTopicsToJson(Activator.getService().getRelatedTopics(id, includeTopicTypes,
                                                                                          includeRelTypes,
                                                                                          excludeRelTypes));
    }

    @GET
    public JSONArray searchTopics(@QueryParam("search") String searchTerm,
                                  @QueryParam("field")  String fieldUri,
                                  @QueryParam("wholeword") boolean wholeWord,
                                  @HeaderParam("Cookie") String cookie) {
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("searchTerm=" + searchTerm + ", fieldUri=" + fieldUri + ", wholeWord=" + wholeWord +
            ", cookie=" + clientContext);
        List searchResult = Activator.getService().searchTopics(searchTerm, fieldUri, wholeWord, clientContext);
        return JSONHelper.topicsToJson(searchResult);
    }

    @GET
    @Path("/by_type/{typeUri}")
    public JSONArray getTopicsByType(@PathParam("typeUri") String typeUri) {
        logger.info("typeUri=" + typeUri);
        return JSONHelper.topicsToJson(Activator.getService().getTopics(typeUri));
    }

    @GET
    @Path("/by_property/{key}/{value}")
    public Response getTopicByProperty(@PathParam("key") String key, @PathParam("value") String value) {
        logger.info("key=" + key + ", value=" + value);
        Topic topic = Activator.getService().getTopic(key, value);
        if (topic != null) {
            return Response.ok(topic.toJSON()).build();
        } else {
            return Response.noContent().build();
        }
    }

    @GET
    @Path("/{typeUri}/{key}/{value}")
    public Response getTopic(@PathParam("typeUri") String typeUri,
                             @PathParam("key") String key, @PathParam("value") String value) {
        logger.info("typeUri=" + typeUri + ", key=" + key + ", value=" + value);
        Topic topic = Activator.getService().getTopic(typeUri, key, value);
        if (topic != null) {
            return Response.ok(topic.toJSON()).build();
        } else {
            return Response.noContent().build();
        }
    }

    @POST
    public JSONObject createTopic(JSONObject topic, @HeaderParam("Cookie") String cookie) throws JSONException {
        String typeUri = topic.getString("type_uri");                           // throws JSONException
        Map properties = JSONHelper.toMap(topic.getJSONObject("properties"));   // throws JSONException
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("Cookie: " + clientContext);
        //
        return Activator.getService().createTopic(typeUri, properties, clientContext).toJSON();
    }

    @PUT
    @Path("/{id}")
    public void setTopicProperties(@PathParam("id") long id, JSONObject properties) {
        Activator.getService().setTopicProperties(id, JSONHelper.toMap(properties));
    }

    @DELETE
    @Path("/{id}")
    public void deleteTopic(@PathParam("id") long id) {
        Activator.getService().deleteTopic(id);
    }
}
