package systems.dmx.core.impl;

import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RelatedAssociationModel;



class RelatedAssociationModelImpl extends AssociationModelImpl implements RelatedAssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModelImpl relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssociationModelImpl(AssociationModelImpl assoc, AssociationModelImpl relatingAssoc) {
        super(assoc);
        this.relatingAssoc = relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationModelImpl getRelatingAssociation() {
        return relatingAssoc;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "related association";
    }

    @Override
    RelatedAssociationImpl instantiate() {
        return new RelatedAssociationImpl(this, pl);
    }
}
