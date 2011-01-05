package de.deepamehta.server.resources;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.osgi.Activator;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



@Path("/topictype")
@Consumes("application/json")
@Produces("application/json")
public class TopicTypeResource {

    private Logger logger = Logger.getLogger(getClass().getName());

    @GET
    public JSONArray getTopicTypeUris() throws JSONException {
        JSONArray typeUris = new JSONArray();
        for (String typeUri : Activator.getService().getTopicTypeUris()) {
            typeUris.put(typeUri);
        }
        return typeUris;
    }

    @GET
    @Path("/{typeUri}")
    public JSONObject getTopicType(@PathParam("typeUri") String typeUri, @HeaderParam("Cookie") String cookie) {
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("Cookie: " + clientContext);
        return Activator.getService().getTopicType(typeUri, clientContext).toJSON();
    }

    @POST
    public JSONObject createTopicType(JSONObject topicType, @HeaderParam("Cookie") String cookie) {
        Map clientContext = JSONHelper.cookieToMap(cookie);
        logger.info("Cookie: " + clientContext);
        TopicType tt = new TopicType(topicType);
        return Activator.getService().createTopicType(tt.getProperties(), tt.getDataFields(), clientContext).toJSON();
    }

    // ---

    @POST
    @Path("/{typeUri}")
    public void addDataField(@PathParam("typeUri") String typeUri, JSONObject dataField) {
        Activator.getService().addDataField(typeUri, new DataField(dataField));
    }

    @PUT
    @Path("/{typeUri}")
    public void updateDataField(@PathParam("typeUri") String typeUri, JSONObject dataField) {
        Activator.getService().updateDataField(typeUri, new DataField(dataField));
    }

    @PUT
    @Path("/{typeUri}/field_order")
    public void setDataFieldOrder(@PathParam("typeUri") String typeUri, JSONArray fieldUris) {
        Activator.getService().setDataFieldOrder(typeUri, JSONHelper.toList(fieldUris));
    }

    @DELETE
    @Path("/{typeUri}/field/{fieldUri}")
    public void removeDataField(@PathParam("typeUri") String typeUri, @PathParam("fieldUri") String fieldUri) {
        Activator.getService().removeDataField(typeUri, fieldUri);
    }
}
