package systems.dmx.core;

import systems.dmx.core.model.AssociationDefinitionModel;



/**
 * Definition of an association between 2 topic types -- part of DMX's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinition extends Association {

    String getAssocDefUri();

    // ---

    String getParentTypeUri();

    String getChildTypeUri();

    // ---

    /**
     * @return  The custom association type, or <code>null</code> if not set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  The type to be used to create an association instance based on this association definition.
     *          This is the custom association type if set, otherwise this is <code>dmx.core.composition</code>
     *          or <code>dmx.core.aggregation</code> depending on this association definition's type.
     *          Is never <code>null</code>.
     */
    String getInstanceLevelAssocTypeUri();

    // --- Parent Cardinality ---

    String getParentCardinalityUri();

    void setParentCardinalityUri(String parentCardinalityUri);

    // --- Child Cardinality ---

    String getChildCardinalityUri();

    void setChildCardinalityUri(String childCardinalityUri);

    // ---

    ViewConfiguration getViewConfig();

    // ---

    void update(AssociationDefinitionModel model);

    // ---

    AssociationDefinitionModel getModel();
}
