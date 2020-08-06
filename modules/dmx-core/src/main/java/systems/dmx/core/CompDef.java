package systems.dmx.core;

import systems.dmx.core.model.CompDefModel;



/**
 * A composition definition of a <i>parent type</i> and a <i>child type</i> -- part of DMX's type system.
 * <p>
 * The parent type is either a {@link TopicType} or an {@link AssocType}. The child type is always a {@link TopicType}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface CompDef extends Assoc {

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

    ViewConfig getViewConfig();

    // ---

    void update(CompDefModel model);

    // ---

    CompDefModel getModel();
}
