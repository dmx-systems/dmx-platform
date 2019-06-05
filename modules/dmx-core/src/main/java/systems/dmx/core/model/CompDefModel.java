package systems.dmx.core.model;



/**
 * Definition of an association between a parent type and a child type -- part of DMX's type system;
 * like a composition/aggregation in an UML class diagram.
 * <p>
 * The child type is a topic type. The parent type is either a topic type or an association type.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface CompDefModel extends AssociationModel {

    String getCompDefUri();

    /**
     * @return  the URI of the Custom Assoc Type set for this comp def, or <code>null</code> if no
     *          Custom Assoc Type is set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  the type to be used to create an association instance based on this comp def.
     */
    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getChildCardinalityUri();

    ViewConfigurationModel getViewConfig();

    // ---

    // TODO: currently not supported. Drop from public API?
    void setChildCardinalityUri(String childCardinalityUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
