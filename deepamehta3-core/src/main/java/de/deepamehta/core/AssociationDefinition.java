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

    String getTopicTypeUri1();

    String getTopicTypeUri2();

    String getRoleTypeUri1();

    String getRoleTypeUri2();

    String getCardinalityUri1();

    String getCardinalityUri2();

    ViewConfiguration getViewConfig();

    // ---

    void setId(long id);

    void setAssocTypeUri(String assocTypeUri);

    void setCardinalityUri1(String cardinalityUri1);

    void setCardinalityUri2(String cardinalityUri2);

    // ---

    JSONObject toJSON();
}
