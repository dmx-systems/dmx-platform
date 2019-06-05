package systems.dmx.core;

import systems.dmx.core.model.RelatedAssociationModel;



/**
 * An Assoc-Assoc pair.
 */
public interface RelatedAssociation extends RelatedObject, Assoc {

    RelatedAssociationModel getModel();
}
