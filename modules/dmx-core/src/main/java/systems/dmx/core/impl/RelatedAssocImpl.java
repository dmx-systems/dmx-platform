package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedAssoc;



/**
 * An Assoc-Assoc pair that is attached to the {@link PersistenceLayer}.
 */
class RelatedAssocImpl extends AssocImpl implements RelatedAssoc {

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssocImpl(RelatedAssociationModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Assoc getRelatingAssociation() {
        return getModel().getRelatingAssociation().instantiate();
    }

    @Override
    public RelatedAssociationModelImpl getModel() {
        return (RelatedAssociationModelImpl) model;
    }
}
