package systems.dmx.core.model;

import java.util.Collection;
import java.util.List;



public interface TypeModel extends TopicModel, Iterable<String> {



    // === Data Type ===

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);



    // === Index Modes ===

    List<IndexMode> getIndexModes();

    void addIndexMode(IndexMode indexMode);



    // === Association Definitions ===

    Collection<? extends AssociationDefinitionModel> getAssocDefs();

    AssociationDefinitionModel getAssocDef(String assocDefUri);

    AssociationDefinitionModel getAssocDef(long assocDefId);

    boolean hasAssocDef(String assocDefUri);

    /**
     * @param   assocDef    the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    TypeModel addAssocDef(AssociationDefinitionModel assocDef);

    /**
     * @param   assocDef            the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    TypeModel addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri);

    AssociationDefinitionModel removeAssocDef(String assocDefUri);



    // === View Configuration ===

    ViewConfigurationModel getViewConfig();

    // TODO: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the core type model.
    Object getViewConfigValue(String configTypeUri, String childTypeUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
