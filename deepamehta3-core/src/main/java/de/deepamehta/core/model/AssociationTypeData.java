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
 * Collection of the data that makes up an {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeData extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationTypeData(String uri, String value) {
        super(uri, new TopicValue(value), "dm3.core.assoc_type");
    }

    public AssociationTypeData(JSONObject assocTypeData) {
        try {
            this.id = -1;
            this.uri = assocTypeData.getString("uri");
            this.value = new TopicValue(assocTypeData.get("value"));
            this.typeUri = "dm3.core.assoc_type";
            this.composite = new Composite();
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationTypeData failed (JSONObject=" + assocTypeData + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "association type data (id=" + id + ", uri=\"" + uri + "\", value=" + value +
            ", typeUri=\"" + typeUri + "\")";
    }
}
