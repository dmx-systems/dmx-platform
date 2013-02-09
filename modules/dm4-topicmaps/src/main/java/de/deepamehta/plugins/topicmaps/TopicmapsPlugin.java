package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.ClusterCoords;
import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.CompositeValueModel;
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

import java.awt.Point;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/topicmap")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends PluginActivator implements TopicmapsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DEFAULT_TOPICMAP_NAME     = "untitled";
    private static final String DEFAULT_TOPICMAP_URI      = "dm4.topicmaps.default_topicmap";
    private static final String DEFAULT_TOPICMAP_RENDERER = "dm4.webclient.default_topicmap_renderer";

    // association type semantics ### TODO: to be dropped. Model-driven manipulators required.
    private static final String TOPIC_MAPCONTEXT       = "dm4.topicmaps.topic_mapcontext";
    private static final String ASSOCIATION_MAPCONTEXT = "dm4.topicmaps.association_mapcontext";
    private static final String ROLE_TYPE_TOPICMAP     = "dm4.core.default";
    private static final String ROLE_TYPE_TOPIC        = "dm4.topicmaps.topicmap_topic";
    private static final String ROLE_TYPE_ASSOCIATION  = "dm4.topicmaps.topicmap_association";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    Map<String, TopicmapRenderer> topicmapRendererRegistry = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    public TopicmapsPlugin() {
        // Note: registering the default renderer in the InitializePluginListener would be too late.
        // The renderer is already needed in the PostInstallPluginListener.
        registerTopicmapRenderer(new DefaultTopicmapRenderer());
    }



    // ***************************************
    // *** TopicmapsService Implementation ***
    // ***************************************



    @POST
    @Path("/{name}/{topicmap_renderer_uri}")
    @Override
    public Topic createTopicmap(@PathParam("name") String name,
                                @PathParam("topicmap_renderer_uri") String topicmapRendererUri,
                                @HeaderParam("Cookie") ClientState clientState) {
        return createTopicmap(name, null, topicmapRendererUri, clientState);
    }

    @Override
    public Topic createTopicmap(String name, String uri, String topicmapRendererUri, ClientState clientState) {
        CompositeValueModel topicmapState = getTopicmapRenderer(topicmapRendererUri).initialTopicmapState();
        return dms.createTopic(new TopicModel(uri, "dm4.topicmaps.topicmap", new CompositeValueModel().put(
            "dm4.topicmaps.name", name).put(
            "dm4.topicmaps.topicmap_renderer_uri", topicmapRendererUri).put(
            "dm4.topicmaps.state", topicmapState)
        ), clientState);
    }

    // ---

    @GET
    @Path("/{id}")
    @Override
    public Topicmap getTopicmap(@PathParam("id") long topicmapId, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return new Topicmap(topicmapId, dms, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Fetching topicmap " + topicmapId + " failed", e));
        }
    }

    // ---

    @POST
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public void addTopicToTopicmap(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                   @PathParam("x") int x, @PathParam("y") int y) {
        dms.createAssociation(new AssociationModel(TOPIC_MAPCONTEXT,
            new TopicRoleModel(topicmapId, ROLE_TYPE_TOPICMAP),
            new TopicRoleModel(topicId,    ROLE_TYPE_TOPIC), new CompositeValueModel().put(
                "dm4.topicmaps.x", x).put(
                "dm4.topicmaps.y", y).put(
                "dm4.topicmaps.visibility", true)
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

    @PUT
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public void moveTopic(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId, @PathParam("x") int x,
                                                                                                @PathParam("y") int y) {
        fetchTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValueModel().put(
            "dm4.topicmaps.x", x).put(
            "dm4.topicmaps.y", y
        ), null, new Directives());    // clientState=null
    }

    @PUT
    @Path("/{id}/topic/{topic_id}/{visibility}")
    @Override
    public void setTopicVisibility(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                     @PathParam("visibility") boolean visibility) {
        fetchTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValueModel().put(
            "dm4.topicmaps.visibility", visibility
        ), null, new Directives());  // clientState=null
    }

    @DELETE
    @Path("/{id}/association/{assoc_id}")
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        fetchAssociationRefAssociation(topicmapId, assocId).delete(new Directives());
    }

    @PUT
    @Path("/{id}")
    @Override
    public void moveCluster(@PathParam("id") long topicmapId, ClusterCoords coords) {
        for (ClusterCoords.Entry entry : coords) {
            moveTopic(topicmapId, entry.topicId, entry.x, entry.y);
        }
    }

    @PUT
    @Path("/{id}/translation/{x}/{y}")
    @Override
    public void setTopicmapTranslation(@PathParam("id") long topicmapId, @PathParam("x") int transX,
                                                                         @PathParam("y") int transY) {
        try {
            CompositeValueModel topicmapState = new CompositeValueModel().put(
                "dm4.topicmaps.state", new CompositeValueModel().put(
                    "dm4.topicmaps.translation", new CompositeValueModel().put(
                        "dm4.topicmaps.translation_x", transX).put(
                        "dm4.topicmaps.translation_y", transY)
            ));
            dms.updateTopic(new TopicModel(topicmapId, topicmapState), null);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Setting translation of topicmap " + topicmapId +
                " failed (transX=" + transX + ", transY=" + transY + ")", e));
        }
    }

    // ---

    @Override
    public void registerTopicmapRenderer(TopicmapRenderer renderer) {
        logger.info("### Registering topicmap renderer \"" + renderer.getClass().getName() + "\"");
        topicmapRendererRegistry.put(renderer.getUri(), renderer);
    }

    // ---

    // Note: not part of topicmaps service
    @GET
    @Path("/{id}")
    @Produces("text/html")
    public InputStream getTopicmapInWebclient() {
        // Note: the path parameter is evaluated at client-side
        return invokeWebclient();
    }

    // Note: not part of topicmaps service
    @GET
    @Path("/{id}/topic/{topic_id}")
    @Produces("text/html")
    public InputStream getTopicmapAndTopicInWebclient() {
        // Note: the path parameters are evaluated at client-side
        return invokeWebclient();
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void postInstall() {
        createTopicmap(DEFAULT_TOPICMAP_NAME, DEFAULT_TOPICMAP_URI, DEFAULT_TOPICMAP_RENDERER, null);
        // Note: null is passed as clientState. On post-install we have no clientState.
        // The workspace assignment is made by the Access Control plugin on all-plugins-active.
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Association fetchTopicRefAssociation(long topicmapId, long topicId) {
        return dms.getAssociation(TOPIC_MAPCONTEXT, topicmapId, topicId,
            ROLE_TYPE_TOPICMAP, ROLE_TYPE_TOPIC, false, null);          // fetchComposite=false, clientState=null
    }

    private Association fetchAssociationRefAssociation(long topicmapId, long assocId) {
        return dms.getAssociationBetweenTopicAndAssociation(ASSOCIATION_MAPCONTEXT, topicmapId, assocId,
            ROLE_TYPE_TOPICMAP, ROLE_TYPE_ASSOCIATION, false, null);    // fetchComposite=false, clientState=null
    }

    // ---

    private TopicmapRenderer getTopicmapRenderer(String rendererUri) {
        TopicmapRenderer renderer = topicmapRendererRegistry.get(rendererUri);
        //
        if (renderer == null) {
            throw new RuntimeException("\"" + rendererUri + "\" is an unknown topicmap renderer");
        }
        //
        return renderer;
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
