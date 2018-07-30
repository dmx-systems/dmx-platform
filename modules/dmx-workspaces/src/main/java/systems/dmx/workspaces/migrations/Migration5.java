package systems.dmx.workspaces.migrations;

import systems.dmx.core.Association;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.5
 */
public class Migration5 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: copy in WorkspacesPlugin.java
    private static final String PROP_WORKSPACE_ID = "dmx.workspaces.workspace_id";

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
        for (Topic topic : dmx.getAllTopics()) {
            initWorkspaceProperty(topic);
        }
        for (Association assoc : dmx.getAllAssociations()) {
            initWorkspaceProperty(assoc);
        }
        logger.info("########## Initializing workspace properties complete\n    Objects processed: " + objects +
            "\n    initialized: " + initialized + "\n    ambiguous: " + ambiguous);
        //
        // 2) Changes cardinality of the workspace facet to "one".
        //
        dmx.getTopicType("dmx.workspaces.workspace_facet").getAssocDef("dmx.workspaces.workspace")
            .setChildCardinalityUri("dmx.core.one");
        //
        // 3) Corrects URI of the "DMX" workspace: "de." -> "dmx."
        //
        dmx.getTopicByUri("de.workspaces.deepamehta").setUri("dmx.workspaces.deepamehta");
        //
        // 4) Sets the sharing mode of all workspaces to "Public".
        //
        for (Topic workspace : dmx.getTopicsByType("dmx.workspaces.workspace")) {
            workspace.update(mf.newTopicModel(mf.newChildTopicsModel()
                .putRef("dmx.workspaces.sharing_mode", "dmx.workspaces.public")
            ));
            // Note: instead of calling update(...) on the entire topic object we could update the child selectively:
            //     workspace.getChildTopics().setRef("dmx.workspaces.sharing_mode", "dmx.workspaces.public")
            // This would be much more concise. However in this case the topic will loose its label.
            // ### TODO: fix that error in the labeling mechanism. ### TODO: check if already fixed
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initWorkspaceProperty(DMXObject object) {
        List<RelatedTopic> workspaces = object.getRelatedTopics("dmx.core.aggregation", "dmx.core.parent",
            "dmx.core.child", "dmx.workspaces.workspace");
        objects++;
        switch (workspaces.size()) {
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
                    workspaces.size() + ") -- This object will get NO workspace assignment " +
                    "in DM 4.5 and will not be editable anymore");
                ambiguous++;
            }
        }
    }

    private boolean isUserAccountRelated(DMXObject object) {
        String typeUri = object.getTypeUri();
        return typeUri.equals("dmx.accesscontrol.user_account") ||
            typeUri.equals("dmx.accesscontrol.username") ||
            typeUri.equals("dmx.accesscontrol.password");
    }

    // ---

    private String info(DMXObject object) {
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
