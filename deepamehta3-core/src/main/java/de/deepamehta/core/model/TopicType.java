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

    // FIXME: we could derive the TopicType interface from the Topic interface. A type IS a topic.
    // We would inherit the Topic's traversal methods, which would help e.g. when fetching the type definition.
    // Furthermore the remaining Topic API delegates in EmbeddedService would vanish.

    long getId();

    String getUri();

    TopicValue getValue();

    // ---

    String getDataTypeUri();

    // ---

    Set<IndexMode> getIndexModes();

    void setIndexModes(Set<IndexMode> indexModes);

    // ---

    Map<String, AssociationDefinition> getAssocDefs();

    AssociationDefinition getAssocDef(String assocDefUri);

    void addAssocDef(AssociationDefinition assocDef);

    // ---

    ViewConfiguration getViewConfig();

    Object getViewConfig(String typeUri, String settingUri);

    // ---

    JSONObject toJSON();
}
