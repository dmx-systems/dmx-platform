package de.deepamehta.plugins.workspaces.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;

import java.util.logging.Logger;



/**
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration5 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dm4.workspaces.workspace_id";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long objects = 0, initialized = 0, ambiguous = 0;;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        //
        // 1) Initializes workspace properties
        //
        logger.info("########## Initializing workspace properties");
        for (Topic topic : dms.getAllTopics()) {
            initWorkspaceProperty(topic);
        }
        for (Association assoc : dms.getAllAssociations()) {
            initWorkspaceProperty(assoc);
        }
        logger.info("########## Initializing workspace properties complete\n    Objects processed: " + objects +
            "\n    initialized: " + initialized + "\n    ambiguous: " + ambiguous);
        //
        // 2) Changes cardinality of the workspace facet to "one".
        //
        dms.getTopicType("dm4.workspaces.workspace_facet").getAssocDef("dm4.workspaces.workspace")
            .setChildCardinalityUri("dm4.core.one");
        //
        // 3) Corrects URI of the "DeepaMehta" workspace: "de." -> "dm4."
        //
        dms.getTopic("uri", new SimpleValue("de.workspaces.deepamehta")).setUri("dm4.workspaces.deepamehta");
        //
        // 4) Sets the sharing mode of all workspaces to "Public".
        //
        for (Topic workspace : dms.getTopics("dm4.workspaces.workspace")) {
            workspace.update(mf.newTopicModel(mf.newChildTopicsModel()
                .putRef("dm4.workspaces.sharing_mode", "dm4.workspaces.public")
            ));
            // Note: instead of calling update(...) on the entire topic object we could update the child selectively:
            //     workspace.getChildTopics().setRef("dm4.workspaces.sharing_mode", "dm4.workspaces.public")
            // This would be much more concise. However in this case the topic will loose its label.
            // ### TODO: fix that error in the labeling mechanism. ### TODO: check if already fixed
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initWorkspaceProperty(DeepaMehtaObject object) {
        ResultList<RelatedTopic> workspaces = object.getRelatedTopics("dm4.core.aggregation",
            "dm4.core.parent", "dm4.core.child", "dm4.workspaces.workspace");
        objects++;
        switch (workspaces.getSize()) {
        case 0:
            break;
        case 1:
            long workspaceId = workspaces.get(0).getId();
            object.setProperty(PROP_WORKSPACE_ID, workspaceId, false);      // addToIndex=false
            initialized++;
            break;
        default:
            // Note: user account related topics get proprietary workspace assignments.
            // This is performed in Access Control migration #8 later on.
            if (!isUserAccountRelated(object)) {
                logger.warning("##### " + info(object) + " has ambiguous workspace assignments (" +
                    workspaces.getSize() + ") -- This object will get NO workspace assignment " +
                    "in DM 4.5 and will not be editable anymore");
                ambiguous++;
            }
        }
    }

    private boolean isUserAccountRelated(DeepaMehtaObject object) {
        String typeUri = object.getTypeUri();
        return typeUri.equals("dm4.accesscontrol.user_account") ||
            typeUri.equals("dm4.accesscontrol.username") ||
            typeUri.equals("dm4.accesscontrol.password");
    }

    // ---

    private String info(DeepaMehtaObject object) {
        if (object instanceof Topic) {
            return "topic " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\", value=\"" +
                object.getSimpleValue() + "\", uri=\"" + object.getUri() + "\")";
        } else if (object instanceof Association) {
            return "association " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\")";
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }
}
