package systems.dmx.core;

import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;



public interface DMXType extends Topic, Iterable<String> {



    // === Data Type ===

    String getDataTypeUri();

    DMXType setDataTypeUri(String dataTypeUri);



    // === Composition Definitions ===

    // TODO: drop it? We're Iterable meanwhile
    Collection<CompDef> getCompDefs();

    CompDef getCompDef(String compDefUri);

    boolean hasCompDef(String compDefUri);

    DMXType addCompDef(CompDefModel compDef);

    /**
     * @param   beforeCompDefUri    the URI of the comp def <i>before</i> the given comp def is inserted.
     *                              If <code>null</code> the comp def is appended at the end.
     */
    DMXType addCompDefBefore(CompDefModel compDef, String beforeCompDefUri);

    DMXType removeCompDef(String compDefUri);



    // === View Configuration ===

    ViewConfiguration getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);



    // ===

    void update(TypeModel model);

    // ---

    TypeModel getModel();
}
