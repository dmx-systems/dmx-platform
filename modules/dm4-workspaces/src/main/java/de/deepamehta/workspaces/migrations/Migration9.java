package de.deepamehta.workspaces.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.facets.FacetsService;
import de.deepamehta.workspaces.WorkspacesService;

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
        for (Association assoc : dm4.getAssociationsByType("dm4.topicmaps.topic_mapcontext")) {
            deleteWorkspaceAssignment(assoc);
            topicContext++;
        }
        for (Association assoc : dm4.getAssociationsByType("dm4.topicmaps.association_mapcontext")) {
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
            facetsService.updateFacet(assoc, "dm4.workspaces.workspace_facet",
                mf.newFacetValueModel("dm4.workspaces.workspace").putDeletionRef(workspace.getId()));
            //
            // 2) delete property
            assoc.removeProperty(WorkspacesService.PROP_WORKSPACE_ID);
        }
    }
}
