package de.deepamehta.core;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface AssociationDefinition {

    long getId();

    String getUri();

    String getAssocTypeUri();

    String getInstanceLevelAssocTypeUri();

    String getWholeTopicTypeUri();

    String getPartTopicTypeUri();

    String getWholeRoleTypeUri();

    String getPartRoleTypeUri();

    String getWholeCardinalityUri();

    String getPartCardinalityUri();

    ViewConfiguration getViewConfig();

    // ---

    void setId(long id);

    void setAssocTypeUri(String assocTypeUri);

    void setWholeCardinalityUri(String wholeCardinalityUri);

    void setPartCardinalityUri(String partCardinalityUri);

    // ---

    JSONObject toJSON();
}
