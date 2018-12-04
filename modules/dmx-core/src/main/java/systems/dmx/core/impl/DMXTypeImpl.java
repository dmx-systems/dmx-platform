package systems.dmx.core.impl;

import systems.dmx.core.AssociationDefinition;
import systems.dmx.core.DMXType;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;



abstract class DMXTypeImpl extends TopicImpl implements DMXType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    DMXTypeImpl(TypeModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************
    // *** DMXType Implementation ***
    // ******************************



    // === Data Type ===

    @Override
    public final String getDataTypeUri() {
        return getModel().getDataTypeUri();
    }

    @Override
    public final DMXType setDataTypeUri(String dataTypeUri) {
        _getModel().updateDataTypeUri(dataTypeUri);     // TODO: should call _updateDataTypeUri()
        return this;
    }



    // === Association Definitions ===

    @Override
    public final Collection<AssociationDefinition> getAssocDefs() {
        return pl.instantiate(getModel().getAssocDefs());
    }

    @Override
    public final AssociationDefinition getAssocDef(String assocDefUri) {
        return getModel().getAssocDef(assocDefUri).instantiate();
    }

    @Override
    public final boolean hasAssocDef(String assocDefUri) {
        return getModel().hasAssocDef(assocDefUri);
    }

    @Override
    public final DMXType addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    @Override
    public final DMXType addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        AssociationDefinitionModelImpl _assocDef = (AssociationDefinitionModelImpl) assocDef;
        pl.typeStorage.storeAssociationDefinition(_assocDef);
        _getModel()._addAssocDefBefore(_assocDef, beforeAssocDefUri);
        return this;
    }

    @Override
    public final DMXType removeAssocDef(String assocDefUri) {
        _getModel()._removeAssocDef(assocDefUri);
        return this;
    }



    // === View Configuration ===

    @Override
    public final ViewConfiguration getViewConfig() {
        RoleModel configurable = pl.typeStorage.newTypeRole(getId());   // ### type ID is uninitialized
        return new ViewConfigurationImpl(configurable, getModel().getViewConfig(), pl);
    }

    @Override
    public final Object getViewConfigValue(String configTypeUri, String childTypeUri) {
        return getModel().getViewConfigValue(configTypeUri, childTypeUri);
    }



    // ===

    @Override
    public void update(TypeModel updateModel) {
        _getModel().update((TypeModelImpl) updateModel);   // ### FIXME: call through pl for access control
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) model;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this Type's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return getModel().iterator();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Returns the <i>internal</i> (= <b>kernel</b>) model underlying this type.
     * <p>
     * Note: type updates must be performed on the internal type model, not the userland's type model (as returned by
     * <code>getModel()</code>). Performing an update on the <b>userland</b>'s type model would have no effect, as it
     * is transient. The userland's type model is always a <i>cloned</i> and filtered (= "projected") version of a
     * kernel type model which is created on-the-fly each time a specific user requests it.
     */
    abstract TypeModelImpl _getModel();

    // --- Label Configuration ---

    // TODO: drop it?
    final List<String> getLabelConfig() {
        return getModel().getLabelConfig();
    }
}
