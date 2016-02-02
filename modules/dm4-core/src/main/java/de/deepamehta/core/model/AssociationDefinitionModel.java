package de.deepamehta.core.model;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 * ### FIXDOC: also assoc types have assoc defs
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinitionModel extends AssociationModel {

    String getAssocDefUri();

    RelatedTopicModel getCustomAssocType();

    String getCustomAssocTypeUri();

    /**
     * The type to be used to create an association instance based on this association definition.
     */
    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getParentCardinalityUri();

    String getChildCardinalityUri();

    ViewConfigurationModel getViewConfigModel();

    // ---

    void setParentCardinalityUri(String parentCardinalityUri);

    void setChildCardinalityUri(String childCardinalityUri);

    void setViewConfigModel(ViewConfigurationModel viewConfigModel);

    // ---

    // ### TODO: remove from public API
    boolean hasSameCustomAssocType(AssociationDefinitionModel assocDef);

    /**
     * @return  <code>null</code> if this assoc def's custom assoc type model is null or represents a deletion ref.
     *          Otherwise returns the custom assoc type URI.
     *
     *  ### TODO: remove from public API
     */
    String getCustomAssocTypeUriOrNull();
}
