package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import java.util.Map;
import java.util.Set;



/**
 * Specification of a topic type -- part of DeepaMehta's type system, like a class.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicType {

    long getId();

    String getUri();

    TopicValue getValue();

    // ---

    String getDataTypeUri();

    Set<IndexMode> getIndexModes();

    Map<String, AssociationDefinition> getAssocDefs();

    ViewConfiguration getViewConfig();

    // ---

    AssociationDefinition getAssocDef(String assocDefUri);

    void addAssocDef(AssociationDefinition assocDef);

    // ---

    JSONObject toJSON();
}
