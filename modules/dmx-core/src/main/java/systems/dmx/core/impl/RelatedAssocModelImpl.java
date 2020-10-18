package systems.dmx.core.impl;

import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedAssocModel;



public class RelatedAssocModelImpl extends AssocModelImpl implements RelatedAssocModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssocModelImpl relatingAssoc;
    private DMXObjectModelImpl otherObject;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssocModelImpl(AssocModelImpl assoc, AssocModelImpl relatingAssoc) {
        this(assoc, relatingAssoc, null);
    }

    RelatedAssocModelImpl(AssocModelImpl assoc, AssocModelImpl relatingAssoc, DMXObjectModelImpl otherObject) {
        super(assoc);
        this.relatingAssoc = relatingAssoc;
        this.otherObject = otherObject;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssocModelImpl getRelatingAssoc() {
        return relatingAssoc;
    }

    @Override
    public <M extends DMXObjectModel> M getOtherDMXObject() {
        return (M) otherObject;
    }

    // Note: no toJSON() and clone() here as not needed so far

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
