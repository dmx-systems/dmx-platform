package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Plugin;

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



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends Plugin implements TopicmapsService {

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

    @PUT
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public long addTopicToTopicmap(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                   @PathParam("x") int x, @PathParam("y") int y) {
        AssociationModel model = new AssociationModel("dm4.topicmaps.topic_mapcontext",
            new TopicRoleModel(topicmapId, "dm4.core.default"),
            new TopicRoleModel(topicId,    "dm4.topicmaps.topicmap_topic"),
            new CompositeValue().put("dm4.topicmaps.x", x)
                                .put("dm4.topicmaps.y", y)
                                .put("dm4.topicmaps.visibility", true)
        );
        Association refAssoc = dms.createAssociation(model, null);     // FIXME: clientState=null
        return refAssoc.getId();
    }

    @PUT
    @Path("/{id}/association/{assoc_id}")
    @Override
    public long addAssociationToTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        AssociationModel model = new AssociationModel("dm4.topicmaps.association_mapcontext",
            new TopicRoleModel(topicmapId,    "dm4.core.default"),
            new AssociationRoleModel(assocId, "dm4.topicmaps.topicmap_association"));
        Association refAssoc = dms.createAssociation(model, null);     // FIXME: clientState=null
        return refAssoc.getId();
    }

    @DELETE
    @Path("/{id}/association/{assoc_id}/{ref_id}")
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId,
                                              @PathParam("assoc_id") long assocId,
                                              @PathParam("ref_id") long refId) {
        removeAssociationFromTopicmap(refId);
    }

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

    /**
     * @param   refId   ID of the "Association Mapcontext" association that relates to the association to remove.
     */
    private void removeAssociationFromTopicmap(long refId) {
        dms.deleteAssociation(refId, null);     // clientState=null
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
