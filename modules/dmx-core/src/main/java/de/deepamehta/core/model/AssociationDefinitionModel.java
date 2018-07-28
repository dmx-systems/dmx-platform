package de.deepamehta.core.model;



/**
 * Definition of an association between a parent type and a child type -- part of DMX's type system;
 * like a composition or an aggregation in an UML class diagram.
 * <p>
 * The child type is a topic type. The parent type is either a topic type or an association type.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinitionModel extends AssociationModel {

    String getAssocDefUri();

    /**
     * @return  the URI of the Custom Association Type set for this association definition,
     *          or <code>null</code> if no Custom Association Type is set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  the type to be used to create an association instance based on this association definition.
     */
    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getParentCardinalityUri();

    String getChildCardinalityUri();

    ViewConfigurationModel getViewConfig();

    // ---

    void setParentCardinalityUri(String parentCardinalityUri);

    void setChildCardinalityUri(String childCardinalityUri);

    void setViewConfig(ViewConfigurationModel viewConfig);
}
