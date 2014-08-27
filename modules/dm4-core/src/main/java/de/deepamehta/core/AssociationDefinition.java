package de.deepamehta.core;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Directives;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinition extends Association {

    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getParentCardinalityUri();

    String getChildCardinalityUri();

    ViewConfiguration getViewConfig();

    // ---

    AssociationDefinitionModel getModel();

    // ---

    void setParentCardinalityUri(String parentCardinalityUri, Directives directives);

    void setChildCardinalityUri(String childCardinalityUri, Directives directives);

    // === Updating ===

    void update(AssociationDefinitionModel model, Directives directives);
}
