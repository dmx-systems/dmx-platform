package de.deepamehta.workspaces.migrations;

import de.deepamehta.workspaces.WorkspacesService;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DMXObject;
import de.deepamehta.core.DMXType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Assigns standard association definitions (and their view config topics) to DeepaMehta workspace.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.6
 */
public class Migration6 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService wsService;

    private long dmxWorkspaceId;

    private long types = 0, standardTypes = 0, assocDefs = 0, configTopics = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Assigning standard association definitions (and their view config topics) to " +
            "DeepaMehta workspace");
        dmxWorkspaceId = getDMXWorkspace().getId();
        //
        for (TopicType topicType : dm4.getAllTopicTypes()) {
            assignWorkspace(topicType);
        }
        for (AssociationType assocType : dm4.getAllAssociationTypes()) {
            assignWorkspace(assocType);
        }
        //
        logger.info("########## Assigning standard association definitions (and their view config topics) to " +
            "DeepaMehta workspace complete\n    Types processed: " + types + "\n    Standard types: " +
            standardTypes + "\n    Association definitions: " + assocDefs + "\n    View config topics: " +
            configTopics);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void assignWorkspace(DMXType type) {
        types++;
        if (isDMXStandardType(type)) {
            standardTypes++;
            for (AssociationDefinition assocDef : type.getAssocDefs()) {
                assocDefs++;
                assignToDMXWorkspace(assocDef);
                for (Topic configTopic : assocDef.getViewConfig().getConfigTopics()) {
                    configTopics++;
                    assignToDMXWorkspace(configTopic);
                }
            }
        }
    }

    private void assignToDMXWorkspace(DMXObject object) {
        wsService.assignToWorkspace(object, dmxWorkspaceId);
    }

    // ### copy in WorkspacesPlugin.java
    private Topic getDMXWorkspace() {
        return wsService.getWorkspace(WorkspacesService.DEEPAMEHTA_WORKSPACE_URI);
    }

    // ### copy in WorkspacesPlugin.java
    private boolean isDMXStandardType(DMXType type) {
        return type.getUri().startsWith("dm4.");
    }
}
