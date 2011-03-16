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

    private String assocTypeUri;

    private String wholeTopicTypeUri;
    private String  partTopicTypeUri;

    private String wholeRoleTypeUri;
    private String  partRoleTypeUri;

    private String wholeCardinalityUri;
    private String  partCardinalityUri;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinition(String assocTypeUri, String wholeTopicTypeUri, String partTopicTypeUri) {
        this.assocTypeUri = assocTypeUri;
        //
        this.wholeTopicTypeUri = wholeTopicTypeUri;
         this.partTopicTypeUri =  partTopicTypeUri;
        // set default role types
        this.wholeRoleTypeUri = wholeTopicTypeUri;
         this.partRoleTypeUri =  partTopicTypeUri;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void setWholeRoleTypeUri(String wholeRoleTypeUri) {
        this.wholeRoleTypeUri = wholeRoleTypeUri;
    }

    public void setPartRoleTypeUri(String partRoleTypeUri) {
        this.partRoleTypeUri = partRoleTypeUri;
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
        return "\n    association type definition (assocTypeUri=\"" + assocTypeUri + "\")\n" +
            "        whole: (type=\"" + wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri + "\", cardinality=\"" + wholeCardinalityUri + "\")\n" +
            "        part: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri + "\", cardinality=\"" + partCardinalityUri + "\")";
    }
}
