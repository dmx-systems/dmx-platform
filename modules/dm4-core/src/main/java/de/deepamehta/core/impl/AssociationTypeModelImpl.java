package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TypeModel;



/**
 * Data that underlies a {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModelImpl extends TypeModelImpl implements AssociationTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationTypeModelImpl(TypeModel type) {
        super(type);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (AssociationTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "association type (" + super.toString() + ")";
    }
}
