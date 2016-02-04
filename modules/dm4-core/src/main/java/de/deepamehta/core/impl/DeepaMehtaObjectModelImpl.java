package de.deepamehta.core.impl;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.SimpleValue;

import org.codehaus.jettison.json.JSONObject;



abstract class DeepaMehtaObjectModelImpl implements DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ### TODO: make these private
    protected long id;                  // is -1 in models used for a create operation. ### FIXDOC
                                        // is never -1 in models used for an update operation.
    protected String uri;               // is never null in models used for a create operation, may be empty. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected String typeUri;           // is never null in models used for a create operation. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected SimpleValue value;        // is never null in models used for a create operation, may be constructed
                                        //                                                   on empty string. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected ChildTopicsModel childTopics; // is never null, may be empty. ### FIXDOC

    // ---------------------------------------------------------------------------------------------------- Constructors

    DeepaMehtaObjectModelImpl(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics) {
        this.id = id;
        this.uri = uri;
        this.typeUri = typeUri;
        this.value = value;
        this.childTopics = childTopics;
    }

    DeepaMehtaObjectModelImpl(DeepaMehtaObjectModel object) {
        this(
            object.getId(),
            object.getUri(),
            object.getTypeUri(),
            object.getSimpleValue(),
            object.getChildTopicsModel()
        );
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // --- ID ---

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    // --- URI ---

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return typeUri;
    }

    @Override
    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    // --- Simple Value ---

    @Override
    public SimpleValue getSimpleValue() {
        return value;
    }

    // ---

    @Override
    public void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(SimpleValue value) {
        this.value = value;
    }

    // --- Child Topics ---

    @Override
    public ChildTopicsModel getChildTopicsModel() {
        return childTopics;
    }

    @Override
    public void setChildTopicsModel(ChildTopicsModel childTopics) {
        this.childTopics = childTopics;
    }

    // ---

    @Override
    public void set(DeepaMehtaObjectModel object) {
        setId(object.getId());
        setUri(object.getUri());
        setTypeUri(object.getTypeUri());
        setSimpleValue(object.getSimpleValue());
        setChildTopicsModel(object.getChildTopicsModel());
    }

    // ---

    // Note: createRoleModel() remains abstract



    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            // Note: for models used for topic/association enrichment (e.g. timestamps, permissions)
            // default values must be set in case they are not fully initialized.
            setDefaults();
            //
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("type_uri", typeUri);
            o.put("value", value.value());
            o.put("childs", childTopics.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public DeepaMehtaObjectModel clone() {
        try {
            DeepaMehtaObjectModel object = (DeepaMehtaObjectModel) super.clone();
            object.setChildTopicsModel(childTopics.clone());
            return object;
        } catch (Exception e) {
            throw new RuntimeException("Cloning a DeepaMehtaObjectModel failed", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((DeepaMehtaObjectModel) o).getId() == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", typeUri=\"" + typeUri + "\", value=\"" + value +
            "\", childTopics=" + childTopics;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: a principal copy exists in Neo4jStorage.
    // Should this be public? It is not meant to be called by the user.
    private void setDefaults() {
        if (getUri() == null) {
            setUri("");
        }
        if (getSimpleValue() == null) {
            setSimpleValue("");
        }
    }
}
