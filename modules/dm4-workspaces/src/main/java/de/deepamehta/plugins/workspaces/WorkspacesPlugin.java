package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.model.FacetValue;
import de.deepamehta.plugins.facets.service.FacetsService;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.core.service.event.IntroduceAssociationTypeListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



@Path("/workspace")
@Consumes("application/json")
@Produces("application/json")
public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, IntroduceTopicTypeListener,
                                                                                    IntroduceAssociationTypeListener,
                                                                                    PostCreateTopicListener,
                                                                                    PostCreateAssociationListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Property URIs
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private FacetsService facetsService;

    @Inject
    private TopicmapsService topicmapsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** WorkspacesService Implementation ***
    // ****************************************



    @POST
    @Path("/{name}/{uri:[^/]*?}/{sharing_mode_uri}")    // Note: default is [^/]+?     // +? is a "reluctant" quantifier
    @Transactional
    @Override
    public Topic createWorkspace(@PathParam("name") final String name, @PathParam("uri") final String uri,
                                 @PathParam("sharing_mode_uri") final SharingMode sharingMode) {
        final String operation = "Creating workspace \"" + name + "\" ";
        final String info = "(uri=\"" + uri + "\", sharingMode=" + sharingMode + ")";
        try {
            // We suppress standard workspace assignment here as 1) a workspace itself gets no assignment at all,
            // and 2) the workspace's default topicmap requires a special assignment. See step 2) below.
            return dms.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    logger.info(operation + info);
                    //
                    // 1) create workspace
                    Topic workspace = dms.createTopic(
                        new TopicModel(uri, "dm4.workspaces.workspace", new ChildTopicsModel()
                            .put("dm4.workspaces.name", name)
                            .putRef("dm4.workspaces.sharing_mode", sharingMode.getUri())));
                    //
                    // 2) create default topicmap and assign to workspace
                    Topic topicmap = topicmapsService.createTopicmap(TopicmapsService.DEFAULT_TOPICMAP_NAME,
                        TopicmapsService.DEFAULT_TOPICMAP_RENDERER);
                    // Note: user <anonymous> has no READ access to the workspace just created as it has no owner.
                    // So we must use the privileged assignToWorkspace() call here. This is to support the
                    // "DM4 Sign-up" 3rd-party plugin.
                    dms.getAccessControl().assignToWorkspace(topicmap, workspace.getId());
                    //
                    return workspace;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(operation + "failed " + info, e);
        }
    }

    // ---

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/{uri}")
    @Override
    public Topic getWorkspace(@PathParam("uri") String uri) {
        Topic workspace = dms.getTopic("uri", new SimpleValue(uri));
        if (workspace == null) {
            throw new RuntimeException("Workspace \"" + uri + "\" does not exist");
        }
        return workspace;
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/{id}/topics/{type_uri}")
    @Override
    public ResultList<RelatedTopic> getAssignedTopics(@PathParam("id") long workspaceId,
                                                      @PathParam("type_uri") String topicTypeUri) {
        ResultList<RelatedTopic> topics = dms.getTopics(topicTypeUri, 0);   // maxResultSize=0
        applyWorkspaceFilter(topics.iterator(), workspaceId);
        return topics;
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/object/{id}")
    @Override
    public Topic getAssignedWorkspace(@PathParam("id") long objectId) {
        long workspaceId = getAssignedWorkspaceId(objectId);
        if (workspaceId == -1) {
            return null;
        }
        return dms.getTopic(workspaceId);
    }

    @Override
    public boolean isAssignedToWorkspace(long objectId, long workspaceId) {
        return getAssignedWorkspaceId(objectId) == workspaceId;
    }

    // ---

    // Note: part of REST API, not part of OSGi service
    @PUT
    @Path("/{workspace_id}/object/{object_id}")
    @Transactional
    public Directives assignToWorkspace(@PathParam("object_id") long objectId,
                                        @PathParam("workspace_id") long workspaceId) {
        assignToWorkspace(dms.getObject(objectId), workspaceId);
        return Directives.get();
    }

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        checkArgument(workspaceId);
        _assignToWorkspace(object, workspaceId);
    }

    @Override
    public void assignTypeToWorkspace(Type type, long workspaceId) {
        assignToWorkspace(type, workspaceId);
        // view config topics
        for (Topic configTopic : type.getViewConfig().getConfigTopics()) {
            _assignToWorkspace(configTopic, workspaceId);
        }
        // association definitions
        for (AssociationDefinition assocDef : type.getAssocDefs()) {
            _assignToWorkspace(assocDef, workspaceId);
            // view config topics (of association definition)
            for (Topic configTopic : assocDef.getViewConfig().getConfigTopics()) {
                _assignToWorkspace(configTopic, workspaceId);
            }
        }
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    /**
     * Takes care the DeepaMehta standard types (and their parts) get an assignment to the DeepaMehta workspace.
     * This is important in conjunction with access control.
     * Note: type introduction is aborted if at least one of these conditions apply:
     *     - A workspace cookie is present. In this case the type gets its workspace assignment the regular way (this 
     *       plugin's post-create listeners). This happens e.g. when a type is created interactively in the Webclient.
     *     - The type is not a DeepaMehta standard type. In this case the 3rd-party plugin developer is responsible
     *       for doing the workspace assignment (in case the type is created programmatically while a migration).
     *       DM can't know to which workspace a 3rd-party type belongs to. A type is regarded a DeepaMehta standard
     *       type if its URI begins with "dm4."
     */
    @Override
    public void introduceTopicType(TopicType topicType) {
        long workspaceId = workspaceIdForType(topicType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignTypeToWorkspace(topicType, workspaceId);
    }

    /**
     * Takes care the DeepaMehta standard types (and their parts) get an assignment to the DeepaMehta workspace.
     * This is important in conjunction with access control.
     * Note: type introduction is aborted if at least one of these conditions apply:
     *     - A workspace cookie is present. In this case the type gets its workspace assignment the regular way (this 
     *       plugin's post-create listeners). This happens e.g. when a type is created interactively in the Webclient.
     *     - The type is not a DeepaMehta standard type. In this case the 3rd-party plugin developer is responsible
     *       for doing the workspace assignment (in case the type is created programmatically while a migration).
     *       DM can't know to which workspace a 3rd-party type belongs to. A type is regarded a DeepaMehta standard
     *       type if its URI begins with "dm4."
     */
    @Override
    public void introduceAssociationType(AssociationType assocType) {
        long workspaceId = workspaceIdForType(assocType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignTypeToWorkspace(assocType, workspaceId);
    }

    // ---

    /**
     * Assigns every created topic to the current workspace.
     */
    @Override
    public void postCreateTopic(Topic topic) {
        if (workspaceAssignmentIsSuppressed(topic)) {
            return;
        }
        // Note: we must avoid a vicious circle that would occur when editing a workspace. A Description topic
        // would be created (as no description is set when the workspace is created) and be assigned to the
        // workspace itself. This would create an endless recursion while bubbling the modification timestamp.
        if (isWorkspaceDescription(topic)) {
            return;
        }
        //
        long workspaceId = workspaceId();
        // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
        // the DeepaMehta workspace. This would not help in gaining data consistency because the topics created
        // so far (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
        // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
        // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
        // the other hand we don't have such a mechanism (and don't want one either).
        if (workspaceId == -1) {
            return;
        }
        //
        assignToWorkspace(topic, workspaceId);
    }

    /**
     * Assigns every created association to the current workspace.
     */
    @Override
    public void postCreateAssociation(Association assoc) {
        if (workspaceAssignmentIsSuppressed(assoc)) {
            return;
        }
        // Note: we must avoid a vicious circle that would occur when the association is an workspace assignment.
        if (isWorkspaceAssignment(assoc)) {
            return;
        }
        //
        long workspaceId = workspaceId();
        // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
        // the DeepaMehta workspace. This would not help in gaining data consistency because the associations created
        // so far (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
        // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
        // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
        // the other hand we don't have such a mechanism (and don't want one either).
        if (workspaceId == -1) {
            return;
        }
        //
        assignToWorkspace(assoc, workspaceId);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private long workspaceId() {
        Cookies cookies = Cookies.get();
        if (!cookies.has("dm4_workspace_id")) {
            return -1;
        }
        return cookies.getLong("dm4_workspace_id");
    }

    /**
     * Returns the ID of the DeepaMehta workspace or -1 to signal abortion of type introduction.
     */
    private long workspaceIdForType(Type type) {
        return workspaceId() == -1 && isDeepaMehtaStandardType(type) ? getDeepaMehtaWorkspace().getId() : -1;
    }

    // ---

    // ### TODO: copy in AccessControlImpl.java
    private long getAssignedWorkspaceId(long objectId) {
        return dms.hasProperty(objectId, PROP_WORKSPACE_ID) ? (Long) dms.getProperty(objectId, PROP_WORKSPACE_ID) : -1;
    }

    private void _assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        try {
            // 1) create assignment association
            facetsService.updateFacet(object, "dm4.workspaces.workspace_facet",
                new FacetValue("dm4.workspaces.workspace").putRef(workspaceId));
            // Note: we are refering to an existing workspace. So we must put a topic *reference* (using putRef()).
            //
            // 2) store assignment property
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(object) + " to workspace " + workspaceId + " failed (" +
                object + ")", e);
        }
    }

    // --- Helper ---

    private boolean isDeepaMehtaStandardType(Type type) {
        return type.getUri().startsWith("dm4.");
    }

    private boolean isWorkspaceDescription(Topic topic) {
        return topic.getTypeUri().equals("dm4.workspaces.description");
    }

    private boolean isWorkspaceAssignment(Association assoc) {
        if (assoc.getTypeUri().equals("dm4.core.aggregation")) {
            Topic topic = assoc.getTopic("dm4.core.child");
            if (topic != null && topic.getTypeUri().equals("dm4.workspaces.workspace")) {
                return true;
            }
        }
        return false;
    }

    // ---

    /**
     * Returns the DeepaMehta workspace or throws an exception if it doesn't exist.
     */
    private Topic getDeepaMehtaWorkspace() {
        return getWorkspace(DEEPAMEHTA_WORKSPACE_URI);
    }

    private void applyWorkspaceFilter(Iterator<? extends Topic> topics, long workspaceId) {
        while (topics.hasNext()) {
            Topic topic = topics.next();
            if (!isAssignedToWorkspace(topic.getId(), workspaceId)) {
                topics.remove();
            }
        }
    }

    /**
     * Checks if the topic with the specified ID exists and is a Workspace. If not, an exception is thrown.
     */
    private void checkArgument(long topicId) {
        String typeUri = dms.getTopic(topicId).getTypeUri();
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + topicId + " is not a workspace (but of type \"" + typeUri +
                "\")");
        }
    }

    /**
     * Returns true if standard workspace assignment is currently suppressed for the current thread.
     */
    private boolean workspaceAssignmentIsSuppressed(DeepaMehtaObject object) {
        boolean abort = dms.getAccessControl().workspaceAssignmentIsSuppressed();
        if (abort) {
            logger.info("### Standard workspace assignment for " + info(object) + " SUPPRESSED");
        }
        return abort;
    }

    // ---

    // ### FIXME: copied from Access Control
    // ### TODO: add shortInfo() to DeepaMehtaObject interface
    private String info(DeepaMehtaObject object) {
        if (object instanceof TopicType) {
            return "topic type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof AssociationType) {
            return "association type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof Topic) {
            return "topic " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\", uri=\"" + object.getUri() +
                "\")";
        } else if (object instanceof Association) {
            return "association " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\")";
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }
}
