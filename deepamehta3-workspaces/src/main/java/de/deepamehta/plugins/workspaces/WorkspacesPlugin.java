package de.deepamehta.plugins.workspaces;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Plugin;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class WorkspacesPlugin extends Plugin {

    private static enum RelationType {
        WORKSPACE_TYPE      // A type assigned to a workspace. Direction is from workspace to type.
    }

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    // Note: we must use the postCreateHook to create the relation because at pre_create the document has no ID yet.
    @Override
    public void postCreateHook(Topic topic, Map<String, String> clientContext) {
        // check precondition 1
        if (topic.typeUri.equals("de/deepamehta/core/topictype/SearchResult") ||
            topic.typeUri.equals("de/deepamehta/core/topictype/Workspace")) {
            // Note 1: we do not relate search results to a workspace. Otherwise the search result would appear
            // as relation when displaying the workspace. That's because a "SEARCH_RESULT" relation is not be
            // created if there is another relation already.
            // Note 2: we do not relate workspaces to a workspace. This would be contra-intuitive.
            logger.info(topic + " is deliberately not assigned to any workspace");
            return;
        }
        // check precondition 2
        if (clientContext == null) {
            logger.warning(topic + " can't be related to a workspace because current workspace is unknown " +
                "(client context is not initialzed)");
            return;
        }
        // check precondition 3
        String workspaceId = clientContext.get("dm3_workspace_id");
        if (workspaceId == null) {
            logger.warning(topic + " can't be related to a workspace because current workspace is unknown " +
                "(no setting found in client context)");
            return;
        }
        // relate topic to workspace
        dms.createRelation("RELATION", topic.id, Long.parseLong(workspaceId), null);
    }

    /**
     * Adds "Workspaces" data field to all topic types.
     */
    @Override
    public void modifyTopicTypeHook(TopicType topicType, Map<String, String> clientContext) {
        //
        DataField workspacesField = new DataField("Workspaces", "reference");
        workspacesField.setUri("de/deepamehta/core/property/Workspaces");
        workspacesField.setRefTopicTypeUri("de/deepamehta/core/topictype/Workspace");
        workspacesField.setEditor("checkboxes");
        //
        topicType.addDataField(workspacesField);
    }



    // ******************
    // *** Public API ***
    // ******************



    public Topic createWorkspace(String name) {
        Map properties = new HashMap();
        properties.put("de/deepamehta/core/property/Name", name);
        return dms.createTopic("de/deepamehta/core/topictype/Workspace", properties, null);     // clientContext=null
    }

    public void assignType(long workspaceId, long typeId) {
        dms.createRelation(RelationType.WORKSPACE_TYPE.name(), workspaceId, typeId, null);      // properties=null
    }

    public List<RelatedTopic> getWorkspaces(long typeId) {
        // Note: takes a type ID instead of a type URI to avoid endless recursion through dms.getTopicType().
        // Consider the Access Control plugin: determining the permissions for a type with MEMBER role would involve
        // retrieving the type itself. This in turn would involve determining its permissions ...
        // See AccessControlPlugin.userIsMember()
        //
        // long typeId = dms.getTopicType(typeUri, null).id;
        return dms.getRelatedTopics(typeId, asList("de/deepamehta/core/topictype/Workspace"),
            asList(RelationType.WORKSPACE_TYPE.name() + ";OUTGOING"), null);
    }
}
