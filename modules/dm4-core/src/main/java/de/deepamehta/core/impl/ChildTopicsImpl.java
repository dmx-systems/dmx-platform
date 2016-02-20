package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.service.ModelFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A child topics model that is attached to the DB.
 */
class ChildTopicsImpl implements ChildTopics {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ChildTopicsModel model;                             // underlying model

    private DeepaMehtaObjectImpl parent;                        // attached object cache

    /**
     * Attached object cache.
     * Key: assoc def URI (String), value: RelatedTopic or List<RelatedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();    // attached object cache

    private EmbeddedService dms;
    private PersistenceLayer pl;
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsImpl(ChildTopicsModel model, DeepaMehtaObjectImpl parent, EmbeddedService dms) {
        this.model = model;
        this.parent = parent;
        this.dms = dms;
        this.pl = dms.pl;
        this.mf = dms.mf;
        initChildTopics();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** ChildTopics Implementation ***
    // **********************************



    // === Accessors ===

    @Override
    public RelatedTopic getTopic(String assocDefUri) {
        loadChildTopics(assocDefUri);
        return _getTopic(assocDefUri);
    }

    @Override
    public RelatedTopic getTopicOrNull(String assocDefUri) {
        loadChildTopics(assocDefUri);
        return _getTopicOrNull(assocDefUri);
    }

    @Override
    public List<RelatedTopic> getTopics(String assocDefUri) {
        loadChildTopics(assocDefUri);
        return _getTopics(assocDefUri);
    }

    @Override
    public List<RelatedTopic> getTopicsOrNull(String assocDefUri) {
        loadChildTopics(assocDefUri);
        return _getTopicsOrNull(assocDefUri);
    }

    // ---

    @Override
    public Object get(String assocDefUri) {
        return childTopics.get(assocDefUri);
    }

    @Override
    public boolean has(String assocDefUri) {
        return childTopics.containsKey(assocDefUri);
    }

    @Override
    public int size() {
        return childTopics.size();
    }

    // ---

    @Override
    public ChildTopicsModel getModel() {
        return model;
    }



    // === Convenience Accessors ===

    @Override
    public String getString(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().toString();
    }

    @Override
    public String getStringOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().toString() : null;
    }

    @Override
    public int getInt(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().intValue();
    }

    @Override
    public Integer getIntOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().intValue() : null;
    }

    @Override
    public long getLong(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().longValue();
    }

    @Override
    public Long getLongOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().longValue() : null;
    }

    @Override
    public double getDouble(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().doubleValue();
    }

    @Override
    public Double getDoubleOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().doubleValue() : null;
    }

    @Override
    public boolean getBoolean(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().booleanValue();
    }

    @Override
    public Boolean getBooleanOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().booleanValue() : null;
    }

    @Override
    public Object getObject(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().value();
    }

    @Override
    public Object getObjectOrNull(String assocDefUri) {
        Topic topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().value() : null;
    }

    // ---

    @Override
    public ChildTopics getChildTopics(String assocDefUri) {
        return getTopic(assocDefUri).getChildTopics();
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    @Override
    public ChildTopics set(String assocDefUri, TopicModel value) {
        return _updateOne(assocDefUri, mf.newRelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics set(String assocDefUri, Object value) {
        return _updateOne(assocDefUri, mf.newRelatedTopicModel(mf.childTypeUri(assocDefUri), new SimpleValue(value)));
    }

    @Override
    public ChildTopics set(String assocDefUri, ChildTopicsModel value) {
        return _updateOne(assocDefUri, mf.newRelatedTopicModel(mf.childTypeUri(assocDefUri), value));
    }

    // ---

    @Override
    public ChildTopics setRef(String assocDefUri, long refTopicId) {
        return _updateOne(assocDefUri, mf.newTopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics setRef(String assocDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(assocDefUri, mf.newTopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics setRef(String assocDefUri, String refTopicUri) {
        return _updateOne(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics setRef(String assocDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(assocDefUri, mf.newTopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics setDeletionRef(String assocDefUri, long refTopicId) {
        return _updateOne(assocDefUri, mf.newTopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics setDeletionRef(String assocDefUri, String refTopicUri) {
        return _updateOne(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
    }

    // --- Multiple-valued Childs ---

    @Override
    public ChildTopics add(String assocDefUri, TopicModel value) {
        return _updateMany(assocDefUri, mf.newRelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics add(String assocDefUri, Object value) {
        return _updateMany(assocDefUri, mf.newRelatedTopicModel(mf.childTypeUri(assocDefUri), new SimpleValue(value)));
    }

    @Override
    public ChildTopics add(String assocDefUri, ChildTopicsModel value) {
        return _updateMany(assocDefUri, mf.newRelatedTopicModel(mf.childTypeUri(assocDefUri), value));
    }

    // ---

    @Override
    public ChildTopics addRef(String assocDefUri, long refTopicId) {
        return _updateMany(assocDefUri, mf.newTopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics addRef(String assocDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(assocDefUri, mf.newTopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics addRef(String assocDefUri, String refTopicUri) {
        return _updateMany(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics addRef(String assocDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(assocDefUri, mf.newTopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics addDeletionRef(String assocDefUri, long refTopicId) {
        return _updateMany(assocDefUri, mf.newTopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics addDeletionRef(String assocDefUri, String refTopicUri) {
        return _updateMany(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
    }



    // === Iterable Implementation ===

    @Override
    public Iterator<String> iterator() {
        return childTopics.keySet().iterator();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(ChildTopicsModel model) {
        try {
            for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
                String assocDefUri    = assocDef.getAssocDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                RelatedTopicModel newChildTopic        = null;  // only used for "one"
                List<RelatedTopicModel> newChildTopics = null;  // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = model.getTopic(assocDefUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = model.getTopics(assocDefUri, null);      // defaultValue=null
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
            recalculateParentLabel();
            //
        } catch (Exception e) {
            throw new RuntimeException("Updating the child topics of " + parentInfo() + " failed", e);
        }
    }

    // Note: the given association definition must not necessarily originate from the parent object's type definition.
    // It may originate from a facet definition as well.
    // Called from DeepaMehtaObjectImpl.updateChildTopic() and DeepaMehtaObjectImpl.updateChildTopics().
    void updateChildTopics(RelatedTopicModel newChildTopic, List<RelatedTopicModel> newChildTopics,
                                                            AssociationDefinition assocDef) {
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
    void loadChildTopics() {
        for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
            loadChildTopics(assocDef);
        }
    }

    /**
     * Loads the child topics for the given assoc def, provided they are not loaded already.
     */
    void loadChildTopics(String assocDefUri) {
        try {
            loadChildTopics(getAssocDef(assocDefUri));
        } catch (Exception e) {
            throw new RuntimeException("Loading \"" + assocDefUri + "\" child topics of " + parentInfo() + " failed",
                e);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Recursively loads child topics (model) and updates this attached object cache accordingly.
     * If the child topics are loaded already nothing is performed.
     *
     * @param   assocDef    the child topics according to this association definition are loaded.
     *                      <p>
     *                      Note: the association definition must not necessarily originate from the parent object's
     *                      type definition. It may originate from a facet definition as well.
     */
    private void loadChildTopics(AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        if (!has(assocDefUri)) {
            logger.fine("### Lazy-loading \"" + assocDefUri + "\" child topic(s) of " + parentInfo());
            pl.valueStorage.fetchChildTopics(parent.getModel(), assocDef.getModel());
            initChildTopics(assocDefUri);
        }
    }

    private void recalculateParentLabel() {
        List<String> labelAssocDefUris = null;
        try {
            DeepaMehtaObjectModel parent = this.parent.getModel();
            //
            // load required childs
            labelAssocDefUris = pl.valueStorage.getLabelAssocDefUris(parent);
            for (String assocDefUri : labelAssocDefUris) {
                loadChildTopics(assocDefUri);
            }
            //
            pl.valueStorage.recalculateLabel(parent);
        } catch (Exception e) {
            throw new RuntimeException("Recalculating the label of " + parentInfo() +
                " failed (assoc defs involved: " + labelAssocDefUris + ")", e);
        }
    }

    // ---

    // Note 1: we need to explicitly declare the arg as RelatedTopicModel. When declared as TopicModel instead the
    // JVM would invoke the ChildTopicsModel's put()/add() which takes a TopicModel object even if at runtime a
    // RelatedTopicModel or even a TopicReferenceModel is passed. This is because Java method overloading involves
    // no dynamic dispatch. See the methodOverloading tests in JavaAPITest.java (in module dm4-test).

    // Note 2: calling parent.update(..) would not work. The JVM would call the update() method of the base class
    // (DeepaMehtaObjectImpl), not the subclass's update() method. This is related to Java's (missing) multiple
    // dispatch. Note that 2 inheritance hierarchies are involved here: the DM object hierarchy and the DM model
    // hierarchy. See the missingMultipleDispatch tests in JavaAPITest.java (in module dm4-test).

    private ChildTopics _updateOne(String assocDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().put(assocDefUri, newChildTopic));
        return this;
    }

    private ChildTopics _updateMany(String assocDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().add(assocDefUri, newChildTopic));
        return this;
    }



    // === Update Child Topics ===

    // --- Composition ---

    private void updateCompositionOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            deleteChildTopicOne(childTopic, assocDef, true);                                        // deleteChild=true
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModel) newChildTopic, assocDef, true);   // deleteChild=true
        } else if (childTopic != null) {
            updateRelatedTopic(childTopic, newChildTopic);
        } else {
            createChildTopicOne(newChildTopic, assocDef);
        }
    }

    private void updateCompositionMany(List<RelatedTopicModel> newChildTopics, AssociationDefinition assocDef) {
        for (RelatedTopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                deleteChildTopicMany(childTopicId, assocDef, true);                                 // deleteChild=true
            } else if (newChildTopic instanceof TopicReferenceModel) {
                createAssignmentMany((TopicReferenceModel) newChildTopic, assocDef);
            } else if (childTopicId != -1) {
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Aggregation ---

    private void updateAggregationOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            deleteChildTopicOne(childTopic, assocDef, false);                                       // deleteChild=false
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModel) newChildTopic, assocDef, false);  // deleteChild=false
        } else if (newChildTopic.getId() != -1) {
            updateChildTopicOne(newChildTopic, assocDef);
        } else {
            if (childTopic != null) {
                childTopic.getRelatingAssociation().delete();
            }
            createChildTopicOne(newChildTopic, assocDef);
        }
    }

    private void updateAggregationMany(List<RelatedTopicModel> newChildTopics, AssociationDefinition assocDef) {
        for (RelatedTopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                deleteChildTopicMany(childTopicId, assocDef, false);                                // deleteChild=false
            } else if (newChildTopic instanceof TopicReferenceModel) {
                createAssignmentMany((TopicReferenceModel) newChildTopic, assocDef);
            } else if (childTopicId != -1) {
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Update ---

    private void updateChildTopicOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
        //
        if (childTopic == null || childTopic.getId() != newChildTopic.getId()) {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parentInfo() + " according to " + assocDef);
        }
        //
        updateRelatedTopic(childTopic, newChildTopic);
        // Note: memory is already up-to-date. The child topic is updated in-place of parent.
    }

    private void updateChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = findChildTopicById(newChildTopic.getId(), assocDef);
        //
        if (childTopic == null) {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parentInfo() + " according to " + assocDef);
        }
        //
        updateRelatedTopic(childTopic, newChildTopic);
        // Note: memory is already up-to-date. The child topic is updated in-place of parent.
    }

    // ---

    private void updateRelatedTopic(RelatedTopic childTopic, RelatedTopicModel newChildTopic) {
        // update topic
        ((TopicImpl) childTopic)._update(newChildTopic);
        // update association
        updateRelatingAssociation(childTopic, newChildTopic);
    }

    private void updateRelatingAssociation(RelatedTopic childTopic, RelatedTopicModel newChildTopic) {
        childTopic.getRelatingAssociation().update(newChildTopic.getRelatingAssociation());
    }

    // --- Create ---

    private void createChildTopicOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        // update DB
        RelatedTopic childTopic = createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        putInChildTopics(childTopic, assocDef);
    }

    private void createChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        // update DB
        RelatedTopic childTopic = createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        addToChildTopics(childTopic, assocDef);
    }

    // ---

    private RelatedTopic createAndAssociateChildTopic(RelatedTopicModel childTopic, AssociationDefinition assocDef) {
        dms.createTopic(childTopic);
        return associateChildTopic(childTopic, assocDef);
    }

    // --- Assignment ---

    private void createAssignmentOne(RelatedTopic childTopic, TopicReferenceModel newChildTopic,
                                     AssociationDefinition assocDef, boolean deleteChildTopic) {
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
        RelatedTopic topic = resolveRefAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        putInChildTopics(topic, assocDef);
    }

    private void createAssignmentMany(TopicReferenceModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = findChildTopicByRef(newChildTopic, assocDef);
        if (childTopic != null) {
            // Note: "create assignment" is an idempotent operation. A create request for an assignment which
            // exists already is not an error. Instead, nothing is performed.
            updateRelatingAssociation(childTopic, newChildTopic);
            // Note: memory is already up-to-date. The association is updated in-place of parent.
            return;
        }
        // update DB
        RelatedTopic topic = resolveRefAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        addToChildTopics(topic, assocDef);
    }

    // ---

    /**
     * Creates an association between our parent object ("Parent" role) and the referenced topic ("Child" role).
     * The association type is taken from the given association definition.
     *
     * @return  the resolved child topic.
     */
    RelatedTopic resolveRefAndAssociateChildTopic(TopicReferenceModel childTopicRef, AssociationDefinition assocDef) {
        pl.valueStorage.resolveReference(childTopicRef);
        return associateChildTopic(childTopicRef, assocDef);
    }

    private RelatedTopic associateChildTopic(RelatedTopicModel childTopic, AssociationDefinition assocDef) {
        pl.valueStorage.associateChildTopic(parent.getModel(), childTopic, assocDef.getModel());
        return instantiateRelatedTopic(childTopic);
    }

    // --- Delete ---

    private void deleteChildTopicOne(RelatedTopic childTopic, AssociationDefinition assocDef,
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
        removeChildTopic(assocDef);
    }

    private void deleteChildTopicMany(long childTopicId, AssociationDefinition assocDef, boolean deleteChildTopic) {
        RelatedTopic childTopic = findChildTopicById(childTopicId, assocDef);
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
        removeFromChildTopics(childTopic, assocDef);
    }



    // === Attached Object Cache ===

    // --- Access ---

    private RelatedTopic _getTopic(String assocDefUri) {
        RelatedTopic topic = _getTopicOrNull(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet() +
                " (" + parentInfo() + ")");
        }
        //
        return topic;
    }

    private RelatedTopic _getTopicOrNull(String assocDefUri) {
        try {
            return (RelatedTopic) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            ((ChildTopicsModelImpl) getModel()).throwInvalidSingleAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    private List<RelatedTopic> _getTopics(String assocDefUri) {
        List<RelatedTopic> topics = _getTopicsOrNull(assocDefUri);
        // error check
        if (topics == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet() +
                " (" + parentInfo() + ")");
        }
        //
        return topics;
    }

    private List<RelatedTopic> _getTopicsOrNull(String assocDefUri) {
        try {
            return (List<RelatedTopic>) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            ((ChildTopicsModelImpl) getModel()).throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * For multiple-valued childs: looks in the attached object cache for a child topic by ID.
     */
    private RelatedTopic findChildTopicById(long childTopicId, AssociationDefinition assocDef) {
        List<RelatedTopic> childTopics = _getTopicsOrNull(assocDef.getAssocDefUri());
        if (childTopics != null) {
            for (RelatedTopic childTopic : childTopics) {
                if (childTopic.getId() == childTopicId) {
                    return childTopic;
                }
            }
        }
        return null;
    }

    /**
     * For multiple-valued childs: looks in the attached object cache for the child topic the given reference refers to.
     *
     * @param   assocDef    the child topics according to this association definition are considered.
     */
    private RelatedTopic findChildTopicByRef(TopicReferenceModel topicRef, AssociationDefinition assocDef) {
        List<RelatedTopic> childTopics = _getTopicsOrNull(assocDef.getAssocDefUri());
        if (childTopics != null) {
            return topicRef.findReferencedTopic(childTopics);
        }
        return null;
    }

    // ---

    private AssociationDefinition getAssocDef(String assocDefUri) {
        // Note: doesn't work for facets
        return parent.getType().getAssocDef(assocDefUri);
    }

    // --- Update attached object cache + underlying model ---

    /**
     * For single-valued childs
     */
    private void putInChildTopics(RelatedTopic childTopic, AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        put(assocDefUri, childTopic);                               // attached object cache
        getModel().put(assocDefUri, childTopic.getModel());         // underlying model
    }

    /**
     * For single-valued childs
     */
    private void removeChildTopic(AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        remove(assocDefUri);                                        // attached object cache
        getModel().remove(assocDefUri);                             // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void addToChildTopics(RelatedTopic childTopic, AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        add(assocDefUri, childTopic);                               // attached object cache
        getModel().add(assocDefUri, childTopic.getModel());         // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void removeFromChildTopics(Topic childTopic, AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        remove(assocDefUri, childTopic);                            // attached object cache
        getModel().remove(assocDefUri, childTopic.getModel());      // underlying model
    }

    // --- Update attached object cache ---

    /**
     * Puts a single-valued child. An existing value is overwritten.
     */
    private void put(String assocDefUri, Topic topic) {
        childTopics.put(assocDefUri, topic);
    }

    /**
     * Removes a single-valued child.
     */
    private void remove(String assocDefUri) {
        childTopics.remove(assocDefUri);
    }

    /**
     * Adds a value to a multiple-valued child.
     */
    private void add(String assocDefUri, RelatedTopic topic) {
        List<RelatedTopic> topics = _getTopicsOrNull(assocDefUri);
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(assocDefUri, topics);
        }
        topics.add(topic);
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    private void remove(String assocDefUri, Topic topic) {
        List<RelatedTopic> topics = _getTopicsOrNull(assocDefUri);
        if (topics != null) {
            topics.remove(topic);
        }
    }

    // --- Initialization ---

    /**
     * Initializes this attached object cache. Creates a hierarchy of attached topics (recursively) that is isomorph
     * to the underlying model.
     */
    private void initChildTopics() {
        for (String assocDefUri : model) {
            initChildTopics(assocDefUri);
        }
    }

    /**
     * Initializes this attached object cache selectively. Creates a hierarchy of attached topics (recursively) that is
     * isomorph to the underlying model, starting at the given child sub-tree.
     */
    private void initChildTopics(String assocDefUri) {
        Object value = model.get(assocDefUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return;
        }
        // Note: no direct recursion takes place here. Recursion is indirect: attached topics are created here, this
        // implies creating further ChildTopicsImpl objects, which in turn calls this method again but for the next
        // child-level. Finally attached topics are created for all child-levels.
        if (value instanceof RelatedTopicModel) {
            RelatedTopicModel childTopic = (RelatedTopicModel) value;
            childTopics.put(assocDefUri, instantiateRelatedTopic(childTopic));
        } else if (value instanceof List) {
            List<RelatedTopic> topics = new ArrayList();
            childTopics.put(assocDefUri, topics);
            for (RelatedTopicModel childTopic : (List<RelatedTopicModel>) value) {
                topics.add(instantiateRelatedTopic(childTopic));
            }
        } else {
            throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
        }
    }

    /**
     * Creates an attached topic to be put in this attached object cache.
     */
    private RelatedTopic instantiateRelatedTopic(RelatedTopicModel model) {
        try {
            return new RelatedTopicImpl(model, dms);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating a RelatedTopic failed (" + model + ")", e);
        }
    }

    // ---

    private String parentInfo() {
        return parent.className() + " " + parent.getId();
    }
}
