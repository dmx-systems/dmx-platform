package systems.dmx.core;

import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;



/**
 * The (abstract) base class of both, {@link TopicType} and {@link AssocType}.
 * <p>
 * Besides the parts derived from {@link Topic} a <code>DMXType</code> has 3 parts: a <b>data type</b>, a
 * collection of <b>Composition Definitions</b> ({@link CompDef}), and a {@link ViewConfig}.
 * <p>
 * Types are referred to by <b>type URI</b>.
 */
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

    ViewConfig getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);



    // ===

    void update(TypeModel model);

    // ---

    TypeModel getModel();
}
