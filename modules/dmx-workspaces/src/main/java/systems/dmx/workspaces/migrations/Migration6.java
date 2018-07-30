package systems.dmx.workspaces.migrations;

import systems.dmx.workspaces.WorkspacesService;

import systems.dmx.core.AssociationDefinition;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;

import java.util.logging.Logger;



/**
 * Assigns standard association definitions (and their view config topics) to DMX workspace.
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
            "DMX workspace");
        dmxWorkspaceId = getDMXWorkspace().getId();
        //
        for (TopicType topicType : dmx.getAllTopicTypes()) {
            assignWorkspace(topicType);
        }
        for (AssociationType assocType : dmx.getAllAssociationTypes()) {
            assignWorkspace(assocType);
        }
        //
        logger.info("########## Assigning standard association definitions (and their view config topics) to " +
            "DMX workspace complete\n    Types processed: " + types + "\n    Standard types: " +
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
        return wsService.getWorkspace(WorkspacesService.DMX_WORKSPACE_URI);
    }

    // ### copy in WorkspacesPlugin.java
    private boolean isDMXStandardType(DMXType type) {
        return type.getUri().startsWith("dmx.");
    }
}
