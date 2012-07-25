package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/topicmap")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends PluginActivator implements TopicmapsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // association type semantics ### TODO: to be dropped. Model-driven manipulators required.
    private static final String TOPIC_MAPCONTEXT       = "dm4.topicmaps.topic_mapcontext";
    private static final String ASSOCIATION_MAPCONTEXT = "dm4.topicmaps.association_mapcontext";
    private static final String ROLE_TYPE_TOPICMAP     = "dm4.core.default";
    private static final String ROLE_TYPE_TOPIC        = "dm4.topicmaps.topicmap_topic";
    private static final String ROLE_TYPE_ASSOCIATION  = "dm4.topicmaps.topicmap_association";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************************
    // *** TopicmapsService Implementation ***
    // ***************************************



    @GET
    @Path("/{id}")
    @Override
    public Topicmap getTopicmap(@PathParam("id") long topicmapId, @HeaderParam("Cookie") ClientState clientState) {
        return new Topicmap(topicmapId, dms, clientState);
    }

    // ---

    @POST
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public void addTopicToTopicmap(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                   @PathParam("x") int x, @PathParam("y") int y) {
        dms.createAssociation(new AssociationModel(TOPIC_MAPCONTEXT,
            new TopicRoleModel(topicmapId, ROLE_TYPE_TOPICMAP),
            new TopicRoleModel(topicId,    ROLE_TYPE_TOPIC),
            new CompositeValue().put("dm4.topicmaps.x", x)
                                .put("dm4.topicmaps.y", y)
                                .put("dm4.topicmaps.visibility", true)
        ), null);   // FIXME: clientState=null
    }

    @POST
    @Path("/{id}/association/{assoc_id}")
    @Override
    public void addAssociationToTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        dms.createAssociation(new AssociationModel(ASSOCIATION_MAPCONTEXT,
            new TopicRoleModel(topicmapId,    ROLE_TYPE_TOPICMAP),
            new AssociationRoleModel(assocId, ROLE_TYPE_ASSOCIATION)
        ), null);   // FIXME: clientState=null
    }

    // ---

    @PUT
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public void moveTopic(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId, @PathParam("x") int x,
                                                                                                @PathParam("y") int y) {
        getTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValue()
            .put("dm4.topicmaps.x", x)
            .put("dm4.topicmaps.y", y), null, new Directives());    // clientState=null
    }

    @PUT
    @Path("/{id}/topic/{topic_id}/{visibility}")
    @Override
    public void setTopicVisibility(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                     @PathParam("visibility") boolean visibility) {
        getTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValue()
            .put("dm4.topicmaps.visibility", visibility), null, new Directives());  // clientState=null
    }

    // ---

    @DELETE
    @Path("/{id}/association/{assoc_id}")
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        getAssociationRefAssociation(topicmapId, assocId).delete(null);     // directives=null
    }

    // ---

    @PUT
    @Path("/{id}/translation/{x}/{y}")
    @Override
    public void setTopicmapTranslation(@PathParam("id") long topicmapId, @PathParam("x") int trans_x,
                                                                         @PathParam("y") int trans_y) {
        CompositeValue topicmapState = new CompositeValue().put("dm4.topicmaps.state", new CompositeValue()
            .put("dm4.topicmaps.translation", new CompositeValue()
                .put("dm4.topicmaps.translation_x", trans_x)
                .put("dm4.topicmaps.translation_y", trans_y)
            )
        );
        dms.updateTopic(new TopicModel(topicmapId, topicmapState), null);
    }

    // ---

    // Note: not part of topicmaps service
    @GET
    @Path("/{id}")
    @Produces("text/html")
    public InputStream getTopicmapInWebclient() {
        // Note: the template parameter is evaluated at client-side
        return invokeWebclient();
    }

    // Note: not part of topicmaps service
    @GET
    @Path("/{id}/topic/{topic_id}")
    @Produces("text/html")
    public InputStream getTopicmapAndTopicInWebclient() {
        // Note: the template parameters are evaluated at client-side
        return invokeWebclient();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Association getTopicRefAssociation(long topicmapId, long topicId) {
        return dms.getAssociation(TOPIC_MAPCONTEXT, topicmapId, topicId,
            ROLE_TYPE_TOPICMAP, ROLE_TYPE_TOPIC, false, null);          // fetchComposite=false, clientState=null
    }

    private Association getAssociationRefAssociation(long topicmapId, long assocId) {
        // ### FIXME: doesn't work! getAssociation() expects 2 topicIDs!
        return dms.getAssociation(ASSOCIATION_MAPCONTEXT, topicmapId, assocId,
            ROLE_TYPE_TOPICMAP, ROLE_TYPE_ASSOCIATION, false, null);    // fetchComposite=false, clientState=null
    }

    // ---

    private InputStream invokeWebclient() {
        try {
            return dms.getPlugin("de.deepamehta.webclient").getResourceAsStream("web/index.html");
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
}
