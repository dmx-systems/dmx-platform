package systems.dmx.core;

import systems.dmx.core.model.CompDefModel;



/**
 * Definition of an association between 2 topic types -- part of DMX's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions. ### FIXDOC
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface CompDef extends Association {

    String getCompDefUri();

    // ---

    String getParentTypeUri();

    String getChildTypeUri();

    // ---

    /**
     * @return  The custom association type, or <code>null</code> if not set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  The type to be used to create an association instance based on this comp def.
     *          That is the custom association type if set, otherwise <code>dmx.core.composition</code>.
     *          Is never <code>null</code>.
     */
    String getInstanceLevelAssocTypeUri();

    // --- Child Cardinality ---

    String getChildCardinalityUri();

    // TODO: currently not supported
    void setChildCardinalityUri(String childCardinalityUri);

    // ---

    ViewConfiguration getViewConfig();

    // ---

    void update(CompDefModel model);

    // ---

    CompDefModel getModel();
}
