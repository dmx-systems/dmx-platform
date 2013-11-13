package de.deepamehta.core;

import de.deepamehta.core.model.RelatedAssociationModel;



/**
 * An Association-Association pair.
 */
public interface RelatedAssociation extends Association {

    Association getRelatingAssociation();   // ### TODO: rename to getAssociation()?

    RelatedAssociationModel getModel();
}
