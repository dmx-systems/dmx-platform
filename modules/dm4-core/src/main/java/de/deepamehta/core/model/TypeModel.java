package de.deepamehta.core.model;

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



    // === Label Configuration ===

    List<String> getLabelConfig();

    void setLabelConfig(List<String> labelConfig);



    // === View Configuration ===

    ViewConfigurationModel getViewConfigModel();

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    Object getViewConfig(String typeUri, String settingUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
