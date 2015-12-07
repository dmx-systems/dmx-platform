package de.deepamehta.core;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TypeModel;

import java.util.Collection;
import java.util.List;



public interface Type extends Topic {



    // === Model ===

    // --- Data Type ---

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);

    // --- Index Modes ---

    List<IndexMode> getIndexModes();

    void addIndexMode(IndexMode indexMode);

    // --- Association Definitions ---

    Collection<AssociationDefinition> getAssocDefs();

    AssociationDefinition getAssocDef(String assocDefUri);

    boolean hasAssocDef(String assocDefUri);

    Type addAssocDef(AssociationDefinitionModel assocDef);

    /**
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    Type addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri);

    /**
     * Note: in contrast to the other "update" methods this one updates the memory only, not the DB!
     * If you want to update memory and DB use {@link AssociationDefinition#update}.
     * <p>
     * This method is here to support a special case: the user retypes an association which results in
     * a changed type definition. In this case the DB is already up-to-date and only the type's memory
     * representation must be updated. So, here the DB update is the *cause* for a necessary memory-update.
     * Normally the situation is vice-versa: the DB update is the necessary *effect* of a memory-update.
     * <p>
     * ### TODO: get rid of this peculiar situation and remove this method. This might be achieved by using
     * the PRE_UPDATE_ASSOCIATION hook instead the POST_UPDATE_ASSOCIATION hook in the Type Editor module.
     * On pre-update we would perform a regular {@link AssociationDefinition#update} and suppress further
     * processing by returning false. ### FIXDOC
     *
     * @param   assocDef    the new association definition.
     *                      Note: in contrast to the other "update" methods this one does not support partial updates.
     *                      That is all association definition fields must be initialized. ### FIXDOC
     */
    void updateAssocDef(AssociationModel assoc);

    Type removeAssocDef(String assocDefUri);

    // --- Label Configuration ---

    List<String> getLabelConfig();

    void setLabelConfig(List<String> labelConfig);

    // --- View Configuration ---

    ViewConfiguration getViewConfig();

    // FIXME: to be dropped
    Object getViewConfig(String typeUri, String settingUri);

    // ---

    TypeModel getModel();



    // === Updating ===

    void update(TypeModel model);
}
