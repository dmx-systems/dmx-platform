package de.deepamehta.plugins.workspaces;

import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.ViewConfiguration;
import de.deepamehta.core.service.Plugin;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class WorkspacesPlugin extends Plugin implements WorkspacesService {

    private static final String DEFAULT_WORKSPACE_NAME = "Default";

    // association type semantics
    private static final String WORKSPACE_TOPIC = "dm3.core.aggregation";   // A topic assigned to a workspace.
    private static final String WORKSPACE_TYPE  = "dm3.core.aggregation";   // A type assigned to a workspace.
    private static final String ROLE_TYPE_TOPIC = "dm3.core.whole";
    private static final String ROLE_TYPE_TYPE = "dm3.core.whole";
    private static final String ROLE_TYPE_WORKSPACE = "dm3.core.part";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
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
            if (topic.getTypeUri().equals("dm3.webclient.search") ||
                topic.getTypeUri().equals("dm3.workspaces.workspace")) {
                // Note 1: we do not relate search topics to a workspace.
                // Note 2: we do not relate workspaces to a workspace.
                logger.info("Assigning topic to a workspace ABORTED: searches and workspaces are not assigned (" +
                    topic + ")");
                return;
            }
            // check precondition 2
            if (clientContext == null) {
                logger.warning("Assigning " + topic + " to a workspace failed (current workspace is unknown " +
                    "(client context is not initialzed))");
                return;
            }
            // check precondition 3
            String wsId = clientContext.get("dm3_workspace_id");
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
                "This can happen if there is a stale \"dm3_workspace_id\" cookie.");
        }
    }

    /**
     * Adds "Workspaces" data field to all topic types.
     */
    @Override
    public void modifyTopicTypeHook(TopicType topicType, ClientContext clientContext) {
        String topicTypeUri = topicType.getUri();
        // skip our own types
        if (topicTypeUri.startsWith("dm3.workspaces.")) {
            return;
        }
        //
        if (!topicType.getDataTypeUri().equals("dm3.core.composite")) {
            return;
        }
        //
        if (!isSearchableUnit(topicType)) {
            return;
        }
        //
        logger.info("########## Associate type \"" + topicTypeUri + "\" with type \"dm3.workspaces.workspace\"");
        AssociationDefinition assocDef = new AssociationDefinition(topicTypeUri, "dm3.workspaces.workspace");
        assocDef.setAssocTypeUri("dm3.core.aggregation");
        assocDef.setCardinalityUri1("dm3.core.many");
        assocDef.setCardinalityUri2("dm3.core.many");
        assocDef.setViewConfig(new ViewConfiguration()); // FIXME: serialization fails if plugin developer forget to set
        //
        // TopicType workspaceType = dms.getTopicType("dm3.workspaces.workspace", null);
        topicType.addAssocDef(assocDef);
    }



    // **********************
    // *** Plugin Service ***
    // **********************



    @Override
    public Topic createWorkspace(String name) {
        logger.info("Creating workspace \"" + name + "\"");
        Composite comp = new Composite("{dm3.workspaces.name: \"" + name + "\"}");
        return dms.createTopic(new TopicModel("dm3.workspaces.workspace", comp), null);  // clientContext=null
    }

    @Override
    public void assignTopic(long workspaceId, long topicId) {
        checkWorkspaceId(workspaceId);
        //
        AssociationData assocData = new AssociationData(WORKSPACE_TOPIC);
        assocData.addTopicRole(new TopicRole(workspaceId, ROLE_TYPE_WORKSPACE));
        assocData.addTopicRole(new TopicRole(topicId, ROLE_TYPE_TOPIC));
        dms.createAssociation(assocData, null);         // clientContext=null
    }

    @Override
    public void assignType(long workspaceId, long typeId) {
        checkWorkspaceId(workspaceId);
        //
        AssociationData assocData = new AssociationData(WORKSPACE_TYPE);
        assocData.addTopicRole(new TopicRole(workspaceId, ROLE_TYPE_WORKSPACE));
        assocData.addTopicRole(new TopicRole(typeId, ROLE_TYPE_TYPE));
        dms.createAssociation(assocData, null);         // clientContext=null
    }

    @Override
    public Set<Topic> getWorkspaces(long typeId) {
        Topic typeTopic = dms.getTopic(typeId, null);   // clientContext=null
        return typeTopic.getRelatedTopics(WORKSPACE_TYPE, ROLE_TYPE_TYPE, ROLE_TYPE_WORKSPACE,
            "dm3.workspaces.workspace", false);         // fetchComposite=false
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void checkWorkspaceId(long workspaceId) {
        String typeUri = dms.getTopic(workspaceId, null).getTypeUri();
        if (!typeUri.equals("dm3.workspaces.workspace")) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but of type \"" +
                typeUri + "\")");
        }
    }

    // FIXME: the "is_searchable_unit" setting is possibly not a view configuration but part of the topic type model.
    // Evidence:
    // - Code is doubled, see isSearchableUnit() and getViewConfig() in WebclientPlugin.
    // - Dependency on Webclient plugin.
    private boolean isSearchableUnit(TopicType topicType) {
        Boolean isSearchableUnit = (Boolean) topicType.getViewConfig("dm3.webclient.view_config",
            "dm3.webclient.is_searchable_unit");
        return isSearchableUnit != null ? isSearchableUnit.booleanValue() : false;  // default is false
    }
}
