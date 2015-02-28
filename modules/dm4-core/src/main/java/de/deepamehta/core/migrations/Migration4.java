package de.deepamehta.core.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;



/**
 * Adds child type "Association Type" to "Composition Definition" and "Composition Definition" association types.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.6
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        dms.getAssociationType("dm4.core.composition_def").addAssocDefBefore(
            new AssociationDefinitionModel("dm4.core.aggregation_def",
                "dm4.core.composition_def", "dm4.core.assoc_type", "dm4.core.many", "dm4.core.one"
            ),
            "dm4.core.include_in_label"
        );
        dms.getAssociationType("dm4.core.aggregation_def").addAssocDefBefore(
            new AssociationDefinitionModel("dm4.core.aggregation_def",
                "dm4.core.aggregation_def", "dm4.core.assoc_type", "dm4.core.many", "dm4.core.one"
            ),
            "dm4.core.include_in_label"
        );
    }
}
