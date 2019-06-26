package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedAssoc;



/**
 * An Assoc-Assoc pair that is attached to the {@link AccessLayer}.
 */
class RelatedAssocImpl extends AssocImpl implements RelatedAssoc {

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssocImpl(RelatedAssocModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Assoc getRelatingAssoc() {
        return getModel().getRelatingAssoc().instantiate();
    }

    @Override
    public RelatedAssocModelImpl getModel() {
        return (RelatedAssocModelImpl) model;
    }
}
