package de.deepamehta.core;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;

import java.util.Map;
import java.util.Set;



public interface Type extends Topic {

    // --- Data Type ---

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);

    // --- Index Modes ---

    Set<IndexMode> getIndexModes();

    void setIndexModes(Set<IndexMode> indexModes);

    // --- Association Definitions ---

    Map<String, AssociationDefinition> getAssocDefs();

    AssociationDefinition getAssocDef(String assocDefUri);

    void addAssocDef(AssociationDefinitionModel assocDef);

    void updateAssocDef(AssociationDefinitionModel assocDef);

    void removeAssocDef(String assocDefUri);

    // --- View Configuration ---

    ViewConfiguration getViewConfig();

    // FIXME: to be dropped
    Object getViewConfig(String typeUri, String settingUri);
}
