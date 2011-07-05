package de.deepamehta.core.model;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



public abstract class DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long id;
    protected String uri;           // is never null, may be empty
    protected TopicValue value;     // is never null, may be constructed on empty string
    protected String typeUri;       // is never null in models used for a create operation
                                    // may be null in models used for an update operation
    protected Composite composite;  // is never null, may be empty

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DeepaMehtaObjectModel(String typeUri) {
        this(typeUri, null);
    }

    public DeepaMehtaObjectModel(String typeUri, Composite composite) {
        this(null, null, typeUri, composite);
    }

    public DeepaMehtaObjectModel(String uri, TopicValue value, String typeUri) {
        this(uri, value, typeUri, null);
    }

    public DeepaMehtaObjectModel(String uri, TopicValue value, String typeUri, Composite composite) {
        this(-1, uri, value, typeUri, composite);
    }

    public DeepaMehtaObjectModel(long id, String typeUri) {
        this(id, null, null, typeUri, null);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   composite   If <code>null</code> an empty composite is set. This is OK.
     */
    public DeepaMehtaObjectModel(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        this.id = id;
        this.uri = uri != null ? uri : "";
        this.value = value != null ? value : new TopicValue("");
        this.typeUri = typeUri;
        this.composite = composite != null ? composite : new Composite();
    }

    public DeepaMehtaObjectModel(DeepaMehtaObjectModel model) {
        this(model.id, model.uri, model.value, model.typeUri, model.composite);
    }

    public DeepaMehtaObjectModel(JSONObject model) {
        try {
            this.id = model.optLong("id", -1);
            this.uri = model.optString("uri");
            this.value = new TopicValue(model.optString("value"));
            this.typeUri = model.optString("type_uri", null);
            if (model.has("composite")) {
                this.composite = new Composite(model.getJSONObject("composite"));
            } else {
                this.composite = new Composite();
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + model + ")", e);
        }
    }

    public DeepaMehtaObjectModel(JSONObject typeModel, String typeUri) {
        try {
            this.id = typeModel.optLong("id", -1);
            this.uri = typeModel.getString("uri");
            this.value = new TopicValue(typeModel.get("value"));
            this.typeUri = typeUri;
            this.composite = new Composite();
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + typeModel +
                ", typeUri=\"" + typeUri + "\")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // --- ID ---

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // --- URI ---

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    // --- Value ---

    public TopicValue getValue() {
        return value;
    }

    // ---

    public void setValue(String value) {
        setValue(new TopicValue(value));
    }

    public void setValue(int value) {
        setValue(new TopicValue(value));
    }

    public void setValue(long value) {
        setValue(new TopicValue(value));
    }

    public void setValue(boolean value) {
        setValue(new TopicValue(value));
    }

    public void setValue(TopicValue value) {
        this.value = value;
    }

    // --- Type URI ---

    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    // --- Composite ---

    public Composite getComposite() {
        return composite;
    }

    public void setComposite(Composite comp) {
        this.composite = comp;
    }



    // === Serialization ===

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("value", value.value());
            o.put("type_uri", typeUri);
            o.put("composite", composite.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    public static JSONArray objectsToJSON(Collection<JSONEnabled> objects) {
        JSONArray array = new JSONArray();
        for (JSONEnabled object : objects) {
            array.put(object.toJSON());
        }
        return array;
    }



    // === Java API ===

    @Override
    public boolean equals(Object o) {
        return ((DeepaMehtaObjectModel) o).id == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", composite=" + composite;
    }
}
