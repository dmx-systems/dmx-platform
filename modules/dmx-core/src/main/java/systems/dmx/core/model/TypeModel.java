package systems.dmx.core.model;

import java.util.Collection;
import java.util.List;



public interface TypeModel extends TopicModel, Iterable<String> {



    // === Data Type ===

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);



    // === Association Definitions ===

    // TODO: drop it? We're Iterable meanwhile
    Collection<? extends CompDefModel> getCompDefs();

    CompDefModel getCompDef(String compDefUri);

    boolean hasCompDef(String compDefUri);

    /**
     * @param   assocDef    the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    TypeModel addCompDef(CompDefModel assocDef);

    /**
     * @param   assocDef            the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeCompDefUri    the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    TypeModel addCompDefBefore(CompDefModel assocDef, String beforeCompDefUri);

    CompDefModel removeCompDef(String compDefUri);



    // === View Configuration ===

    ViewConfigurationModel getViewConfig();

    // TODO: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the core type model.
    Object getViewConfigValue(String configTypeUri, String childTypeUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
