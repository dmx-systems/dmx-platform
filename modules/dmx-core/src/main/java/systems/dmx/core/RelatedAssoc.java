package systems.dmx.core;

import systems.dmx.core.model.RelatedAssocModel;



/**
 * An Assoc-Assoc pair.
 */
public interface RelatedAssoc extends RelatedObject, Assoc {

    RelatedAssocModel getModel();
}
