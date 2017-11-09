package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.Directive;

import java.util.List;



/**
 * Data that underlies a {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModelImpl extends TypeModelImpl implements AssociationTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationTypeModelImpl(TypeModelImpl type) {
        super(type);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (AssociationTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public AssociationTypeModelImpl clone() {
        try {
            return (AssociationTypeModelImpl) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a AssociationTypeModel failed", e);
        }
    }

    @Override
    public String toString() {
        // TODO
        return super.toString();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association type";
    }

    @Override
    AssociationTypeImpl instantiate() {
        // Note: declaration and assignment is required for type inference to work (at least in Java 6)
        AssociationTypeModelImpl type = clone().filterReadableAssocDefs();
        return new AssociationTypeImpl(type, pl);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<AssociationModelImpl> getAllInstances() {
        return pl.fetchAssociations("typeUri", new SimpleValue(uri));
    }

    // ---

    @Override
    Directive getUpdateTypeDirective() {
        return Directive.UPDATE_ASSOCIATION_TYPE;
    }

    @Override
    Directive getDeleteTypeDirective() {
        return Directive.DELETE_ASSOCIATION_TYPE;
    }
}
