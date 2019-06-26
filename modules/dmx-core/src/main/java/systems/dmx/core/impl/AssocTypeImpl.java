package systems.dmx.core.impl;

import systems.dmx.core.AssocType;
import systems.dmx.core.model.AssocTypeModel;

/**
 * An association type that is attached to the {@link CoreService}.
 */
class AssocTypeImpl extends DMXTypeImpl implements AssocType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocTypeImpl(AssocTypeModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // AssocType Implementation

    @Override
    public AssocTypeModelImpl getModel() {
        return (AssocTypeModelImpl) model;
    }

    @Override
    public void update(AssocTypeModel updateModel) {
        model.update((AssocTypeModelImpl) updateModel);     // ### FIXME: call through al for access control
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    AssocTypeModelImpl _getModel() {
        return al._getAssocType(getUri());
    }
}
