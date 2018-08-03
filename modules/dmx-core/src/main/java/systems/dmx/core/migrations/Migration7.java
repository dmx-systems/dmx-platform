package systems.dmx.core.migrations;

import systems.dmx.core.Association;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Repairing assoc defs with missing "Include in Label" topic.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.8.6
 *
 * ### TODO: drop this
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
        dmx.getAssociationType("dmx.core.aggregation_def").getAssocDef("dmx.core.assoc_type#dmx.core.custom_assoc_type")
            .getChildTopics().set("dmx.core.include_in_label", false);
        dmx.getAssociationType("dmx.core.composition_def").getAssocDef("dmx.core.assoc_type#dmx.core.custom_assoc_type")
            .getChildTopics().set("dmx.core.include_in_label", false);
        //
        process(dmx.getAssociationsByType("dmx.core.composition_def"), 0);
        process(dmx.getAssociationsByType("dmx.core.aggregation_def"), 1);
        //
        logger.info("########## Repairing assoc defs with missing \"Include in Label\" topic complete\n    " +
            "Composition defs repaired: " + count[0][1] + "/" + count[0][0] + "\n    " +
            "Aggregation defs repaired: " + count[1][1] + "/" + count[1][0]);
    }

    private void process(List<Association> assocs, int i) {
        for (Association assoc : assocs) {
            ChildTopics childs = assoc.getChildTopics();
            Topic includeInLabel = childs.getTopicOrNull("dmx.core.include_in_label");
            if (includeInLabel == null) {
                childs.set("dmx.core.include_in_label", false);
                count[i][1]++;
            }
            count[i][0]++;
        }
    }
}
