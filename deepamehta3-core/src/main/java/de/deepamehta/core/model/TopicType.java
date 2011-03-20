package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import java.util.Map;
import java.util.Set;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicType {

    long getId();

    String getUri();

    TopicValue getValue();

    // ---

    String getDataTypeUri();

    Map<String, AssociationDefinition> getAssocDefs();

    // ---

    AssociationDefinition getAssociationDefinition(String assocDefUri);

    void addAssociationDefinition(AssociationDefinition assocDef);

    // ---

    JSONObject toJSON();
}
