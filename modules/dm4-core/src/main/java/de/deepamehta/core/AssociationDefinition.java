package de.deepamehta.core;

import de.deepamehta.core.model.AssociationDefinitionModel;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinition extends Association {

    String getAssocDefUri();

    /**
     * @return  The custom association type, or <code>null</code> if not set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  The type to be used to create an association instance based on this association definition.
     *          This is the custom association type if set, otherwise this is <code>dm4.core.composition</code>
     *          or <code>dm4.core.aggregation</code> depending on this association definition's type.
     *          Is never <code>null</code>.
     */
    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getParentCardinalityUri();

    String getChildCardinalityUri();

    ViewConfiguration getViewConfig();

    AssociationDefinitionModel getModel();

    // ---

    void setParentCardinalityUri(String parentCardinalityUri);

    void setChildCardinalityUri(String childCardinalityUri);

    // === Updating ===

    void update(AssociationDefinitionModel model);
}
