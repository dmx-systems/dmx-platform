package systems.dmx.topicmaps;

import systems.dmx.topicmaps.model.TopicmapViewmodel;

import systems.dmx.core.Association;
import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationRoleModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.model.topicmaps.AssociationViewModel;
import systems.dmx.core.model.topicmaps.TopicViewModel;
import systems.dmx.core.model.topicmaps.ViewProperties;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.util.DMXUtils;
import systems.dmx.core.util.IdList;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;



@Path("/topicmap")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends PluginActivator implements TopicmapsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // association type semantics ### TODO: to be dropped. Model-driven manipulators required.
    private static final String TOPIC_MAPCONTEXT       = "dmx.topicmaps.topic_mapcontext";
    private static final String ASSOCIATION_MAPCONTEXT = "dmx.topicmaps.association_mapcontext";
    private static final String ROLE_TYPE_TOPICMAP     = "dmx.core.default";
    private static final String ROLE_TYPE_TOPIC        = "dmx.topicmaps.topicmap_topic";
    private static final String ROLE_TYPE_ASSOCIATION  = "dmx.topicmaps.topicmap_association";

    private static final String PROP_X          = "dmx.topicmaps.x";
    private static final String PROP_Y          = "dmx.topicmaps.y";
    private static final String PROP_VISIBILITY = "dmx.topicmaps.visibility";
    private static final String PROP_PINNED     = "dmx.topicmaps.pinned";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, TopicmapRenderer> topicmapRenderers = new HashMap();
    private List<ViewmodelCustomizer> viewmodelCustomizers = new ArrayList();
    private Messenger me = new Messenger("systems.dmx.webclient");

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    public TopicmapsPlugin() {
        // Note: registering the default renderer in the init() hook would be too late.
        // The renderer is already needed at install-in-DB time ### Still true? Use preInstall() hook?
        registerTopicmapRenderer(new DefaultTopicmapRenderer());
    }



    // ***************************************
    // *** TopicmapsService Implementation ***
    // ***************************************



    @POST
    @Transactional
    @Override
    public Topic createTopicmap(@QueryParam("name") String name,
                                @QueryParam("renderer_uri") String topicmapRendererUri,
                                @QueryParam("private") boolean isPrivate) {
        logger.info("Creating topicmap \"" + name + "\" (topicmapRendererUri=\"" + topicmapRendererUri +
            "\", isPrivate=" + isPrivate +")");
        Topic topicmapTopic = dmx.createTopic(mf.newTopicModel("dmx.topicmaps.topicmap", mf.newChildTopicsModel()
            .put("dmx.topicmaps.name", name)
            .put("dmx.topicmaps.topicmap_renderer_uri", topicmapRendererUri)
            .put("dmx.topicmaps.private", isPrivate)
            .put("dmx.topicmaps.state", getTopicmapRenderer(topicmapRendererUri).initialTopicmapState(mf))));
        me.newTopicmap(topicmapTopic);      // FIXME: broadcast to eligible users only
        return topicmapTopic;
    }

    // ---

    @GET
    @Path("/{id}")
    @Override
    public TopicmapViewmodel getTopicmap(@PathParam("id") long topicmapId,
                                         @QueryParam("include_childs") boolean includeChilds) {
        try {
            logger.info("Loading topicmap " + topicmapId + " (includeChilds=" + includeChilds + ")");
            // Note: a TopicmapViewmodel is not a DMXObject. So the JerseyResponseFilter's automatic
            // child topic loading is not applied. We must load the child topics manually here.
            Topic topicmapTopic = dmx.getTopic(topicmapId).loadChildTopics();
            Map<Long, TopicViewModel> topics = fetchTopics(topicmapTopic, includeChilds);
            Map<Long, AssociationViewModel> assocs = fetchAssociations(topicmapTopic);
            //
            return new TopicmapViewmodel(topicmapTopic.getModel(), topics, assocs);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topicmap " + topicmapId + " failed", e);
        }
    }

    @Override
    public boolean isTopicInTopicmap(long topicmapId, long topicId) {
        return fetchTopicMapcontext(topicmapId, topicId) != null;
    }

    @Override
    public boolean isAssociationInTopicmap(long topicmapId, long assocId) {
        return fetchAssociationMapcontext(topicmapId, assocId) != null;
    }

    // ---

    @POST
    @Path("/{id}/topic/{topic_id}")
    @Transactional
    @Override
    public void addTopicToTopicmap(@PathParam("id") final long topicmapId,
                                   @PathParam("topic_id") final long topicId, final ViewProperties viewProps) {
        try {
            // Note: a Mapcontext association must have no workspace assignment as it is not user-deletable
            dmx.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Void>() {  // throws Exception
                @Override
                public Void call() {
                    if (isTopicInTopicmap(topicmapId, topicId)) {
                        throw new RuntimeException("Topic " + topicId + " already added to topicmap" + topicmapId);
                    }
                    createTopicMapcontext(topicmapId, topicId, viewProps);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Adding topic " + topicId + " to topicmap " + topicmapId + " failed " +
                "(viewProps=" + viewProps + ")", e);
        }
    }

    @Override
    public void addTopicToTopicmap(long topicmapId, long topicId, int x, int y, boolean visibility) {
        addTopicToTopicmap(topicmapId, topicId, new ViewProperties(x, y, visibility, false));   // pinned=false
    }

    @POST
    @Path("/{id}/association/{assoc_id}")
    @Transactional
    @Override
    public void addAssociationToTopicmap(@PathParam("id") final long topicmapId,
                                         @PathParam("assoc_id") final long assocId, final ViewProperties viewProps) {
        try {
            // Note: a Mapcontext association must have no workspace assignment as it is not user-deletable
            dmx.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Void>() {  // throws Exception
                @Override
                public Void call() {
                    if (isAssociationInTopicmap(topicmapId, assocId)) {
                        throw new RuntimeException("Association " + assocId + " already added to topicmap " +
                            topicmapId);
                    }
                    createAssociationMapcontext(topicmapId, assocId, viewProps);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Adding association " + assocId + " to topicmap " + topicmapId + " failed " +
                "(viewProps=" + viewProps + ")", e);
        }
    }

    @POST
    @Path("/{id}/topic/{topic_id}/association/{assoc_id}")
    @Transactional
    @Override
    public void addRelatedTopicToTopicmap(@PathParam("id") final long topicmapId,
                                          @PathParam("topic_id") final long topicId,
                                          @PathParam("assoc_id") final long assocId, final ViewProperties viewProps) {
        try {
            // Note: a Mapcontext association must have no workspace assignment as it is not user-deletable
            dmx.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Void>() {  // throws Exception
                @Override
                public Void call() {
                    // 1) add topic
                    Association topicMapcontext = fetchTopicMapcontext(topicmapId, topicId);
                    if (topicMapcontext == null) {
                        createTopicMapcontext(topicmapId, topicId, viewProps);
                    } else {
                        if (!visibility(topicMapcontext)) {
                            setTopicVisibility(topicmapId, topicId, true);
                        }
                    }
                    // 2) add association
                    // Note: it is an error if the association is already in the topicmap. In this case the topic is
                    // already in the topicmap too, and the Webclient would not send the request in the first place.
                    // ### TODO: rethink method contract. Do it analoguous to "add topic"?
                    addAssociationToTopicmap(topicmapId, assocId, new ViewProperties().put(PROP_PINNED, false));
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Adding related topic " + topicId + " (assocId=" + assocId + ") to topicmap " +
                topicmapId + " failed (viewProps=" + viewProps + ")", e);
        }
    }

    // ---

    @PUT
    @Path("/{id}/topic/{topic_id}")
    @Transactional
    @Override
    public void setTopicViewProperties(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                         ViewProperties viewProps) {
        storeTopicViewProperties(topicmapId, topicId, viewProps);
    }

    @PUT
    @Path("/{id}/association/{assoc_id}")
    @Transactional
    @Override
    public void setAssociationViewProperties(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId,
                                                                               ViewProperties viewProps) {
        storeAssociationViewProperties(topicmapId, assocId, viewProps);
    }

    @PUT
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Transactional
    @Override
    public void setTopicPosition(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                   @PathParam("x") int x, @PathParam("y") int y) {
        try {
            storeTopicViewProperties(topicmapId, topicId, new ViewProperties(x, y));
            me.setTopicPosition(topicmapId, topicId, x, y);
        } catch (Exception e) {
            throw new RuntimeException("Setting position of topic " + topicId + " in topicmap " + topicmapId +
                " failed ", e);
        }
    }

    @PUT
    @Path("/{id}/topic/{topic_id}/{visibility}")
    @Transactional
    @Override
    public void setTopicVisibility(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                                                     @PathParam("visibility") boolean visibility) {
        try {
            // remove associations
            if (!visibility) {
                for (Association assoc : dmx.getTopic(topicId).getAssociations()) {
                    Association assocMapcontext = fetchAssociationMapcontext(topicmapId, assoc.getId());
                    if (assocMapcontext != null) {
                        deleteAssociationMapcontext(assocMapcontext);
                    }
                }
            }
            // show/hide topic
            storeTopicViewProperties(topicmapId, topicId, new ViewProperties(visibility));
            // send message
            me.setTopicVisibility(topicmapId, topicId, visibility);
        } catch (Exception e) {
            throw new RuntimeException("Setting visibility of topic " + topicId + " in topicmap " + topicmapId +
                " failed ", e);
        }
    }

    @DELETE
    @Path("/{id}/association/{assoc_id}")
    @Transactional
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        try {
            Association assocMapcontext = fetchAssociationMapcontext(topicmapId, assocId);
            // Note: idempotence of remove-assoc-from-topicmap is needed for delete-muti
            if (assocMapcontext != null) {
                deleteAssociationMapcontext(assocMapcontext);
                me.removeAssociationFromTopicmap(topicmapId, assocId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Removing association " + assocId + " from topicmap " + topicmapId + " failed ",
                e);
        }
    }

    // ---

    @PUT
    @Path("/{id}/topics/{topicIds}/visibility/false")
    @Transactional
    @Override
    public void hideTopics(@PathParam("id") long topicmapId, @PathParam("topicIds") IdList topicIds) {
        hideMulti(topicmapId, topicIds, new IdList());
    }

    @PUT
    @Path("/{id}/assocs/{assocIds}/visibility/false")
    @Transactional
    @Override
    public void hideAssocs(@PathParam("id") long topicmapId, @PathParam("assocIds") IdList assocIds) {
        hideMulti(topicmapId, new IdList(), assocIds);
    }

    @PUT
    @Path("/{id}/topics/{topicIds}/assocs/{assocIds}/visibility/false")
    @Transactional
    @Override
    public void hideMulti(@PathParam("id") long topicmapId, @PathParam("topicIds") IdList topicIds,
                                                            @PathParam("assocIds") IdList assocIds) {
        logger.info("topicmapId=" + topicmapId + ", topicIds=" + topicIds + ", assocIds=" + assocIds);
        for (long id : topicIds) {
            setTopicVisibility(topicmapId, id, false);
        }
        for (long id : assocIds) {
            removeAssociationFromTopicmap(topicmapId, id);
        }
    }

    // ---

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public void setClusterPosition(@PathParam("id") long topicmapId, ClusterCoords coords) {
        for (ClusterCoords.Entry entry : coords) {
            setTopicPosition(topicmapId, entry.topicId, entry.x, entry.y);
        }
    }

    @PUT
    @Path("/{id}/translation/{x}/{y}")
    @Transactional
    @Override
    public void setTopicmapTranslation(@PathParam("id") long topicmapId, @PathParam("x") int transX,
                                                                         @PathParam("y") int transY) {
        try {
            ChildTopicsModel topicmapState = mf.newChildTopicsModel()
                .put("dmx.topicmaps.state", mf.newChildTopicsModel()
                    .put("dmx.topicmaps.translation", mf.newChildTopicsModel()
                        .put("dmx.topicmaps.translation_x", transX)
                        .put("dmx.topicmaps.translation_y", transY)));
            dmx.updateTopic(mf.newTopicModel(topicmapId, topicmapState));
        } catch (Exception e) {
            throw new RuntimeException("Setting translation of topicmap " + topicmapId + " failed (transX=" +
                transX + ", transY=" + transY + ")", e);
        }
    }

    // ---

    @Override
    public void registerTopicmapRenderer(TopicmapRenderer renderer) {
        logger.info("### Registering topicmap renderer \"" + renderer.getClass().getName() + "\"");
        topicmapRenderers.put(renderer.getUri(), renderer);
    }

    // ---

    @Override
    public void registerViewmodelCustomizer(ViewmodelCustomizer customizer) {
        logger.info("### Registering viewmodel customizer \"" + customizer.getClass().getName() + "\"");
        viewmodelCustomizers.add(customizer);
    }

    @Override
    public void unregisterViewmodelCustomizer(ViewmodelCustomizer customizer) {
        logger.info("### Unregistering viewmodel customizer \"" + customizer.getClass().getName() + "\"");
        if (!viewmodelCustomizers.remove(customizer)) {
            throw new RuntimeException("Unregistering viewmodel customizer failed (customizer=" + customizer + ")");
        }
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



    // ------------------------------------------------------------------------------------------------- Private Methods

    // --- Fetch ---

    private Map<Long, TopicViewModel> fetchTopics(Topic topicmapTopic, boolean includeChilds) {
        Map<Long, TopicViewModel> topics = new HashMap();
        List<RelatedTopic> relTopics = topicmapTopic.getRelatedTopics(TOPIC_MAPCONTEXT, "dmx.core.default",
            "dmx.topicmaps.topicmap_topic", null);  // othersTopicTypeUri=null
        if (includeChilds) {
            DMXUtils.loadChildTopics(relTopics);
        }
        for (RelatedTopic topic : relTopics) {
            topics.put(topic.getId(), createTopicViewModel(topic));
        }
        return topics;
    }

    private Map<Long, AssociationViewModel> fetchAssociations(Topic topicmapTopic) {
        Map<Long, AssociationViewModel> assocs = new HashMap();
        List<RelatedAssociation> relAssocs = topicmapTopic.getRelatedAssociations(ASSOCIATION_MAPCONTEXT,
            "dmx.core.default", "dmx.topicmaps.topicmap_association", null);
        for (RelatedAssociation assoc : relAssocs) {
            assocs.put(assoc.getId(), createAssocViewModel(assoc));
        }
        return assocs;
    }

    // ---

    private TopicViewModel createTopicViewModel(RelatedTopic topic) {
        try {
            ViewProperties viewProps = fetchTopicViewProperties(topic.getRelatingAssociation());
            invokeViewmodelCustomizers(topic, viewProps);
            return mf.newTopicViewModel(topic.getModel(), viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Creating viewmodel for topic " + topic.getId() + " failed", e);
        }
    }

    private AssociationViewModel createAssocViewModel(RelatedAssociation assoc) {
        try {
            ViewProperties viewProps = fetchAssocViewProperties(assoc.getRelatingAssociation());
            // invokeViewmodelCustomizers(assoc, viewProps);    // TODO: assoc customizers?
            return mf.newAssociationViewModel(assoc.getModel(), viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Creating viewmodel for association " + assoc.getId() + " failed", e);
        }
    }

    // ---

    private Association fetchTopicMapcontext(long topicmapId, long topicId) {
        return dmx.getAssociation(TOPIC_MAPCONTEXT, topicmapId, topicId, ROLE_TYPE_TOPICMAP, ROLE_TYPE_TOPIC);
    }

    private Association fetchAssociationMapcontext(long topicmapId, long assocId) {
        return dmx.getAssociationBetweenTopicAndAssociation(ASSOCIATION_MAPCONTEXT, topicmapId, assocId,
            ROLE_TYPE_TOPICMAP, ROLE_TYPE_ASSOCIATION);
    }

    // ---

    private void createTopicMapcontext(long topicmapId, long topicId, ViewProperties viewProps) {
        Association topicMapcontext = dmx.createAssociation(mf.newAssociationModel(TOPIC_MAPCONTEXT,
            mf.newTopicRoleModel(topicmapId, ROLE_TYPE_TOPICMAP),
            mf.newTopicRoleModel(topicId,    ROLE_TYPE_TOPIC)
        ));
        storeViewProperties(topicMapcontext, viewProps);
        //
        TopicViewModel topic = mf.newTopicViewModel(dmx.getTopic(topicId).getModel(), viewProps);
        me.addTopicToTopicmap(topicmapId, topic);
    }

    private void createAssociationMapcontext(long topicmapId, long assocId, ViewProperties viewProps) {
        Association assocMapcontext = dmx.createAssociation(mf.newAssociationModel(ASSOCIATION_MAPCONTEXT,
            mf.newTopicRoleModel(topicmapId,    ROLE_TYPE_TOPICMAP),
            mf.newAssociationRoleModel(assocId, ROLE_TYPE_ASSOCIATION)
        ));
        storeViewProperties(assocMapcontext, viewProps);
        //
        AssociationModel assoc = dmx.getAssociation(assocId).getModel();    // TODO: view props
        me.addAssociationToTopicmap(topicmapId, assoc);
    }

    // ---

    private void deleteAssociationMapcontext(Association assocMapcontext) {
        // Note: a mapcontext association has no workspace assignment -- it belongs to the system.
        // Deleting a mapcontext association is a privileged operation.
        dmx.getAccessControl().deleteAssociationMapcontext(assocMapcontext);
    }

    // ---

    private ViewProperties fetchTopicViewProperties(Association topicMapcontext) {
        return new ViewProperties(
            (Integer) topicMapcontext.getProperty(PROP_X),
            (Integer) topicMapcontext.getProperty(PROP_Y),
            visibility(topicMapcontext),
            pinned(topicMapcontext)
        );
    }

    private ViewProperties fetchAssocViewProperties(Association assocMapcontext) {
        return new ViewProperties().put(PROP_PINNED, pinned(assocMapcontext));
    }

    private boolean visibility(Association topicMapcontext) {
        return (Boolean) topicMapcontext.getProperty(PROP_VISIBILITY);
    }

    private boolean pinned(Association mapcontext) {
        return (Boolean) mapcontext.getProperty(PROP_PINNED);
    }

    // --- Store ---

    /**
     * Convenience.
     */
    private void storeTopicViewProperties(long topicmapId, long topicId, ViewProperties viewProps) {
        try {
            Association topicMapcontext = fetchTopicMapcontext(topicmapId, topicId);
            if (topicMapcontext == null) {
                throw new RuntimeException("Topic " + topicId + " is not contained in topicmap " + topicmapId);
            }
            storeViewProperties(topicMapcontext, viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Storing view properties of topic " + topicId + " failed " +
                "(viewProps=" + viewProps + ")", e);
        }
    }

    /**
     * Convenience.
     */
    private void storeAssociationViewProperties(long topicmapId, long assocId, ViewProperties viewProps) {
        try {
            Association assocMapcontext = fetchAssociationMapcontext(topicmapId, assocId);
            if (assocMapcontext == null) {
                throw new RuntimeException("Association " + assocId + " is not contained in topicmap " + topicmapId);
            }
            storeViewProperties(assocMapcontext, viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Storing view properties of association " + assocId + " failed " +
                "(viewProps=" + viewProps + ")", e);
        }
    }

    private void storeViewProperties(Association mapcontext, ViewProperties viewProps) {
        for (String propUri : viewProps) {
            mapcontext.setProperty(propUri, viewProps.get(propUri), false);    // addToIndex = false
        }
    }

    // --- Viewmodel Customizers ---

    private void invokeViewmodelCustomizers(RelatedTopic topic, ViewProperties viewProps) {
        for (ViewmodelCustomizer customizer : viewmodelCustomizers) {
            invokeViewmodelCustomizer(customizer, topic, viewProps);
        }
    }

    private void invokeViewmodelCustomizer(ViewmodelCustomizer customizer, RelatedTopic topic,
                                                                           ViewProperties viewProps) {
        try {
            customizer.enrichViewProperties(topic, viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Invoking viewmodel customizer for topic " + topic.getId() + " failed " +
                "(customizer=\"" + customizer.getClass().getName() + "\")", e);
        }
    }

    // --- Topicmap Renderers ---

    private TopicmapRenderer getTopicmapRenderer(String rendererUri) {
        TopicmapRenderer renderer = topicmapRenderers.get(rendererUri);
        //
        if (renderer == null) {
            throw new RuntimeException("\"" + rendererUri + "\" is an unknown topicmap renderer");
        }
        //
        return renderer;
    }

    // ---

    private InputStream invokeWebclient() {
        return dmx.getPlugin("systems.dmx.webclient").getStaticResource("/web/index.html");
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class Messenger {

        private String pluginUri;

        private Messenger(String pluginUri) {
            this.pluginUri = pluginUri;
        }

        // ---

        private void newTopicmap(Topic topicmapTopic) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "newTopicmap")
                    .put("args", new JSONObject()
                        .put("topicmapTopic", topicmapTopic.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"newTopicmap\" message:", e);
            }
        }

        private void addTopicToTopicmap(long topicmapId, TopicViewModel topic) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "addTopicToTopicmap")
                    .put("args", new JSONObject()
                        .put("topicmapId", topicmapId)
                        .put("viewTopic", topic.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"addTopicToTopicmap\" message:", e);
            }
        }

        private void addAssociationToTopicmap(long topicmapId, AssociationModel assoc) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "addAssocToTopicmap")
                    .put("args", new JSONObject()
                        .put("topicmapId", topicmapId)
                        .put("assoc", assoc.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"addAssocToTopicmap\" message:", e);
            }
        }

        private void setTopicPosition(long topicmapId, long topicId, int x, int y) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "setTopicPosition")
                    .put("args", new JSONObject()
                        .put("topicmapId", topicmapId)
                        .put("topicId", topicId)
                        .put("pos", new JSONObject()
                            .put("x", x)
                            .put("y", y)
                        )
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"setTopicPosition\" message:", e);
            }
        }

        private void setTopicVisibility(long topicmapId, long topicId, boolean visibility) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "setTopicVisibility")
                    .put("args", new JSONObject()
                        .put("topicmapId", topicmapId)
                        .put("topicId", topicId)
                        .put("visibility", visibility)
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"setTopicVisibility\" message:", e);
            }
        }

        private void removeAssociationFromTopicmap(long topicmapId, long assocId) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "removeAssocFromTopicmap")
                    .put("args", new JSONObject()
                        .put("topicmapId", topicmapId)
                        .put("assocId", assocId)
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"removeAssocFromTopicmap\" message:", e);
            }
        }

        // ---

        private void messageToAllButOne(JSONObject message) {
            dmx.getWebSocketsService().messageToAllButOne(request, pluginUri, message.toString());
        }
    }
}
