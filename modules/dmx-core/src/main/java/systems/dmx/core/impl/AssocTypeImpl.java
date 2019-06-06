package systems.dmx.core.impl;

import systems.dmx.core.AssocType;
import systems.dmx.core.model.AssociationTypeModel;

/**
 * An association type that is attached to the {@link CoreService}.
 */
class AssocTypeImpl extends DMXTypeImpl implements AssocType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocTypeImpl(AssociationTypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // AssocType Implementation

    @Override
    public AssociationTypeModelImpl getModel() {
        return (AssociationTypeModelImpl) model;
    }

    @Override
    public void update(AssociationTypeModel updateModel) {
        model.update((AssociationTypeModelImpl) updateModel);     // ### FIXME: call through pl for access control
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    AssociationTypeModelImpl _getModel() {
        return pl._getAssociationType(getUri());
    }
}
