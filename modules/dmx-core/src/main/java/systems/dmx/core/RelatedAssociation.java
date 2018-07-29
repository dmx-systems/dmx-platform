package systems.dmx.core;

import systems.dmx.core.model.RelatedAssociationModel;



/**
 * An Association-Association pair.
 */
public interface RelatedAssociation extends RelatedObject, Association {

    RelatedAssociationModel getModel();
}
