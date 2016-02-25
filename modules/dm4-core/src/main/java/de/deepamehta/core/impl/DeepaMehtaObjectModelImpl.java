package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



class DeepaMehtaObjectModelImpl implements DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

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

    // ---

    protected PersistenceLayer pl;
    protected ModelFactory mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    DeepaMehtaObjectModelImpl(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics,
                                                                                      PersistenceLayer pl) {
        this.id          = id;
        this.uri         = uri;
        this.typeUri     = typeUri;
        this.value       = value;
        this.childTopics = childTopics != null ? childTopics : pl.mf.newChildTopicsModel();
        //
        this.pl          = pl;
        this.mf          = pl.mf;
    }

    DeepaMehtaObjectModelImpl(DeepaMehtaObjectModel object) {
        this(object.getId(), object.getUri(), object.getTypeUri(), object.getSimpleValue(),
            object.getChildTopicsModel(), ((DeepaMehtaObjectModelImpl) object).pl);
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

    // --- misc ---

    @Override
    public void set(DeepaMehtaObjectModel object) {
        setId(object.getId());
        setUri(object.getUri());
        setTypeUri(object.getTypeUri());
        setSimpleValue(object.getSimpleValue());
        setChildTopicsModel(object.getChildTopicsModel());
    }

    // ---

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        throw new RuntimeException("Not implemented");  // only implemented in subclasses
        // Note: technically this class is not abstract. It is instantiated by the ModelFactory.
    }



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



    // ----------------------------------------------------------------------------------------- Package Private Methods

    String className() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaObject instantiate() {
        throw new UnsupportedOperationException();
    }

    // ---

    TypeModel getType() {
        throw new UnsupportedOperationException();
    }

    List<AssociationModel> getAssociations() {
        throw new UnsupportedOperationException();
    }

    // ---

    RelatedTopicModel getRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    ResultList<RelatedTopicModel> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                   String othersRoleTypeUri, String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    ResultList<RelatedTopicModel> getRelatedTopics(List assocTypeUris, String myRoleTypeUri,
                                                   String othersRoleTypeUri, String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    // ---

    void updateUri(String uri) {
        setUri(uri);            // update memory
        storeUri();             // update DB, "abstract"
    }

    void updateTypeUri(String typeUri) {
        setTypeUri(typeUri);    // update memory
        storeTypeUri();         // update DB, "abstract"
    }

    void updateSimpleValue(SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        setSimpleValue(value);  // update memory
        storeSimpleValue();     // update DB, "abstract"
    }

    // ---

    /**
     * Calculates the simple value that is to be indexed for this object.
     *
     * HTML tags are stripped from HTML values. Non-HTML values are returned directly.
     */
    SimpleValue getIndexValue() {
        SimpleValue value = getSimpleValue();
        if (getType().getDataTypeUri().equals("dm4.core.html")) {
            return new SimpleValue(JavaUtils.stripHTML(value.toString()));
        } else {
            return value;
        }
    }

    // ---

    void storeUri() {
        throw new UnsupportedOperationException();
    }

    void storeTypeUri() {
        throw new UnsupportedOperationException();
    }

    /**
     * Stores and indexes the simple value of the specified topic or association model.
     * Determines the index key and index modes.
     */
    void storeSimpleValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Indexes the simple value of the given object model according to the given index mode.
     * <p>
     * Called to index existing topics/associations once an index mode has been added to a type definition.
     */
    void indexSimpleValue(IndexMode indexMode) {
        throw new UnsupportedOperationException();
    }

    // ---

    void delete() {
        pl.deleteObject(this);
    }

    void _delete() {
        throw new UnsupportedOperationException();
    }

    // ---

    DeepaMehtaEvent getPreGetEvent() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaEvent getPreDeleteEvent() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaEvent getPostDeleteEvent() {
        throw new UnsupportedOperationException();
    }

    // ---

    Directive getUpdateDirective() {
        throw new UnsupportedOperationException();
    }

    Directive getDeleteDirective() {
        throw new UnsupportedOperationException();
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
