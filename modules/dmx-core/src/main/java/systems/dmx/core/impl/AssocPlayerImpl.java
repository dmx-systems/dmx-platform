package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocPlayer;



/**
 * An association role that is attached to the {@link PersistenceLayer}.
 */
class AssocPlayerImpl extends PlayerImpl implements AssocPlayer {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocPlayerImpl(AssociationRoleModelImpl model, AssocModelImpl assoc) {
        super(model, assoc);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssocPlayer Implementation ===

    @Override
    public Assoc getAssociation() {
        return (Assoc) getPlayer();
    }



    // === PlayerImpl Overrides ===

    @Override
    public AssociationRoleModelImpl getModel() {
        return (AssociationRoleModelImpl) model;
    }
}
