package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;



class DeepaMehtaObjectModelImpl implements DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long id;                        // is -1 in models used for a create operation. ### FIXDOC
                                    // is never -1 in models used for an update operation.
    String uri;                     // is never null in models used for a create operation, may be empty. ### FIXDOC
                                    // may be null in models used for an update operation.
    String typeUri;                 // is never null in models used for a create operation. ### FIXDOC
                                    // may be null in models used for an update operation.
    SimpleValue value;              // is never null in models used for a create operation, may be constructed
                                    //                                                   on empty string. ### FIXDOC
                                    // may be null in models used for an update operation.
    ChildTopicsModel childTopics;   // is never null, may be empty. ### FIXDOC

    // ---

    PersistenceLayer pl;
    EventManager em;
    ModelFactory mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

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
        this.em          = pl.em;
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



    // === Update ===

    void update(DeepaMehtaObjectModel newModel) {
        // URI
        String newUri = newModel.getUri();
        if (newUri != null && !newUri.equals(uri)) {                // abort if no update is requested
            logger.info("### Changing URI of " + className() + " " + id + " from \"" + uri + "\" -> \"" +
                newUri + "\"");
            updateUri(newUri);
        }
        // type URI
        String newTypeUri = newModel.getTypeUri();
        if (newTypeUri != null && !newTypeUri.equals(typeUri)) {    // abort if no update is requested
            logger.info("### Changing type URI of " + className() + " " + id + " from \"" + typeUri + "\" -> \"" +
                newTypeUri + "\"");
            updateTypeUri(newTypeUri);
        }
        //
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            getChildTopics().update(newModel.getChildTopicsModel());    // ### FIXME
        } else {
            // simple value
            SimpleValue newValue = newModel.getSimpleValue();
            if (newValue != null && !newValue.equals(value)) {      // abort if no update is requested
                logger.info("### Changing simple value of " + className() + " " + id + " from \"" + value + "\" -> \"" +
                    newValue + "\"");
                updateSimpleValue(newValue);
            }
        }
        //
        Directives.get().add(getUpdateDirective(), this);
    }



    // === Delete ===

    /**
     * Deletes 1) this DeepaMehta object's child topics (recursively) which have an underlying association definition of
     * type "Composition Definition" and 2) deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses. ### FIXDOC
     */
    void delete() {
        try {
            em.fireEvent(getPreDeleteEvent(), instantiate());
            //
            // delete child topics (recursively)
            for (AssociationDefinitionModel assocDef : getType().getAssocDefs()) {
                if (assocDef.getTypeUri().equals("dm4.core.composition_def")) {
                    for (TopicModel childTopic : getRelatedTopics(assocDef.getInstanceLevelAssocTypeUri(),
                            "dm4.core.parent", "dm4.core.child", assocDef.getChildTypeUri())) {
                        ((DeepaMehtaObjectModelImpl) childTopic).delete();
                    }
                }
            }
            // delete direct associations
            for (AssociationModel assoc : getAssociations()) {
                ((DeepaMehtaObjectModelImpl) assoc).delete();
            }
            // delete object itself
            logger.info("Deleting " + this);
            Directives.get().add(getDeleteDirective(), this);
            _delete();
            //
            em.fireEvent(getPostDeleteEvent(), this);
        } catch (IllegalStateException e) {
            // Note: getAssociations() might throw IllegalStateException and is no problem.
            // This can happen when this object is an association which is already deleted.
            //
            // Consider this particular situation: let A1 and A2 be associations of this object and let A2 point to A1.
            // If A1 gets deleted first (the association set order is non-deterministic), A2 is implicitely deleted
            // with it (because it is a direct association of A1 as well). Then when the loop comes to A2
            // "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2 has been deleted
            // already. (The Node appearing in the exception is the middle node of A2.) If, on the other hand, A2
            // gets deleted first no error would occur.
            //
            // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext
            // associations is also a part of the topicmap itself. This originates e.g. when the user reveals
            // a topicmap's mapcontext association and then deletes the topicmap.
            //
            if (e.getMessage().equals("Node[" + id + "] has been deleted in this tx")) {
                logger.info("### Association " + id + " has already been deleted in this transaction. This can " +
                    "happen while deleting a topic with associations A1 and A2 while A2 points to A1 (" + this + ")");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + className() + " " + id + " failed (" + this + ")", e);
        }
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
