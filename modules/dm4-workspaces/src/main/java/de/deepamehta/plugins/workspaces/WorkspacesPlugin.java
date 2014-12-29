package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;
import de.deepamehta.plugins.facets.model.FacetValue;
import de.deepamehta.plugins.facets.service.FacetsService;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import java.util.Iterator;
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

    // Query parameter
    private static final String PARAM_NO_WORKSPACE_ASSIGNMENT = "no_workspace_assignment";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private FacetsService facetsService;

    @Inject
    private TopicmapsService topicmapsService;

    @Context
    private UriInfo uriInfo;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** WorkspacesService Implementation ***
    // ****************************************



    @POST
    @Path("/{name}/{uri:[^/]*?}/{sharing_mode_uri}")    // Note: default is [^/]+?     // +? is a "reluctant" quantifier
    @Transactional
    @Override
    public Topic createWorkspace(@PathParam("name") String name, @PathParam("uri") String uri,
                                 @PathParam("sharing_mode_uri") SharingMode sharingMode) {
        logger.info("Creating workspace \"" + name + "\" (uri=\"" + uri + "\", sharingMode=" + sharingMode + ")");
        // create workspace
        Topic workspace = dms.createTopic(new TopicModel(uri, "dm4.workspaces.workspace", new ChildTopicsModel()
            .put("dm4.workspaces.name", name)
            .putRef("dm4.workspaces.sharing_mode", sharingMode.getUri())
        ));
        // create default topicmap and assign to workspace
        Topic topicmap = topicmapsService.createTopicmap(TopicmapsService.DEFAULT_TOPICMAP_NAME,
            TopicmapsService.DEFAULT_TOPICMAP_RENDERER);
        assignToWorkspace(topicmap, workspace.getId());
        //
        return workspace;
    }

    // ---

    @Override
    public Topic getWorkspace(String uri) {
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
    public boolean isAssignedToWorkspace(Topic topic, long workspaceId) {
        // ### TODO: check property instead facet
        return facetsService.hasFacet(topic.getId(), "dm4.workspaces.workspace_facet", workspaceId);
    }

    // ---

    @Override
    public void assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        checkArgument(workspaceId);
        //
        _assignToWorkspace(object, workspaceId);
    }

    @Override
    public void assignTypeToWorkspace(Type type, long workspaceId) {
        checkArgument(workspaceId);
        //
        _assignToWorkspace(type, workspaceId);
        for (Topic configTopic : type.getViewConfig().getConfigTopics()) {
            _assignToWorkspace(configTopic, workspaceId);
        }
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    /**
     * Creates the "Default" workspace.
     */
    @Override
    public void postInstall() {
        createWorkspace(DEEPAMEHTA_WORKSPACE_NAME, DEEPAMEHTA_WORKSPACE_URI, DEEPAMEHTA_WORKSPACE_SHARING_MODE);
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void introduceTopicType(TopicType topicType) {
        long workspaceId = workspaceIdForType(topicType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignTypeToWorkspace(topicType, workspaceId);
    }

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
        if (abortAssignment(topic)) {
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
        // the default workspace. This would not help in gaining data consistency because the topics created so far
        // (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
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
        if (abortAssignment(assoc)) {
            return;
        }
        // Note: we must avoid a vicious circle that would occur when the association is an workspace assignment.
        if (isWorkspaceAssignment(assoc)) {
            return;
        }
        //
        long workspaceId = workspaceId();
        // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
        // the default workspace. This would not help in gaining data consistency because the associations created
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

    private long workspaceIdForType(Type type) {
        long workspaceId = workspaceId();
        if (workspaceId != -1) {
            return workspaceId;
        } else {
            // assign types of the DeepaMehta standard distribution to the default workspace
            if (isDeepaMehtaStandardType(type)) {
                Topic defaultWorkspace = getDeepaMehtaWorkspace();
                // Note: the default workspace is NOT required to exist ### TODO: think about it
                if (defaultWorkspace != null) {
                    return defaultWorkspace.getId();
                }
            }
        }
        return -1;
    }

    // ---

    private long getAssignedWorkspaceId(long id) {
        if (!dms.hasProperty(id, PROP_WORKSPACE_ID)) {
            return -1;
        }
        //
        return (Long) dms.getProperty(id, PROP_WORKSPACE_ID);
    }

    private void _assignToWorkspace(DeepaMehtaObject object, long workspaceId) {
        try {
            // 1) create assignment association
            // Note 1: we are refering to an existing workspace. So we must add a topic reference.
            // Note 2: workspace_facet is a multi-facet. So we must call addRef() (as opposed to putRef()).
            // ### TODO: redefine workspace_facet as a single-facet and use putRef() then
            FacetValue value = new FacetValue("dm4.workspaces.workspace").addRef(workspaceId);
            facetsService.updateFacet(object, "dm4.workspaces.workspace_facet", value);
            //
            // 2) store assignment property
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(object) + " to workspace " + workspaceId + " failed", e);
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

    private Topic getDeepaMehtaWorkspace() {
        return getWorkspace(DEEPAMEHTA_WORKSPACE_URI);
    }

    private void applyWorkspaceFilter(Iterator<? extends Topic> topics, long workspaceId) {
        while (topics.hasNext()) {
            Topic topic = topics.next();
            if (!isAssignedToWorkspace(topic, workspaceId)) {
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

    // ### TODO: abort topic and association assignments separately?
    private boolean abortAssignment(DeepaMehtaObject object) {
        try {
            String value = uriInfo.getQueryParameters().getFirst(PARAM_NO_WORKSPACE_ASSIGNMENT);
            if (value == null) {
                // no such parameter in request
                return false;
            }
            if (!value.equals("false") && !value.equals("true")) {
                throw new RuntimeException("\"" + value + "\" is an unexpected value for the \"" +
                    PARAM_NO_WORKSPACE_ASSIGNMENT + "\" query parameter (expected are \"false\" or \"true\")");
            }
            boolean abort = value.equals("true");
            if (abort) {
                logger.info("### Workspace assignment for " + info(object) + " ABORTED -- \"" +
                    PARAM_NO_WORKSPACE_ASSIGNMENT + "\" query parameter detected");
            }
            return abort;
        } catch (IllegalStateException e) {
            // Note: this happens if a UriInfo method is called outside request scope
            return false;
        }
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
