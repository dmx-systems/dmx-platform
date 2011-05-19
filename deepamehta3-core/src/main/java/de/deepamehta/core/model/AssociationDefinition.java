package de.deepamehta.core.model;

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
public class AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long id;                    // ID of the underlying association
    private String uri;                 // not persistent, value is derived from other values, there is no setter
    private String assocTypeUri;

    private String topicTypeUri1;
    private String topicTypeUri2;

    private String roleTypeUri1;        // value might be derived, there is not necessarily such a role type topic
    private String roleTypeUri2;        // value might be derived, there is not necessarily such a role type topic

    private String cardinalityUri1;
    private String cardinalityUri2;

    private ViewConfiguration viewConfig;   // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinition(String topicTypeUri1, String topicTypeUri2) {
        this(-1, topicTypeUri1, topicTypeUri2);
    }

    public AssociationDefinition(long id, String topicTypeUri1, String topicTypeUri2
                                          /* ### String roleTypeUri1, String roleTypeUri2 */) {
        this.id = id;
        //
        this.topicTypeUri1 = topicTypeUri1;
        this.topicTypeUri2 = topicTypeUri2;
        // set default role types
        this.roleTypeUri1 = "dm3.core.whole";       // ### roleTypeUri1 != null ? roleTypeUri1 : topicTypeUri1;
        this.roleTypeUri2 = "dm3.core.part";        // ### roleTypeUri2 != null ? roleTypeUri2 : topicTypeUri2;
        // derive uri
        this.uri = topicTypeUri2;                   // ### roleTypeUri2;
    }

    public AssociationDefinition(JSONObject assocDef, String topicTypeUri1) {
        try {
            this.id = -1;
            //
            this.topicTypeUri1 = topicTypeUri1;
            this.topicTypeUri2 = assocDef.getString("topic_type_uri_2");
            //
            this.roleTypeUri1 = "dm3.core.whole";   // ### assocDef.optString("role_type_uri_1", topicTypeUri1);
            this.roleTypeUri2 = "dm3.core.part";    // ### assocDef.optString("role_type_uri_2", topicTypeUri2);
            //
            this.uri = topicTypeUri2;               // ### roleTypeUri2;
            this.assocTypeUri = assocDef.getString("assoc_type_uri");
            //
            if (!assocDef.has("cardinality_uri_1") && !assocTypeUri.equals("dm3.core.composition")) {
                throw new RuntimeException("\"cardinality_uri_1\" is missing");
            }
            this.cardinalityUri1 = assocDef.optString("cardinality_uri_1", "dm3.core.one");
            this.cardinalityUri2 = assocDef.getString("cardinality_uri_2");
            //
            this.viewConfig = new ViewConfiguration(assocDef);
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinition failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public String getAssocTypeUri() {
        return assocTypeUri;
    }

    public String getTopicTypeUri1() {
        return topicTypeUri1;
    }

    public String getTopicTypeUri2() {
        return topicTypeUri2;
    }

    public String getRoleTypeUri1() {
        return roleTypeUri1;
    }

    public String getRoleTypeUri2() {
        return roleTypeUri2;
    }

    public String getCardinalityUri1() {
        return cardinalityUri1;
    }

    public String getCardinalityUri2() {
        return cardinalityUri2;
    }

    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    public void setId(long id) {
        this.id = id;
    }

    public void setAssocTypeUri(String assocTypeUri) {
        this.assocTypeUri = assocTypeUri;
    }

    public void setCardinalityUri1(String cardinalityUri1) {
        this.cardinalityUri1 = cardinalityUri1;
    }

    public void setCardinalityUri2(String cardinalityUri2) {
        this.cardinalityUri2 = cardinalityUri2;
    }

    public void setViewConfig(ViewConfiguration viewConfig) {
        this.viewConfig = viewConfig;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("assoc_type_uri", assocTypeUri);
            o.put("topic_type_uri_1", topicTypeUri1);
            o.put("topic_type_uri_2", topicTypeUri2);
            o.put("role_type_uri_1", roleTypeUri1);
            o.put("role_type_uri_2", roleTypeUri2);
            o.put("cardinality_uri_1", cardinalityUri1);
            o.put("cardinality_uri_2", cardinalityUri2);
            viewConfig.toJSON(o);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n    association definition (id=" + id + ", uri=\"" + uri + "\", assocTypeUri=\"" + assocTypeUri +
            "\")\n        pos 1: (type=\"" + topicTypeUri1 + "\", role=\"" + roleTypeUri1 +
            "\", cardinality=\"" + cardinalityUri1 +
            "\")\n        pos 2: (type=\"" + topicTypeUri2 + "\", role=\"" + roleTypeUri2 +
            "\", cardinality=\"" + cardinalityUri2 +
            "\")\n        association definition " + viewConfig;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static void toJSON(Collection<AssociationDefinition> assocDefs, JSONObject o) throws Exception {
        List assocDefList = new ArrayList();
        for (AssociationDefinition assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }
}
