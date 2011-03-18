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

    @Override
    public String toString() {
        return "\n    association definition (assocTypeUri=\"" + assocTypeUri + "\")\n        whole: (type=\"" +
            wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri + "\", cardinality=\"" + wholeCardinalityUri +
            "\")\n        part: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri + "\", cardinality=\"" +
            partCardinalityUri + "\")";
    }
}
