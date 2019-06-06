package systems.dmx.core.impl;

import systems.dmx.core.DMXObject;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.Directive;
import systems.dmx.core.service.Directives;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;



class DMXObjectModelImpl implements DMXObjectModel {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_CHILD_SEPARATOR = " ";
    private static final String LABEL_TOPIC_SEPARATOR = ", ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long id;                            // is -1 in models used for a create operation. ### FIXDOC
                                        // is never -1 in models used for an update operation.
    String uri;                         // is never null in models used for a create operation, may be empty. ### FIXDOC
                                        // may be null in models used for an update operation.
    String typeUri;                     // is never null in models used for a create operation. ### FIXDOC
                                        // may be null in models used for an update operation.
    SimpleValue value;                  // is never null in models used for a create operation, may be constructed
                                        //                                                   on empty string. ### FIXDOC
                                        // may be null in models used for an update operation.
    ChildTopicsModelImpl childTopics;   // is never null, may be empty. ### FIXDOC

    // ---

    PersistenceLayer pl;
    EventManager em;
    ModelFactoryImpl mf;

    Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    DMXObjectModelImpl(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModelImpl childTopics,
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

    DMXObjectModelImpl(DMXObjectModelImpl object) {
        this(object.getId(), object.getUri(), object.getTypeUri(), object.getSimpleValue(),
            object.getChildTopicsModel(), object.pl);
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
    public ChildTopicsModelImpl getChildTopicsModel() {
        return childTopics;
    }

    @Override
    public void setChildTopicsModel(ChildTopicsModel childTopics) {
        this.childTopics = (ChildTopicsModelImpl) childTopics;
    }

    // --- misc ---

    @Override
    public void set(DMXObjectModel object) {
        setId(object.getId());
        setUri(object.getUri());
        setTypeUri(object.getTypeUri());
        setSimpleValue(object.getSimpleValue());
        setChildTopicsModel(object.getChildTopicsModel());
    }

    // ---

    @Override
    public PlayerModel createRoleModel(String roleTypeUri) {
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
            return new JSONObject()
                .put("id", id)
                .put("uri", uri)
                .put("typeUri", typeUri)
                .put("value", value.value())
                .put("childs", childTopics.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }



    // === Java API ===

    @Override
    public DMXObjectModel clone() {
        try {
            DMXObjectModel object = (DMXObjectModel) super.clone();
            object.setChildTopicsModel(childTopics.clone());
            return object;
        } catch (Exception e) {
            throw new RuntimeException("Cloning a DMXObjectModel failed", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((DMXObjectModel) o).getId() == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + " " + toJSON().toString(4);
        } catch (Exception e) {
            throw new RuntimeException("Prettyprinting failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Abstract Methods ===

    // ### TODO: make this a real abstract class.
    // Change the model factory in a way it never instantiates DMXObjectModels.

    String className() {
        throw new UnsupportedOperationException();
    }

    DMXObject instantiate() {
        throw new UnsupportedOperationException();
    }

    DMXObjectModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        throw new UnsupportedOperationException();
    }

    // ---

    TypeModelImpl getType() {
        throw new UnsupportedOperationException();
    }

    List<AssocModelImpl> getAssocs() {
        throw new UnsupportedOperationException();
    }

    // ---

    RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                     String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    List<RelatedTopicModelImpl> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    List<RelatedTopicModelImpl> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        throw new UnsupportedOperationException();
    }

    // ---

    void storeUri() {
        throw new UnsupportedOperationException();
    }

    void storeTypeUri() {
        throw new UnsupportedOperationException();
    }

    /**
     * Stores and indexes the simple value of this object model.
     */
    void storeSimpleValue() {
        throw new UnsupportedOperationException();
    }

    void storeProperty(String propUri, Object propValue, boolean addToIndex) {
        throw new UnsupportedOperationException();
    }

    void removeProperty(String propUri) {
        throw new UnsupportedOperationException();
    }

    // ---

    void _delete() {
        throw new UnsupportedOperationException();
    }

    // ---

    /**
     * @throws  AccessControlException
     */
    void checkReadAccess() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws  AccessControlException
     */
    void checkWriteAccess() {
        throw new UnsupportedOperationException();
    }

    // ---

    DMXEvent getPreUpdateEvent() {
        throw new UnsupportedOperationException();
    }

    DMXEvent getPostUpdateEvent() {
        throw new UnsupportedOperationException();
    }

    DMXEvent getPreDeleteEvent() {
        throw new UnsupportedOperationException();
    }

    DMXEvent getPostDeleteEvent() {
        throw new UnsupportedOperationException();
    }

    // ---

    Directive getUpdateDirective() {
        throw new UnsupportedOperationException();
    }

    Directive getDeleteDirective() {
        throw new UnsupportedOperationException();
    }



    // === Core Internal Hooks ===

    void preCreate() {
    }

    void postCreate() {
    }

    // ---

    void preUpdate(DMXObjectModel updateModel) {
    }

    void postUpdate(DMXObjectModel updateModel, DMXObjectModel oldObject) {
    }

    // ---

    void preDelete() {
    }

    void postDelete() {
    }



    // === Update (memory + DB) ===

    final void updateChildTopics(ChildTopicsModel childTopics) {
        update(createModelWithChildTopics(childTopics));
    }

    final void updateChildTopics(ChildTopicsModel updateModel, CompDefModel compDef) {
        // ### TODO: think about: no directives are added, no events are fired, no core internal hooks are invoked.
        // Possibly this is not wanted for facet updates. This method is solely used for facet updates.
        // Compare to update() method.
        new ValueIntegrator(pl).integrate(createModelWithChildTopics(updateModel), this, compDef);
    }

    // ---

    /**
     * @param   updateModel    The data to update.
     *              If the URI is <code>null</code> it is not updated.
     *              If the type URI is <code>null</code> it is not updated.
     *              If the simple value is <code>null</code> it is not updated.
     */
    final void update(DMXObjectModelImpl updateModel) {
        try {
            logger.info("Updating " + objectInfo() + " (typeUri=\"" + typeUri + "\")");
            DMXObjectModel oldObject = clone();
            em.fireEvent(getPreUpdateEvent(), instantiate(), updateModel);
            //
            preUpdate(updateModel);
            //
            _updateUri(updateModel.getUri());
            _updateTypeUri(updateModel.getTypeUri());
            new ValueIntegrator(pl).integrate(updateModel, this, null);   // TODO: handle return value
            // TODO: rethink semantics of 1) events, 2) core internal hooks, and 3) directives in the face
            // of DM5 update logic (= unification). Note that update() is not called recursively anmore.
            //
            postUpdate(updateModel, oldObject);
            //
            // Note: in case of a type topic the instantiate() call above creates a cloned model
            // that doesn't reflect the update. Here we instantiate the now updated model.
            DMXObject object = instantiate();
            Directives.get().add(getUpdateDirective(), object);
            em.fireEvent(getPostUpdateEvent(), object, updateModel, oldObject);
        } catch (Exception e) {
            throw new RuntimeException("Updating " + objectInfo() + " failed (typeUri=\"" + typeUri + "\")", e);
        }
    }

    // ---

    final void updateUri(String uri) {
        setUri(uri);            // update memory
        storeUri();             // update DB, "abstract"
    }

    final void updateTypeUri(String typeUri) {
        setTypeUri(typeUri);    // update memory
        storeTypeUri();         // update DB, "abstract"
    }

    final void updateSimpleValue(SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        setSimpleValue(value);  // update memory
        storeSimpleValue();     // update DB, "abstract"
    }



    // === Delete ===

    /**
     * Deletes this object's direct associations, and the object itself.
     */
    final void delete() {
        try {
            if (id == -1) {
                throw new RuntimeException("ID not initialzed in " + this);
            }
            em.fireEvent(getPreDeleteEvent(), instantiate());
            //
            preDelete();
            //
            // delete direct associations
            for (AssocModelImpl assoc : getAssocs()) {
                assoc.delete();
            }
            // delete object itself
            logger.info("Deleting " + objectInfo() + " (typeUri=\"" + typeUri + "\")");
            _delete();
            //
            postDelete();
            //
            Directives.get().add(getDeleteDirective(), this);
            em.fireEvent(getPostDeleteEvent(), this);
        } catch (IllegalStateException e) {
            // Note: getAssocs() might throw IllegalStateException and is no problem.
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
            // Compare to PersistenceLayer.deleteAssoc()
            // TODO: introduce storage-vendor neutral DM exception.
            //
            if (e.getMessage().equals("Node[" + id + "] has been deleted in this tx")) {
                logger.info("### Assoc " + id + " has already been deleted in this transaction. This can happen " +
                    "while deleting a topic with associations A1 and A2 while A2 points to A1 (" + this + ")");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + objectInfo() + " failed (typeUri=\"" + typeUri + "\")", e);
        }
    }



    // === Load Child Topics ===

    // All 3 loadChildTopics() methods use this object itself as a cache.
    // Child topics are fetched from DB only when not fetched already.
    // Caching is done on a per comp def basis.

    /**
     * Recursively loads this object's child topics which are not loaded already.
     */
    final DMXObjectModel loadChildTopics(boolean deep) {
        for (CompDefModel compDef : getType().getCompDefs()) {
            loadChildTopics(compDef, deep);
        }
        return this;
    }

    /**
     * Recursively loads this object's child topics for the given comp def, provided they are not loaded already.
     * If the child topics are loaded already nothing is performed.
     * <p>
     * Implemented on top of {@link #loadChildTopics(CompDefModel, boolean)}.
     * The comp def is get from this object's type definition.
     * <p>
     * Can <i>not</i> be used to load facet values.
     * To load facet values use {@link #loadChildTopics(CompDefModel, boolean)} and pass the facet type's comp def.
     */
    final DMXObjectModel loadChildTopics(String compDefUri, boolean deep) {
        try {
            return loadChildTopics(getCompDef(compDefUri), deep);
        } catch (Exception e) {
            throw new RuntimeException("Loading \"" + compDefUri + "\" child topics of " + objectInfo() + " failed", e);
        }
    }

    /**
     * Recursively loads this object's child topics for the given comp def, provided they are not loaded already.
     * If the child topics are loaded already nothing is performed.
     * <p>
     * Can be used to load facet values.
     *
     * @param   compDef     the child topics according to this comp def are loaded.
     *                      <p>
     *                      Note: the comp def must not necessarily originate from this object's type definition.
     *                      It may originate from a facet type as well.
     */
    final DMXObjectModel loadChildTopics(CompDefModel compDef, boolean deep) {
        String compDefUri = compDef.getCompDefUri();
        if (!childTopics.has(compDefUri)) {
            logger.fine("### Loading \"" + compDefUri + "\" child topics of " + objectInfo());
            new ChildTopicsFetcher(pl).fetch(this, compDef, deep);
        }
        return this;
    }



    // ===

    final boolean uriChange(String newUri, String compareUri) {
        return newUri != null && !newUri.equals(compareUri);
    }

    final boolean isSimple() {
        // TODO: add isSimple() to type model
        String dataTypeUri = getType().getDataTypeUri();
        return dataTypeUri.equals("dmx.core.text")
            || dataTypeUri.equals("dmx.core.html")
            || dataTypeUri.equals("dmx.core.number")
            || dataTypeUri.equals("dmx.core.boolean");
    }

    final boolean isHtml() {
        return getType().getDataTypeUri().equals("dmx.core.html");
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: a principal copy exists in Neo4jStorage.
    // Should this be package private? Should Neo4jStorage have access to the Core's impl package?
    private void setDefaults() {
        if (getUri() == null) {
            setUri("");
        }
        if (getSimpleValue() == null) {
            setSimpleValue("");
        }
    }



    // === Update (memory + DB) ===

    private void _updateUri(String newUri) {
        if (uriChange(newUri, uri)) {                               // abort if no update is requested
            logger.fine("### Changing URI of " + objectInfo() + ": \"" + uri + "\" -> \"" + newUri + "\"");
            updateUri(newUri);
        }
    }

    private void _updateTypeUri(String newTypeUri) {
        if (newTypeUri != null && !newTypeUri.equals(typeUri)) {    // abort if no update is requested
            logger.fine("### Changing type URI of " + objectInfo() + ": \"" + typeUri + "\" -> \"" + newTypeUri +
                "\"");
            updateTypeUri(newTypeUri);
        }
    }

    final void _updateSimpleValue(SimpleValue newValue) {
        if (newValue != null && !newValue.equals(value)) {          // abort if no update is requested
            logger.fine("### Changing simple value of " + objectInfo() + ": \"" + value + "\" -> \"" + newValue +
                "\"");
            updateSimpleValue(newValue);
        }
    }



    // === Helper ===

    // Note: doesn't work for facets
    private CompDefModel getCompDef(String compDefUri) {
        return getType().getCompDef(compDefUri);
    }

    // ### TODO: drop it?
    String objectInfo() {
        return className() + " " + id;
    }
}
