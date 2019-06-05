package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Directive;

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
        if (type.dataTypeUri.equals("dmx.core.value") || type.dataTypeUri.equals("dmx.core.identity")) {
            throw new IllegalArgumentException("\"" + type.dataTypeUri + "\" is an illegal data type for an assoc " +
                "type. Use \"dmx.core.composite\" instead. " + type);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationTypeModel addCompDef(CompDefModel compDef) {
        return (AssociationTypeModel) super.addCompDef(compDef);
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association type";
    }

    @Override
    AssociationTypeImpl instantiate() {
        // Note: declaration and assignment is required for type inference to work (at least in Java 6)
        AssociationTypeModelImpl type = clone().filterReadableCompDefs();
        return new AssociationTypeImpl(type, pl);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<AssocModelImpl> getAllInstances() {
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
