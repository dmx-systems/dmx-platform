package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationDefinition {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String uri;                 // value might be derived (there's not necessarily a topic with that uri)
    private String assocTypeUri;

    private String wholeTopicTypeUri;
    private String  partTopicTypeUri;

    private String wholeRoleTypeUri;    // value might be derived (there's not necessarily a topic with that uri)
    private String  partRoleTypeUri;    // value might be derived (there's not necessarily a topic with that uri)

    private String wholeCardinalityUri;
    private String  partCardinalityUri;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinition(String wholeTopicTypeUri, String partTopicTypeUri,
                                 String wholeRoleTypeUri,  String partRoleTypeUri) {
        this.wholeTopicTypeUri = wholeTopicTypeUri;
         this.partTopicTypeUri =  partTopicTypeUri;
        // set default role types
        this.wholeRoleTypeUri = wholeRoleTypeUri != null ? wholeRoleTypeUri : wholeTopicTypeUri;
         this.partRoleTypeUri =  partRoleTypeUri != null ? partRoleTypeUri  : partTopicTypeUri;
        // set default uri
        this.uri = this.partRoleTypeUri;
    }

    public AssociationDefinition(JSONObject assocDef, String wholeTopicTypeUri) {
        try {
            this.wholeTopicTypeUri = wholeTopicTypeUri;
             this.partTopicTypeUri = assocDef.getString("part_topic_type_uri");
            //
            this.wholeRoleTypeUri = assocDef.optString("whole_role_type_uri", wholeTopicTypeUri);
             this.partRoleTypeUri = assocDef.optString( "part_role_type_uri",  partTopicTypeUri);
            //
            this.uri = assocDef.optString("uri", partRoleTypeUri);  // FIXME: make uri purely derived (not manual set)?
            this.assocTypeUri = assocDef.getString("assoc_type_uri");
            //
            if (!assocDef.has("whole_cardinality_uri") && !assocTypeUri.equals("dm3.core.composition")) {
                throw new RuntimeException("\"whole_cardinality_uri\" is missing");
            }
            this.wholeCardinalityUri = assocDef.optString("whole_cardinality_uri", "dm3.core.one");
             this.partCardinalityUri = assocDef.getString("part_cardinality_uri");
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinition failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

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

    // ---

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setAssocTypeUri(String assocTypeUri) {
        this.assocTypeUri = assocTypeUri;
    }

    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        this.wholeCardinalityUri = wholeCardinalityUri;
    }

    public void setPartCardinalityUri(String partCardinalityUri) {
        this.partCardinalityUri = partCardinalityUri;
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
            o.put("uri", uri);
            o.put("assoc_type_uri", assocTypeUri);
            o.put("whole_topic_type_uri", wholeTopicTypeUri);
            o.put( "part_topic_type_uri",  partTopicTypeUri);
            o.put("whole_role_type_uri", wholeRoleTypeUri);
            o.put( "part_role_type_uri",  partRoleTypeUri);
            o.put("whole_cardinality_uri", wholeCardinalityUri);
            o.put( "part_cardinality_uri",  partCardinalityUri);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n    association definition (uri=\"" + uri + "\", assocTypeUri=\"" + assocTypeUri + "\")\n        " +
            "whole: (type=\"" + wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri + "\", cardinality=\"" +
            wholeCardinalityUri + "\")\n        part: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri +
            "\", cardinality=\"" + partCardinalityUri + "\")";
    }
}
