package systems.dmx.core.impl;

import systems.dmx.core.model.RelatedAssocModel;



public class RelatedAssocModelImpl extends AssocModelImpl implements RelatedAssocModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssocModelImpl relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssocModelImpl(AssocModelImpl assoc, AssocModelImpl relatingAssoc) {
        super(assoc);
        this.relatingAssoc = relatingAssoc;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssocModelImpl getRelatingAssoc() {
        return relatingAssoc;
    }

    // FIXME: define toJSON()?
    // FIXME: define clone()?
    // Compare to RelatedTopicModelImpl.java

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Overrides DMXObjectModelImpl
     * ### Copy in RelatedTopicModelImpl
     */
    @Override
    boolean isReadable() {
        try {
            return super.isReadable() && getRelatingAssoc().isReadable();
        } catch (Exception e) {
            throw new RuntimeException("Checking related assoc READability failed (" + this + ")", e);
        }
    }

    @Override
    String className() {
        return "related association";
    }

    @Override
    RelatedAssocImpl instantiate() {
        return new RelatedAssocImpl(this, al);
    }
}
