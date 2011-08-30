package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Plugin;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends Plugin implements WorkspacesService {

    private static final String DEFAULT_WORKSPACE_NAME = "Default";

    // association type semantics ### FIXME: to be dropped. Model-driven manipulators required.
    private static final String WORKSPACE_TOPIC     = "dm4.workspaces.workspace_context";
    private static final String WORKSPACE_TYPE      = "dm4.workspaces.workspace_context";
    private static final String ROLE_TYPE_TOPIC     = "dm4.workspaces.workspace_topic";
    private static final String ROLE_TYPE_TYPE      = "dm4.workspaces.workspace_type";
    private static final String ROLE_TYPE_WORKSPACE = "dm4.core.default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    /**
     * Creates the "Default" workspace.
     */
    @Override
    public void postInstallPluginHook() {
        createWorkspace(DEFAULT_WORKSPACE_NAME);
    }

    /**
     * Assigns a newly created topic to the current workspace.
     */
    @Override
    public void postCreateHook(Topic topic, ClientContext clientContext) {
        long workspaceId = -1;
        try {
            // check precondition 1
            if (topic.getTypeUri().equals("dm4.webclient.search") ||
                topic.getTypeUri().equals("dm4.workspaces.workspace")) {
                // Note 1: we do not relate search topics to a workspace.
                // Note 2: we do not relate workspaces to a workspace.
                logger.info("Assigning topic to a workspace ABORTED: searches and workspaces are not assigned (" +
                    topic + ")");
                return;
            }
            // check precondition 2
            if (clientContext == null) {
                // ### logger.warning("Assigning " + topic + " to a workspace failed (current workspace is unknown " +
                // ###     "(client context is not initialzed))");
                return;
            }
            // check precondition 3
            String wsId = clientContext.get("dm4_workspace_id");
            if (wsId == null) {
                logger.warning("Assigning " + topic + " to a workspace failed (current workspace is unknown " +
                    "(no setting found in client context))");
                return;
            }
            // assign topic to workspace
            workspaceId = Long.parseLong(wsId);
            assignTopic(workspaceId, topic.getId());
        } catch (Exception e) {
            logger.warning("Assigning " + topic + " to workspace " + workspaceId + " failed (" + e + "). " +
                "This can happen after a DB reset if there is a stale \"dm4_workspace_id\" browser cookie.");
        }
    }

    /**
     * Adds a "Workspace" association to all topic types.
     * FIXME: not ready for the prime time
    @Override
    public void modifyTopicTypeHook(TopicType topicType, ClientContext clientContext) {
        String topicTypeUri = topicType.getUri();
        // skip our own types
        if (topicTypeUri.startsWith("dm4.workspaces.")) {
            return;
        }
        //
        if (!topicType.getDataTypeUri().equals("dm4.core.composite")) {
            return;
        }
        //
        if (!isSearchableUnit(topicType)) {
            return;
        }
        //
        logger.info("########## Associate type \"" + topicTypeUri + "\" with type \"dm4.workspaces.workspace\"");
        AssociationDefinition assocDef = new AssociationDefinition(topicTypeUri, "dm4.workspaces.workspace");
        assocDef.setAssocTypeUri("dm4.core.aggregation_def");
        assocDef.setWholeCardinalityUri("dm4.core.many");
        assocDef.setPartCardinalityUri("dm4.core.many");
        assocDef.setViewConfigModel(new ViewConfigurationModel());  // FIXME: serialization fails if plugin developer
                                                                    // forget to set
        //
        topicType.addAssocDef(assocDef);
    } */



    // **********************
    // *** Plugin Service ***
    // **********************



    @Override
    public Topic createWorkspace(String name) {
        logger.info("Creating workspace \"" + name + "\"");
        CompositeValue comp = new CompositeValue("{dm4.workspaces.name: \"" + name + "\"}");
        return dms.createTopic(new TopicModel("dm4.workspaces.workspace", comp), null);  // clientContext=null
    }

    @Override
    public void assignTopic(long workspaceId, long topicId) {
        checkWorkspaceId(workspaceId);
        //
        AssociationModel assocModel = new AssociationModel(WORKSPACE_TOPIC);
        assocModel.setRoleModel1(new TopicRoleModel(workspaceId, ROLE_TYPE_WORKSPACE));
        assocModel.setRoleModel2(new TopicRoleModel(topicId, ROLE_TYPE_TOPIC));
        dms.createAssociation(assocModel, null);         // clientContext=null
    }

    @Override
    public void assignType(long workspaceId, long typeId) {
        checkWorkspaceId(workspaceId);
        //
        AssociationModel assocModel = new AssociationModel(WORKSPACE_TYPE);
        assocModel.setRoleModel1(new TopicRoleModel(workspaceId, ROLE_TYPE_WORKSPACE));
        assocModel.setRoleModel2(new TopicRoleModel(typeId, ROLE_TYPE_TYPE));
        dms.createAssociation(assocModel, null);         // clientContext=null
    }

    @Override
    public Set<RelatedTopic> getWorkspaces(long typeId) {
        Topic typeTopic = dms.getTopic(typeId, false, null);    // fetchComposite=false, clientContext=null
        return typeTopic.getRelatedTopics(WORKSPACE_TYPE, ROLE_TYPE_TYPE, null,
            "dm4.workspaces.workspace", false, false);          // fetchComposite=false
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = dms.getTopic(workspaceId, false, null).getTypeUri();   // fetchComposite=false
        if (!typeUri.equals("dm4.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but of type \"" +
                typeUri + "\")");
        }
    }

    // FIXME: the "is_searchable_unit" setting is possibly not a view configuration but part of the topic type model.
    // Evidence:
    // - Code is doubled, see isSearchableUnit() and getViewConfig() in WebclientPlugin.
    // - Dependency on Webclient plugin.
    private boolean isSearchableUnit(TopicType topicType) {
        Boolean isSearchableUnit = (Boolean) topicType.getViewConfig("dm4.webclient.view_config",
            "dm4.webclient.is_searchable_unit");
        return isSearchableUnit != null ? isSearchableUnit.booleanValue() : false;  // default is false
    }
}
