package de.deepamehta.server.resources;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.Relation;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/relation")
@Consumes("application/json")
@Produces("application/json")
public class RelationResource {

    private Logger logger = Logger.getLogger(getClass().getName());

    @GET
    public JSONObject getRelation(@QueryParam("src") long srcTopicId, @QueryParam("dst") long dstTopicId,
                                  @QueryParam("type") String typeId, @QueryParam("directed") boolean isDirected) {
        logger.info("srcTopicId=" + srcTopicId + " dstTopicId=" + dstTopicId +
            " typeId=" + typeId + " isDirected=" + isDirected);
        //
        Relation rel = Activator.getService().getRelation(srcTopicId, dstTopicId, typeId, isDirected);
        //
        if (rel != null) {
            return rel.toJSON();
        }
        return null;
    }

    @GET
    @Path("/multiple")
    public JSONArray getRelations(@QueryParam("src") long srcTopicId, @QueryParam("dst") long dstTopicId,
                                  @QueryParam("type") String typeId, @QueryParam("directed") boolean isDirected) {
        logger.info("srcTopicId=" + srcTopicId + " dstTopicId=" + dstTopicId +
            " typeId=" + typeId + " isDirected=" + isDirected);
        //
        List<Relation> relations = Activator.getService().getRelations(srcTopicId, dstTopicId, typeId, isDirected);
        return JSONHelper.relationsToJson(relations);
    }

    @POST
    public JSONObject createRelation(JSONObject relation) throws JSONException {
        //
        String typeId = relation.getString("type_id");                              // throws JSONException
        long srcTopicId = relation.getLong("src_topic_id");                         // throws JSONException
        long dstTopicId = relation.getLong("dst_topic_id");                         // throws JSONException
        Map properties = JSONHelper.toMap(relation.getJSONObject("properties"));    // throws JSONException
        //
        return Activator.getService().createRelation(typeId, srcTopicId, dstTopicId, properties).toJSON();
    }

    @PUT
    @Path("/{id}")
    public void setTopicProperties(@PathParam("id") long id, JSONObject properties) {
        Activator.getService().setRelationProperties(id, JSONHelper.toMap(properties));
    }

    @DELETE
    @Path("/{id}")
    public void deleteRelation(@PathParam("id") long id) {
        Activator.getService().deleteRelation(id);
    }
}
