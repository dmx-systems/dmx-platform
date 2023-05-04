package systems.dmx.workspaces.migrations;

import static systems.dmx.workspaces.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Logger;



/**
 * Repairs workspaces: remove random assignments of workspace constituting associations.
 * <p>
 * Part of DMX 5.3
 * Runs ALWAYS.
 */
public class Migration5 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService ws;

    private Map<String, Integer> repaired = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        for (Topic workspace : dmx.getTopicsByType(WORKSPACE)) {
            int r = repair(workspace, WORKSPACE_NAME) +
                    repair(workspace, WORKSPACE_DESCRIPTION) +
                    repair(workspace, SHARING_MODE);
            if (r > 0) {
                repaired.put(workspace.getSimpleValue().toString(), r);
            }
        }
        logger.info("##### Repaired workspaces: " + repaired.size() + "\n  " + repaired.keySet().stream().map(
            ws -> "\"" + ws + "\" (" + repaired.get(ws) + " assocs)"
        ).collect(Collectors.joining("\n  ")));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private int repair(Topic workspace, String compDefUri) {
        RelatedTopic topic = workspace.getChildTopics().getTopicOrNull(compDefUri);
        if (topic != null) {
            Assoc assoc = topic.getRelatingAssoc();
            Topic _workspace = ws.getAssignedWorkspace(assoc.getId());
            if (_workspace != null) {
                ws.assignToWorkspace(assoc, -1);
                return 1;
            }
        }
        return 0;
    }
}
