package de.deepamehta.core.model;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Identifiable;
import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



// ### TODO: differentiate between a model and an update model.
// ### The latter's constructor must set no default values (see #311).
public abstract class DeepaMehtaObjectModel implements Identifiable, JSONEnabled, Cloneable {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ### TODO: make these private?
    protected long id;                  // is -1 in models used for a create operation. ### FIXDOC
                                        // is never -1 in models used for an update operation.
    protected String uri;               // is never null in models used for a create operation, may be empty. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected String typeUri;           // is never null in models used for a create operation. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected SimpleValue value;        // is never null in models used for a create operation, may be constructed
                                        //                                                   on empty string. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected ChildTopicsModel composite; // is never null, may be empty. ### FIXDOC

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DeepaMehtaObjectModel(String typeUri) {
        this(-1, typeUri);
    }

    public DeepaMehtaObjectModel(String typeUri, SimpleValue value) {
        this(null, typeUri, value);
    }

    public DeepaMehtaObjectModel(String typeUri, ChildTopicsModel composite) {
        this(null, typeUri, composite);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri) {
        this(-1, uri, typeUri, null, null);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri, SimpleValue value) {
        this(-1, uri, typeUri, value, null);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri, ChildTopicsModel composite) {
        this(-1, uri, typeUri, null, composite);
    }

    public DeepaMehtaObjectModel(long id) {
        this(id, null, null);
    }

    public DeepaMehtaObjectModel(long id, String typeUri) {
        this(id, typeUri, null);
    }

    public DeepaMehtaObjectModel(long id, ChildTopicsModel composite) {
        this(id, null, composite);
    }

    public DeepaMehtaObjectModel(long id, String typeUri, ChildTopicsModel composite) {
        this(id, null, typeUri, null, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK. ### FIXDOC
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null). ### FIXDOC
     * @param   value       If <code>null</code> an empty string value is set. This is OK. ### FIXDOC
     * @param   composite   If <code>null</code> an empty composite is set. This is OK. ### FIXDOC
     */
    public DeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel composite) {
        this.id = id;
        this.uri = uri != null ? uri : "";                          // ### FIXME: don't set default at this level
        this.typeUri = typeUri;
        this.value = value != null ? value : new SimpleValue("");   // ### FIXME: don't set default at this level
        this.composite = composite != null ? composite : new ChildTopicsModel();
    }

    public DeepaMehtaObjectModel(DeepaMehtaObjectModel model) {
        this(model.id, model.uri, model.typeUri, model.value, model.composite);
    }

    /**
     * Used for regular objects: topics and associations.
     */
    public DeepaMehtaObjectModel(JSONObject model) {
        try {
            this.id        = model.optLong("id", -1);
            this.uri       = model.optString("uri", null);
            this.typeUri   = model.optString("type_uri", null);
            this.value     = model.has("value") ? new SimpleValue(model.get("value")) : null;
            this.composite = model.has("composite") ? new ChildTopicsModel(model.getJSONObject("composite"))
                                                    : new ChildTopicsModel();
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + model + ")", e);
        }
    }

    /**
     * Used for types: topic types and association types.
     */
    public DeepaMehtaObjectModel(JSONObject typeModel, String typeUri) {
        try {
            this.id = typeModel.optLong("id", -1);
            this.uri = typeModel.optString("uri");
            this.typeUri = typeUri;
            this.value = new SimpleValue(typeModel.get("value"));
            this.composite = new ChildTopicsModel();
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

    // --- Type URI ---

    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    // --- Simple Value ---

    public SimpleValue getSimpleValue() {
        return value;
    }

    // ---

    public void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(SimpleValue value) {
        this.value = value;
    }

    // --- Composite Value ---

    public ChildTopicsModel getCompositeValue() {
        return composite;
    }

    public void setCompositeValue(ChildTopicsModel comp) {
        this.composite = comp;
    }



    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("type_uri", typeUri);
            o.put("value", value.value());
            o.put("composite", composite.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public DeepaMehtaObjectModel clone() {
        try {
            DeepaMehtaObjectModel model = (DeepaMehtaObjectModel) super.clone();
            model.composite = composite.clone();
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning a DeepaMehtaObjectModel failed", e);
        }
    }

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
        return "id=" + id + ", uri=\"" + uri + "\", typeUri=\"" + typeUri + "\", value=\"" + value +
            "\", composite=" + composite;
    }
}
