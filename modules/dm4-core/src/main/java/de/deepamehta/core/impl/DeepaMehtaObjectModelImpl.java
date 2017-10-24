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
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



class DeepaMehtaObjectModelImpl implements DeepaMehtaObjectModel {

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

    DeepaMehtaObjectModelImpl(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModelImpl childTopics,
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

    DeepaMehtaObjectModelImpl(DeepaMehtaObjectModelImpl object) {
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
            o.put("typeUri", typeUri);
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



    // === Abstract Methods ===

    // ### TODO: make this a real abstract class.
    // Change the model factory in a way it never instantiates DeepaMehtaObjectModels.

    String className() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaObject instantiate() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaObjectModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        throw new UnsupportedOperationException();
    }

    // ---

    TypeModelImpl getType() {
        throw new UnsupportedOperationException();
    }

    List<AssociationModelImpl> getAssociations() {
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

    // ---

    DeepaMehtaEvent getPreUpdateEvent() {
        throw new UnsupportedOperationException();
    }

    DeepaMehtaEvent getPostUpdateEvent() {
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



    // === Core Internal Hooks ===

    void preCreate() {
    }

    void postCreate() {
    }

    // ---

    void preUpdate(DeepaMehtaObjectModel updateModel) {
    }

    void postUpdate(DeepaMehtaObjectModel updateModel, DeepaMehtaObjectModel oldObject) {
    }

    // ---

    void preDelete() {
    }

    void postDelete() {
    }



    // === Update (memory + DB) ===

    final void updateWithChildTopics(ChildTopicsModel childTopics) {
        update(createModelWithChildTopics(childTopics));
    }

    /**
     * @param   updateModel    The data to update.
     *              If the URI is <code>null</code> it is not updated.
     *              If the type URI is <code>null</code> it is not updated.
     *              If the simple value is <code>null</code> it is not updated.
     */
    final void update(DeepaMehtaObjectModelImpl updateModel) {
        try {
            logger.info("Updating " + objectInfo() + " (typeUri=\"" + typeUri + "\")");
            DeepaMehtaObjectModel oldObject = clone();
            em.fireEvent(getPreUpdateEvent(), instantiate(), updateModel);
            //
            preUpdate(updateModel);
            //
            _updateUri(updateModel.getUri());
            _updateTypeUri(updateModel.getTypeUri());
            new ValueIntegrator(pl).integrate(updateModel, this);   // TODO: handle return value
            // TODO: rethink semantics of 1) events, 2) core internal hooks, and 3) directives in the face
            // of DM5 update logic (= unification). Note that update() is not called recursively anmore.
            /* TODO: drop it!
            if (isSimple()) {
                _updateSimpleValue(updateModel.getSimpleValue());
            } else {
                _updateChildTopics(updateModel.getChildTopicsModel());
            } */
            //
            postUpdate(updateModel, oldObject);
            //
            // Note: in case of a type topic the instantiate() call above creates a cloned model
            // that doesn't reflect the update. Here we instantiate the now updated model.
            DeepaMehtaObject object = instantiate();
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
     * Deletes 1) this DeepaMehta object's child topics (recursively) which have an underlying association definition of
     * type "Composition Definition" and 2) deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses. ### FIXDOC
     */
    final void delete() {
        try {
            em.fireEvent(getPreDeleteEvent(), instantiate());
            //
            preDelete();
            //
            // delete child topics (recursively)
            for (AssociationDefinitionModel assocDef : getType().getAssocDefs()) {
                if (assocDef.getTypeUri().equals("dm4.core.composition_def")) {
                    for (TopicModelImpl childTopic : getRelatedTopics(assocDef.getInstanceLevelAssocTypeUri(),
                            "dm4.core.parent", "dm4.core.child", assocDef.getChildTypeUri())) {
                        childTopic.delete();
                    }
                }
            }
            // delete direct associations
            for (AssociationModelImpl assoc : getAssociations()) {
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
            throw new RuntimeException("Deleting " + objectInfo() + " failed (typeUri=\"" + typeUri + "\")", e);
        }
    }



    // === Update Child Topics (memory + DB) ===

    // ### TODO: make this private. See comment in DeepaMehtaObjectImpl.setChildTopics()
    // ### TODO: drop it!
    final void _updateChildTopics(ChildTopicsModelImpl updateModel) {
        try {
            for (AssociationDefinitionModel assocDef : getType().getAssocDefs()) {
                String assocDefUri    = assocDef.getAssocDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                RelatedTopicModelImpl newChildTopic = null;             // only used for "one"
                List<RelatedTopicModelImpl> newChildTopics = null;      // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = updateModel.getTopicOrNull(assocDefUri);
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = updateModel.getTopicsOrNull(assocDefUri);
                    // skip if not contained in update request
                    if (newChildTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                updateChildTopics(newChildTopic, newChildTopics, assocDef);
            }
            //
            _calculateLabelAndUpdate();
            //
        } catch (Exception e) {
            throw new RuntimeException("Updating the child topics of " + objectInfo() + " failed", e);
        }
    }

    // Note: the given association definition must not necessarily originate from the parent object's type definition.
    // It may originate from a facet definition as well.
    // Called from DeepaMehtaObjectImpl.updateChildTopic() and DeepaMehtaObjectImpl.updateChildTopics().
    // ### TODO: make this private? See comments in DeepaMehtaObjectImpl.
    // ### TODO: drop it!
    final void updateChildTopics(RelatedTopicModelImpl newChildTopic, List<RelatedTopicModelImpl> newChildTopics,
                                                                      AssociationDefinitionModel assocDef) {
        // Note: updating the child topics requires them to be loaded
        loadChildTopics(assocDef);
        //
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = newChildTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                updateCompositionOne(newChildTopic, assocDef);
            } else {
                updateCompositionMany(newChildTopics, assocDef);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                updateAggregationOne(newChildTopic, assocDef);
            } else {
                updateAggregationMany(newChildTopics, assocDef);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // ---

    /**
     * Loads the child topics which are not loaded already.
     */
    final DeepaMehtaObjectModel loadChildTopics() {
        for (AssociationDefinitionModel assocDef : getType().getAssocDefs()) {
            loadChildTopics(assocDef);
        }
        return this;
    }

    /**
     * Loads the child topics for the given assoc def, provided they are not loaded already.
     */
    final DeepaMehtaObjectModel loadChildTopics(String assocDefUri) {
        try {
            return loadChildTopics(getAssocDef(assocDefUri));
        } catch (Exception e) {
            throw new RuntimeException("Loading \"" + assocDefUri + "\" child topics of " + objectInfo() + " failed",
                e);
        }
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

    boolean uriChange(String newUri, String compareUri) {
        return newUri != null && !newUri.equals(compareUri);
    }

    boolean isSimple() {
        return !getType().getDataTypeUri().equals("dm4.core.composite");
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

    /**
     * Recursively loads child topics (model) and updates this attached object cache accordingly. ### FIXDOC
     * If the child topics are loaded already nothing is performed.
     *
     * @param   assocDef    the child topics according to this association definition are loaded.
     *                      <p>
     *                      Note: the association definition must not necessarily originate from the parent object's
     *                      type definition. It may originate from a facet definition as well.
     */
    DeepaMehtaObjectModel loadChildTopics(AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        if (!childTopics.has(assocDefUri)) {
            logger.fine("### Lazy-loading \"" + assocDefUri + "\" child topic(s) of " + objectInfo());
            pl.valueStorage.fetchChildTopics(this, assocDef);
        }
        return this;
    }



    // === Update (memory + DB) ===

    private void _updateUri(String newUri) {
        if (uriChange(newUri, uri)) {                               // abort if no update is requested
            logger.info("### Changing URI of " + objectInfo() + " from \"" + uri + "\" -> \"" + newUri + "\"");
            updateUri(newUri);
        }
    }

    private void _updateTypeUri(String newTypeUri) {
        if (newTypeUri != null && !newTypeUri.equals(typeUri)) {    // abort if no update is requested
            logger.info("### Changing type URI of " + objectInfo() + " from \"" + typeUri + "\" -> \"" + newTypeUri +
                "\"");
            updateTypeUri(newTypeUri);
        }
    }

    void _updateSimpleValue(SimpleValue newValue) {
        if (newValue != null && !newValue.equals(value)) {          // abort if no update is requested
            logger.info("### Changing simple value of " + objectInfo() + " from \"" + value + "\" -> \"" + newValue +
                "\"");
            updateSimpleValue(newValue);
        }
    }



    // ### TODO: drop it!
    // === Update Child Topics (memory + DB) ===

    // --- Composition ---

    private void updateCompositionOne(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = childTopics.getTopicOrNull(assocDef.getAssocDefUri());
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            deleteChildTopicOne(childTopic, assocDef, true);                                         // deleteChild=true
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModelImpl) newChildTopic, assocDef, true);// deleteChild=true
        } else if (childTopic != null) {
            updateRelatedTopic(childTopic, newChildTopic);
        } else {
            createChildTopicOne(newChildTopic, assocDef);
        }
    }

    private void updateCompositionMany(List<RelatedTopicModelImpl> newChildTopics,
                                       AssociationDefinitionModel assocDef) {
        for (RelatedTopicModelImpl newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                deleteChildTopicMany(childTopicId, assocDef, true);                                 // deleteChild=true
            } else if (newChildTopic instanceof TopicReferenceModel) {
                createAssignmentMany((TopicReferenceModelImpl) newChildTopic, assocDef);
            } else if (childTopicId != -1) {
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Aggregation ---

    private void updateAggregationOne(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = childTopics.getTopicOrNull(assocDef.getAssocDefUri());
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            deleteChildTopicOne(childTopic, assocDef, false);                                       // deleteChild=false
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModelImpl) newChildTopic, assocDef, false);
        } else if (newChildTopic.getId() != -1) {                                                   // deleteChild=false
            updateChildTopicOne(newChildTopic, assocDef);
        } else {
            if (childTopic != null) {
                childTopic.getRelatingAssociation().delete();
            }
            createChildTopicOne(newChildTopic, assocDef);
        }
    }

    private void updateAggregationMany(List<RelatedTopicModelImpl> newChildTopics,
                                       AssociationDefinitionModel assocDef) {
        for (RelatedTopicModelImpl newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                deleteChildTopicMany(childTopicId, assocDef, false);                                // deleteChild=false
            } else if (newChildTopic instanceof TopicReferenceModel) {
                createAssignmentMany((TopicReferenceModelImpl) newChildTopic, assocDef);
            } else if (childTopicId != -1) {
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Update ---

    private void updateChildTopicOne(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = childTopics.getTopicOrNull(assocDef.getAssocDefUri());
        //
        if (childTopic == null || childTopic.getId() != newChildTopic.getId()) {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " + objectInfo() +
                " according to " + assocDef);
        }
        //
        updateRelatedTopic(childTopic, newChildTopic);
        // Note: memory is already up-to-date. The child topic is updated in-place of parent.
    }

    private void updateChildTopicMany(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = childTopics.findChildTopicById(newChildTopic.getId(), assocDef);
        //
        if (childTopic == null) {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " + objectInfo() +
                " according to " + assocDef);
        }
        //
        updateRelatedTopic(childTopic, newChildTopic);
        // Note: memory is already up-to-date. The child topic is updated in-place of parent.
    }

    // ---

    private void updateRelatedTopic(RelatedTopicModelImpl childTopic, RelatedTopicModelImpl newChildTopic) {
        // update topic
        childTopic.update(newChildTopic);
        // update association
        updateRelatingAssociation(childTopic, newChildTopic);
    }

    private void updateRelatingAssociation(RelatedTopicModelImpl childTopic, RelatedTopicModelImpl newChildTopic) {
        childTopic.getRelatingAssociation().update(newChildTopic.getRelatingAssociation());
    }

    // --- Create ---

    private void createChildTopicOne(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        // update DB
        createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        childTopics.putInChildTopics(newChildTopic, assocDef);
    }

    private void createChildTopicMany(RelatedTopicModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        // update DB
        createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        childTopics.addToChildTopics(newChildTopic, assocDef);
    }

    // ---

    private void createAndAssociateChildTopic(RelatedTopicModelImpl childTopic, AssociationDefinitionModel assocDef) {
        pl.createTopic(childTopic);
        associateChildTopic(childTopic, assocDef);
    }

    // --- Assignment ---

    private void createAssignmentOne(RelatedTopicModelImpl childTopic, TopicReferenceModelImpl newChildTopic,
                                     AssociationDefinitionModel assocDef, boolean deleteChildTopic) {
        if (childTopic != null) {
            if (newChildTopic.isReferingTo(childTopic)) {
                updateRelatingAssociation(childTopic, newChildTopic);
                // Note: memory is already up-to-date. The association is updated in-place of parent.
                return;
            }
            if (deleteChildTopic) {
                childTopic.delete();
            } else {
                childTopic.getRelatingAssociation().delete();
            }
        }
        // update DB
        resolveRefAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        childTopics.putInChildTopics(newChildTopic, assocDef);
    }

    private void createAssignmentMany(TopicReferenceModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = childTopics.findChildTopicByRef(newChildTopic, assocDef);
        if (childTopic != null) {
            // Note: "create assignment" is an idempotent operation. A create request for an assignment which
            // exists already is not an error. Instead, nothing is performed.
            updateRelatingAssociation(childTopic, newChildTopic);
            // Note: memory is already up-to-date. The association is updated in-place of parent.
            return;
        }
        // update DB
        resolveRefAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        childTopics.addToChildTopics(newChildTopic, assocDef);
    }

    // ---

    /**
     * Creates an association between our parent object ("Parent" role) and the referenced topic ("Child" role).
     * The association type is taken from the given association definition.
     *
     * @return  the resolved child topic.
     */
    private void resolveRefAndAssociateChildTopic(TopicReferenceModel childTopicRef,
                                                  AssociationDefinitionModel assocDef) {
        pl.valueStorage.resolveReference(childTopicRef);
        associateChildTopic(childTopicRef, assocDef);
    }

    private void associateChildTopic(RelatedTopicModel childTopic, AssociationDefinitionModel assocDef) {
        pl.valueStorage.associateChildTopic(this, childTopic, assocDef);
    }

    // --- Delete ---

    private void deleteChildTopicOne(RelatedTopicModelImpl childTopic, AssociationDefinitionModel assocDef,
                                                                       boolean deleteChildTopic) {
        if (childTopic == null) {
            // Note: "delete child"/"delete assignment" is an idempotent operation. A delete request for a
            // child/assignment which has been deleted already (resp. is non-existing) is not an error.
            // Instead, nothing is performed.
            return;
        }
        // update DB
        if (deleteChildTopic) {
            childTopic.delete();
        } else {
            childTopic.getRelatingAssociation().delete();
        }
        // update memory
        childTopics.removeChildTopic(assocDef);
    }

    private void deleteChildTopicMany(long childTopicId, AssociationDefinitionModel assocDef,
                                                         boolean deleteChildTopic) {
        RelatedTopicModelImpl childTopic = childTopics.findChildTopicById(childTopicId, assocDef);
        if (childTopic == null) {
            // Note: "delete child"/"delete assignment" is an idempotent operation. A delete request for a
            // child/assignment which has been deleted already (resp. is non-existing) is not an error.
            // Instead, nothing is performed.
            return;
        }
        // update DB
        if (deleteChildTopic) {
            childTopic.delete();
        } else {
            childTopic.getRelatingAssociation().delete();
        }
        // update memory
        childTopics.removeFromChildTopics(childTopic, assocDef);
    }



    // === Label Calculation ===

    private void _calculateLabelAndUpdate() {
        List<String> labelAssocDefUris = null;
        try {
            // load required childs
            labelAssocDefUris = getLabelAssocDefUris();
            for (String assocDefUri : labelAssocDefUris) {
                loadChildTopics(assocDefUri);
            }
            //
            calculateLabelAndUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Calculating and updating label of " + objectInfo() +
                " failed (assoc defs involved: " + labelAssocDefUris + ")", e);
        }
    }

    /**
     * Calculates the label for this object model and updates it in both, memory (in-place), and DB.
     * <p>
     * Prerequisites:
     * 1) this object model is a composite.
     * 2) this object model contains all the child topic models involved in the label calculation.
     *    Note: this method does not load any child topics from DB.
     *
     * ### TODO: make private
     */
    void calculateLabelAndUpdate() {
        try {
            updateSimpleValue(new SimpleValue(calculateLabel()));
        } catch (Exception e) {
            throw new RuntimeException("Calculating and updating label of " + objectInfo() + " failed", e);
        }
    }

    /**
     * Calculates the label for this object model recursively. Recursion ends at a simple object model.
     * <p>
     * Note: called from this class only but can't be private as called on a different object.
     */
    String calculateLabel() {
        TypeModel type = getType();
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            StringBuilder builder = new StringBuilder();
            for (String assocDefUri : getLabelAssocDefUris()) {
                appendLabel(calculateChildLabel(assocDefUri), builder, LABEL_CHILD_SEPARATOR);
            }
            return builder.toString();
        } else {
            return getSimpleValue().toString();
        }
    }

    private String calculateChildLabel(String assocDefUri) {
        Object value = getChildTopicsModel().get(assocDefUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return "";
        }
        //
        if (value instanceof TopicModel) {
            // single value
            return ((TopicModelImpl) value).calculateLabel();                               // recursion
        } else if (value instanceof List) {
            // multiple value
            StringBuilder builder = new StringBuilder();
            for (TopicModelImpl childTopic : (List<TopicModelImpl>) value) {
                appendLabel(childTopic.calculateLabel(), builder, LABEL_TOPIC_SEPARATOR);   // recursion
            }
            return builder.toString();
        } else {
            throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
        }
    }

    private void appendLabel(String label, StringBuilder builder, String separator) {
        // add separator
        if (builder.length() > 0 && label.length() > 0) {
            builder.append(separator);
        }
        //
        builder.append(label);
    }

    /**
     * Prerequisite: this is a composite model.
     */
    private List<String> getLabelAssocDefUris() {
        TypeModelImpl type = getType();
        List<String> labelConfig = type.getLabelConfig();
        if (labelConfig.size() > 0) {
            return labelConfig;
        } else {
            List<String> assocDefUris = new ArrayList();
            Iterator<? extends AssociationDefinitionModel> i = type.getAssocDefs().iterator();
            // Note: types just created might have no child types yet
            if (i.hasNext()) {
                assocDefUris.add(i.next().getAssocDefUri());
            }
            return assocDefUris;
        }
    }



    // === Helper ===

    private AssociationDefinitionModel getAssocDef(String assocDefUri) {
        // Note: doesn't work for facets
        return getType().getAssocDef(assocDefUri);
    }

    String objectInfo() {
        return className() + " " + id;
    }
}
