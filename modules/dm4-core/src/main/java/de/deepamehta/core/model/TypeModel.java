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

    Collection<AssociationDefinitionModel> getAssocDefs();

    AssociationDefinitionModel getAssocDef(String assocDefUri);

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

    // ---

    /**
     * Finds an assoc def by ID and returns its URI (at index 0). Returns the URI of the next-in-sequence
     * assoc def as well (at index 1), or null if the found assoc def is the last one.
     *
     * ### TODO: remove from public API
     */
    String[] findAssocDefUris(long assocDefId);

    // ### TODO: remove from public API
    boolean hasSameAssocDefSequence(Collection<AssociationDefinitionModel> assocDefs);

    // ### TODO: remove from public API
    void rehashAssocDef(String assocDefUri, String beforeAssocDefUri);

    // ### TODO: remove from public API
    void rehashAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs);

    // ### TODO: remove from public API
    void replaceAssocDef(AssociationDefinitionModel assocDef);

    // ### TODO: remove from public API
    void replaceAssocDef(AssociationDefinitionModel assocDef, String oldAssocDefUri, String beforeAssocDefUri);



    // === Label Configuration ===

    List<String> getLabelConfig();

    void setLabelConfig(List<String> labelConfig);

    // ---

    // ### TODO: remove from public API
    void replaceInLabelConfig(String newAssocDefUri, String oldAssocDefUri);

    // ### TODO: remove from public API
    void removeFromLabelConfig(String assocDefUri);



    // === View Configuration ===

    ViewConfigurationModel getViewConfigModel();

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    Object getViewConfig(String typeUri, String settingUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
