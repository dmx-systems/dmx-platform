package de.deepamehta.core.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Repairs assoc defs with missing \"Include in Label\" topic.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8.6
 */
public class Migration7 extends Migration {

    private int[][] count = new int[2][2];

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        logger.info("########## Repairing assoc defs with missing \"Include in Label\" topic");
        //
        // - Must repair the label config of the types ("comp def" and "aggr def") BEFORE the instances can be repaired
        // - Must repair "aggr def" BEFORE "comp def"
        dm4.getAssociationType("dm4.core.aggregation_def").getAssocDef("dm4.core.assoc_type#dm4.core.custom_assoc_type")
            .getChildTopics().set("dm4.core.include_in_label", false);
        dm4.getAssociationType("dm4.core.composition_def").getAssocDef("dm4.core.assoc_type#dm4.core.custom_assoc_type")
            .getChildTopics().set("dm4.core.include_in_label", false);
        //
        process(dm4.getAssociationsByType("dm4.core.composition_def"), 0);
        process(dm4.getAssociationsByType("dm4.core.aggregation_def"), 1);
        //
        logger.info("########## Repairing assoc defs with missing \"Include in Label\" topic complete\n    " +
            "Composition defs repaired: " + count[0][1] + "/" + count[0][0] + "\n    " +
            "Aggregation defs repaired: " + count[1][1] + "/" + count[1][0]);
    }

    private void process(List<Association> assocs, int i) {
        for (Association assoc : assocs) {
            ChildTopics childs = assoc.getChildTopics();
            Topic includeInLabel = childs.getTopicOrNull("dm4.core.include_in_label");
            if (includeInLabel == null) {
                childs.set("dm4.core.include_in_label", false);
                count[i][1]++;
            }
            count[i][0]++;
        }
    }
}
