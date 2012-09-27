package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostInstallPluginListener;

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
import java.util.Map;
import java.util.logging.Logger;



@Path("/topicmap")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends PluginActivator implements TopicmapsService, PostInstallPluginListener {

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

    Map<String, TopicmapRenderer> topicmapRendererRegistry = new HashMap<String, TopicmapRenderer>();

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
        CompositeValue topicmapState = getTopicmapRenderer(topicmapRendererUri).initialTopicmapState();
        return dms.createTopic(new TopicModel(uri, "dm4.topicmaps.topicmap", new CompositeValue()
            .put("dm4.topicmaps.name", name)
            .put("dm4.topicmaps.topicmap_renderer_uri", topicmapRendererUri)
            .put("dm4.topicmaps.state", topicmapState)
        ), clientState);
    }

    // ---

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
            new TopicRoleModel(topicId,    ROLE_TYPE_TOPIC), new CompositeValue()
                .put("dm4.topicmaps.x", x)
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

    @PUT
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public void moveTopic(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId, @PathParam("x") int x,
                                                                                                @PathParam("y") int y) {
        fetchTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValue()
            .put("dm4.topicmaps.x", x)
            .put("dm4.topicmaps.y", y), null, new Directives());    // clientState=null
    }

    @PUT
    @Path("/{id}/topic/{topic_id}/{visibility}")
    @Override
    public void setTopicVisibility(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                     @PathParam("visibility") boolean visibility) {
        fetchTopicRefAssociation(topicmapId, topicId).setCompositeValue(new CompositeValue()
            .put("dm4.topicmaps.visibility", visibility), null, new Directives());  // clientState=null
    }

    @DELETE
    @Path("/{id}/association/{assoc_id}")
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        fetchAssociationRefAssociation(topicmapId, assocId).delete(new Directives());
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



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postInstallPlugin() {
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
