package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;



/**
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        //
        // 1) Changes cardinality of the workspace facet to "one".
        //
        dms.getTopicType("dm4.workspaces.workspace_facet").getAssocDef("dm4.workspaces.workspace")
            .setChildCardinalityUri("dm4.core.one");
        //
        // 2) Corrects URI of the "DeepaMehta" workspace: "de." -> "dm4."
        //
        dms.getTopic("uri", new SimpleValue("de.workspaces.deepamehta")).setUri("dm4.workspaces.deepamehta");
        //
        // 3) Sets the sharing mode of all workspaces to "Public".
        //
        for (Topic workspace : dms.getTopics("dm4.workspaces.workspace", 0)) {
            workspace.update(new TopicModel(null, new ChildTopicsModel()
                .putRef("dm4.workspaces.sharing_mode", "dm4.workspaces.public")
            ));
            // Note: instead of calling update(...) on the entire topic object we could update the child selectively:
            //     workspace.getChildTopics().setRef("dm4.workspaces.sharing_mode", "dm4.workspaces.public")
            // This would be much more concise. However in this case the topic will loose its label.
            // ### TODO: fix that error in the labeling mechanism.
        }
    }
}
