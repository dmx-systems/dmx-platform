package systems.dmx.core.model;

import java.util.Collection;
import java.util.List;



public interface TypeModel extends TopicModel, Iterable<String> {



    // === Data Type ===

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);



    // === Composition Definitions ===

    // TODO: drop it? We're Iterable meanwhile
    Collection<? extends CompDefModel> getCompDefs();

    CompDefModel getCompDef(String compDefUri);

    boolean hasCompDef(String compDefUri);

    /**
     * @param   compDef     the comp def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    TypeModel addCompDef(CompDefModel compDef);

    /**
     * @param   compDef             the comp def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeCompDefUri    the URI of the comp def <i>before</i> the given comp def is inserted.
     *                              If <code>null</code> the comp def is appended at the end.
     */
    TypeModel addCompDefBefore(CompDefModel compDef, String beforeCompDefUri);

    CompDefModel removeCompDef(String compDefUri);



    // === View Configuration ===

    ViewConfigModel getViewConfig();

    // TODO: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the core type model.
    Object getViewConfigValue(String configTypeUri, String childTypeUri);

    void setViewConfig(ViewConfigModel viewConfig);
}
