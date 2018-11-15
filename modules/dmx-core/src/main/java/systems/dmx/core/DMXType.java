package systems.dmx.core;

import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.TypeModel;

import java.util.Collection;



public interface DMXType extends Topic {



    // === Data Type ===

    String getDataTypeUri();

    DMXType setDataTypeUri(String dataTypeUri);



    // === Association Definitions ===

    Collection<AssociationDefinition> getAssocDefs();

    AssociationDefinition getAssocDef(String assocDefUri);

    boolean hasAssocDef(String assocDefUri);

    DMXType addAssocDef(AssociationDefinitionModel assocDef);

    /**
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    DMXType addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri);

    DMXType removeAssocDef(String assocDefUri);



    // === View Configuration ===

    ViewConfiguration getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);



    // ===

    void update(TypeModel model);

    // ---

    TypeModel getModel();
}
