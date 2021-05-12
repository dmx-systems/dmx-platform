package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Directive;

import java.util.List;



/**
 * Data that underlies a {@link AssocType}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public class AssocTypeModelImpl extends TypeModelImpl implements AssocTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocTypeModelImpl(TypeModelImpl type) {
        super(type);
        if (type.dataTypeUri.equals(VALUE) || type.dataTypeUri.equals(ENTITY)) {
            throw new IllegalArgumentException("\"" + type.dataTypeUri + "\" is an illegal data type for an assoc " +
                "type. Use \"dmx.core.composite\" instead. " + type);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssocTypeModel addCompDef(CompDefModel compDef) {
        return (AssocTypeModel) super.addCompDef(compDef);
    }

    // ---

    @Override
    public AssocTypeModelImpl clone() {
        try {
            return (AssocTypeModelImpl) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a AssocTypeModel failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association type";
    }

    @Override
    AssocTypeImpl instantiate() {
        return new AssocTypeImpl(clone().filterReadableCompDefs(), al);
    }



    // === Implementation of abstract TypeModelImpl methods ===

    @Override
    List<AssocModelImpl> getAllInstances() {
        return al.db.queryAssocs("typeUri", uri);
    }

    // ---

    @Override
    Directive getUpdateTypeDirective() {
        return Directive.UPDATE_ASSOC_TYPE;
    }

    @Override
    Directive getDeleteTypeDirective() {
        return Directive.DELETE_ASSOC_TYPE;
    }
}
