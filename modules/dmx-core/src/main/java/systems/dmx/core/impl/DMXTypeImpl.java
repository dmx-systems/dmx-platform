package systems.dmx.core.impl;

import systems.dmx.core.CompDef;
import systems.dmx.core.DMXType;
import systems.dmx.core.ViewConfig;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;



abstract class DMXTypeImpl extends TopicImpl implements DMXType {

    // ---------------------------------------------------------------------------------------------------- Constructors

    DMXTypeImpl(TypeModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************
    // *** DMXType ***
    // ***************



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



    // === Composition Definitions ===

    @Override
    public final Collection<CompDef> getCompDefs() {
        return al.instantiate(getModel().getCompDefs());
    }

    @Override
    public final CompDef getCompDef(String compDefUri) {
        return getModel().getCompDef(compDefUri).instantiate();
    }

    @Override
    public final boolean hasCompDef(String compDefUri) {
        return getModel().hasCompDef(compDefUri);
    }

    @Override
    public final DMXType addCompDef(CompDefModel compDef) {
        return addCompDefBefore(compDef, null);     // beforeCompDefUri=null
    }

    @Override
    public final DMXType addCompDefBefore(CompDefModel compDef, String beforeCompDefUri) {
        CompDefModelImpl _compDef = (CompDefModelImpl) compDef;
        al.typeStorage.storeCompDef(_compDef);
        _getModel()._addCompDefBefore(_compDef, beforeCompDefUri);
        return this;
    }

    @Override
    public final DMXType removeCompDef(String compDefUri) {
        _getModel()._removeCompDef(compDefUri);
        return this;
    }



    // === View Configuration ===

    @Override
    public final ViewConfig getViewConfig() {
        PlayerModel configurable = al.typeStorage.newTypePlayer(getId());   // ### type ID is uninitialized
        return new ViewConfigImpl(configurable, getModel().getViewConfig(), al);
    }

    @Override
    public final Object getViewConfigValue(String configTypeUri, String childTypeUri) {
        return getModel().getViewConfigValue(configTypeUri, childTypeUri);
    }



    // ===

    @Override
    public void update(TypeModel updateModel) {
        _getModel().update((TypeModelImpl) updateModel);   // ### FIXME: call through al for access control
    }

    // ---

    @Override
    public TypeModelImpl getModel() {
        return (TypeModelImpl) model;
    }



    // === Iterable ===

    /**
     * Returns an interator which iterates this Type's comp def URIs.
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
