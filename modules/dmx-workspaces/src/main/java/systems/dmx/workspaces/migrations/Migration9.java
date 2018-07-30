package systems.dmx.workspaces.migrations;

import systems.dmx.core.Association;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.facets.FacetsService;
import systems.dmx.workspaces.WorkspacesService;

import java.util.logging.Logger;



/**
 * Deletes the workspace assignments of the "Topic Mapcontext" and "Association Mapcontext" associations.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.8.3
 */
public class Migration9 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private WorkspacesService workspacesService;

    @Inject
    private FacetsService facetsService;

    private long topicContext = 0, assocContext = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Deleting workspace assignments of \"Mapcontext\" associations");
        //
        for (Association assoc : dmx.getAssociationsByType("dmx.topicmaps.topic_mapcontext")) {
            deleteWorkspaceAssignment(assoc);
            topicContext++;
        }
        for (Association assoc : dmx.getAssociationsByType("dmx.topicmaps.association_mapcontext")) {
            deleteWorkspaceAssignment(assoc);
            assocContext++;
        }
        //
        logger.info("########## Deleting workspace assignments of \"Mapcontext\" associations complete\n    " +
            "\"Topic Mapcontext\" associations processed: " + topicContext + "\n    " +
            "\"Association Mapcontext\" associations processed: " + assocContext);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void deleteWorkspaceAssignment(Association assoc) {
        Topic workspace = workspacesService.getAssignedWorkspace(assoc.getId());
        if (workspace != null) {
            // 1) delete association
            facetsService.updateFacet(assoc, "dmx.workspaces.workspace_facet",
                mf.newFacetValueModel("dmx.workspaces.workspace").putDeletionRef(workspace.getId()));
            //
            // 2) delete property
            assoc.removeProperty(WorkspacesService.PROP_WORKSPACE_ID);
        }
    }
}
