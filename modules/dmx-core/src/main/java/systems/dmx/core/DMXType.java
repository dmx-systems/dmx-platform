package systems.dmx.core;

import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;



public interface DMXType extends Topic, Iterable<String> {



    // === Data Type ===

    String getDataTypeUri();

    DMXType setDataTypeUri(String dataTypeUri);



    // === Association Definitions ===

    // TODO: drop it? We're Iterable meanwhile
    Collection<CompDef> getCompDefs();

    CompDef getCompDef(String assocDefUri);

    boolean hasCompDef(String assocDefUri);

    DMXType addCompDef(CompDefModel assocDef);

    /**
     * @param   beforeCompDefUri    the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    DMXType addCompDefBefore(CompDefModel assocDef, String beforeCompDefUri);

    DMXType removeCompDef(String assocDefUri);



    // === View Configuration ===

    ViewConfiguration getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);



    // ===

    void update(TypeModel model);

    // ---

    TypeModel getModel();
}
