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
    public String toString() {
        return "association type (" + super.toString() + ")";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association type";
    }

    @Override
    AssociationType instantiate() {
        return new AssociationTypeImpl(this, pl);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<AssociationModel> getAllInstances() {
        return pl.fetchAssociations("type_uri", new SimpleValue(uri));
    }

    // ---

    @Override
    void putInTypeCache() {
        pl.typeCache.putAssociationType(instantiate());
    }

    @Override
    void removeFromTypeCache() {
        pl.typeCache.removeAssociationType(uri);
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
