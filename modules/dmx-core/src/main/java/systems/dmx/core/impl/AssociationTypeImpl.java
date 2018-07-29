package systems.dmx.core.impl;

import systems.dmx.core.AssociationType;
import systems.dmx.core.model.AssociationTypeModel;



/**
 * An association type that is attached to the {@link CoreService}.
 */
class AssociationTypeImpl extends DMXTypeImpl implements AssociationType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationTypeImpl(AssociationTypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** AssociationType Implementation ***
    // **************************************



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
