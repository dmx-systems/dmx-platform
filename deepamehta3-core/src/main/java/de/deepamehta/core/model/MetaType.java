package de.deepamehta.core.model;

import de.deepamehta.core.model.impl.BaseTopic;

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
public class MetaType extends BaseTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public MetaType(long id, String uri, TopicValue value) {
        super(id, uri, value, null, null);  // typeUri=null, composite=null
    }

    public MetaType(JSONObject type) {
        try {
            this.id = -1;
            this.uri = type.getString("uri");
            this.value = new TopicValue(type.get("value"));
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("value", value.value());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    @Override
    public String toString() {
        return "meta type " + id + " \"" + value + "\" (uri=\"" + uri + "\")";
    }
}
