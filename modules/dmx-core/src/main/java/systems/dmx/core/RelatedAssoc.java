package systems.dmx.core;

import systems.dmx.core.model.RelatedAssociationModel;



/**
 * An Assoc-Assoc pair.
 */
public interface RelatedAssoc extends RelatedObject, Assoc {

    RelatedAssociationModel getModel();
}
