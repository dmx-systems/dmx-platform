package de.deepamehta.core;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.Collection;
import java.util.List;
import java.util.Set;



public interface Type extends Topic {



    // === Model ===

    // --- Data Type ---

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);

    // --- Index Modes ---

    Set<IndexMode> getIndexModes();

    void setIndexModes(Set<IndexMode> indexModes);

    // --- Association Definitions ---

    Collection<AssociationDefinition> getAssocDefs();

    AssociationDefinition getAssocDef(String childTypeUri);

    void addAssocDef(AssociationDefinitionModel assocDef);

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
     * processing by returning false.
     *
     * @param   assocDef    the new association definition.
     *                      Note: in contrast to the other "update" methods this one does not support partial updates.
     *                      That is all association definition fields must be initialized.
     */
    void updateAssocDef(AssociationDefinitionModel assocDef);

    void removeAssocDef(String childTypeUri);

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

    void update(TypeModel model, ClientState clientState, Directives directives);
}
