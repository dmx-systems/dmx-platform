package systems.dmx.core.migrations;

import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.service.Migration;



/**
 * Adds child type "Association Type" to "Composition Definition" and "Aggregation Definition" association types.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.6
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        // Note: "Aggregation Definition" must be updated before "Composition Definition" as the child type
        // is added via "Aggregation Definition" and this very definition is changed here.
        dmx.getAssociationType("dmx.core.aggregation_def").addAssocDefBefore(
            mf.newAssociationDefinitionModel("dmx.core.aggregation_def", "dmx.core.custom_assoc_type", false, false,
                "dmx.core.aggregation_def", "dmx.core.assoc_type", "dmx.core.many", "dmx.core.one"
            ),
            "dmx.core.identity_attr"
        );
        dmx.getAssociationType("dmx.core.composition_def").addAssocDefBefore(
            mf.newAssociationDefinitionModel("dmx.core.aggregation_def", "dmx.core.custom_assoc_type", false, false,
                "dmx.core.composition_def", "dmx.core.assoc_type", "dmx.core.many", "dmx.core.one"
            ),
            "dmx.core.identity_attr"
        );
    }
}
