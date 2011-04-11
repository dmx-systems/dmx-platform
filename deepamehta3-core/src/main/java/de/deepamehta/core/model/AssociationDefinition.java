package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
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

    private long id;                        // ID of the underlying association
    private String uri;                     // not persistent, value is derived from other values, there is no setter
    private String assocTypeUri;

    private String wholeTopicTypeUri;
    private String  partTopicTypeUri;

    private String wholeRoleTypeUri;        // value might be derived, there is not necessarily such a role type topic
    private String  partRoleTypeUri;        // value might be derived, there is not necessarily such a role type topic

    private String wholeCardinalityUri;
    private String  partCardinalityUri;

    private ViewConfiguration viewConfig;   // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinition(long id, String wholeTopicTypeUri, String partTopicTypeUri,
                                          String wholeRoleTypeUri,  String partRoleTypeUri) {
        this.id = id;
        //
        this.wholeTopicTypeUri = wholeTopicTypeUri;
         this.partTopicTypeUri =  partTopicTypeUri;
        // set default role types
        this.wholeRoleTypeUri = wholeRoleTypeUri != null ? wholeRoleTypeUri : wholeTopicTypeUri;
         this.partRoleTypeUri =  partRoleTypeUri != null ? partRoleTypeUri  : partTopicTypeUri;
        // derive uri
        this.uri = this.partRoleTypeUri;
    }

    public AssociationDefinition(JSONObject assocDef, String wholeTopicTypeUri) {
        try {
            this.id = -1;
            //
            this.wholeTopicTypeUri = wholeTopicTypeUri;
             this.partTopicTypeUri = assocDef.getString("part_topic_type_uri");
            //
            this.wholeRoleTypeUri = assocDef.optString("whole_role_type_uri", wholeTopicTypeUri);
             this.partRoleTypeUri = assocDef.optString( "part_role_type_uri",  partTopicTypeUri);
            //
            this.uri = this.partRoleTypeUri;
            this.assocTypeUri = assocDef.getString("assoc_type_uri");
            //
            if (!assocDef.has("whole_cardinality_uri") && !assocTypeUri.equals("dm3.core.composition")) {
                throw new RuntimeException("\"whole_cardinality_uri\" is missing");
            }
            this.wholeCardinalityUri = assocDef.optString("whole_cardinality_uri", "dm3.core.one");
             this.partCardinalityUri = assocDef.getString("part_cardinality_uri");
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

    public String getWholeTopicTypeUri() {
        return wholeTopicTypeUri;
    }

    public String getPartTopicTypeUri() {
        return partTopicTypeUri;
    }

    public String getWholeRoleTypeUri() {
        return wholeRoleTypeUri;
    }

    public String getPartRoleTypeUri() {
        return partRoleTypeUri;
    }

    public String getWholeCardinalityUri() {
        return wholeCardinalityUri;
    }

    public String getPartCardinalityUri() {
        return partCardinalityUri;
    }

    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // ---

    public void setAssocTypeUri(String assocTypeUri) {
        this.assocTypeUri = assocTypeUri;
    }

    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        this.wholeCardinalityUri = wholeCardinalityUri;
    }

    public void setPartCardinalityUri(String partCardinalityUri) {
        this.partCardinalityUri = partCardinalityUri;
    }

    public void setViewConfig(ViewConfiguration viewConfig) {
        this.viewConfig = viewConfig;
    }

    // ---

    public AssociationData toAssociationData() {
        AssociationData assocData = new AssociationData(getAssocTypeUri());
        assocData.addRole(new Role(getWholeTopicTypeUri(),   "dm3.core.whole_topic_type"));
        assocData.addRole(new Role( getPartTopicTypeUri(),   "dm3.core.part_topic_type"));
        assocData.addRole(new Role(getWholeRoleTypeUri(),    "dm3.core.whole_role_type"));
        assocData.addRole(new Role( getPartRoleTypeUri(),    "dm3.core.part_role_type"));
        assocData.addRole(new Role(getWholeCardinalityUri(), "dm3.core.whole_cardinality"));
        assocData.addRole(new Role( getPartCardinalityUri(), "dm3.core.part_cardinality"));
        return assocData;
    }

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("assoc_type_uri", assocTypeUri);
            o.put("whole_topic_type_uri", wholeTopicTypeUri);
            o.put( "part_topic_type_uri",  partTopicTypeUri);
            o.put("whole_role_type_uri", wholeRoleTypeUri);
            o.put( "part_role_type_uri",  partRoleTypeUri);
            o.put("whole_cardinality_uri", wholeCardinalityUri);
            o.put( "part_cardinality_uri",  partCardinalityUri);
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
            "\")\n        whole: (type=\"" + wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri +
            "\", cardinality=\"" + wholeCardinalityUri +
            "\")\n        part: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri +
            "\", cardinality=\"" + partCardinalityUri +
            "\")\n        association definition " + viewConfig;
    }
}
