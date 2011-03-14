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
 * A topic type. Part of the meta-model (like a class).
 * <p>
 * A topic type is an ordered collection of {@link DataField}s.
 * A topic type itself is a {@link Topic}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationType extends Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationType(Topic topic) {
        super(topic);
    }

    public AssociationType(JSONObject type) {
        try {
            this.id = -1;
            this.uri = type.getString("uri");
            this.value = new TopicValue(type.get("value"));
            this.typeUri = "dm3.core.assoc_type";
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "assocication type " + id + " \"" + value + "\" (uri=\"" + uri + "\", typeUri=\"" + typeUri + "\")";
    }
}
