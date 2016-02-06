package de.deepamehta.core.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;



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
        dms.getAssociationType("dm4.core.aggregation_def").addAssocDefBefore(
            mf.newAssociationDefinitionModel("dm4.core.aggregation_def", "dm4.core.custom_assoc_type",
                "dm4.core.aggregation_def", "dm4.core.assoc_type", "dm4.core.many", "dm4.core.one"
            ),
            "dm4.core.include_in_label"
        );
        dms.getAssociationType("dm4.core.composition_def").addAssocDefBefore(
            mf.newAssociationDefinitionModel("dm4.core.aggregation_def", "dm4.core.custom_assoc_type",
                "dm4.core.composition_def", "dm4.core.assoc_type", "dm4.core.many", "dm4.core.one"
            ),
            "dm4.core.include_in_label"
        );
    }
}
