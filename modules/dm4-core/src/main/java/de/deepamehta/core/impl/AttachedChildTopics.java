package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TopicRoleModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A child topics model that is attached to the DB.
 */
class AttachedChildTopics implements ChildTopics {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ChildTopicsModel model;                             // underlying model

    private AttachedDeepaMehtaObject parent;                    // attached object cache

    /**
     * Attached object cache.
     * Key: child type URI (String), value: RelatedTopic or List<RelatedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();    // attached object cache

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedChildTopics(ChildTopicsModel model, AttachedDeepaMehtaObject parent, EmbeddedService dms) {
        this.model = model;
        this.parent = parent;
        this.dms = dms;
        initAttachedObjectCache();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** ChildTopics Implementation ***
    // **********************************



    // === Accessors ===

    @Override
    public RelatedTopic getTopic(String childTypeUri) {
        loadChildTopics(childTypeUri);
        return _getTopic(childTypeUri);
    }

    @Override
    public List<RelatedTopic> getTopics(String childTypeUri) {
        loadChildTopics(childTypeUri);
        return _getTopics(childTypeUri);
    }

    // ---

    @Override
    public Object get(String childTypeUri) {
        return childTopics.get(childTypeUri);
    }

    @Override
    public boolean has(String childTypeUri) {
        return childTopics.containsKey(childTypeUri);
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
    public String getString(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().toString();
    }

    @Override
    public int getInt(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().intValue();
    }

    @Override
    public long getLong(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().longValue();
    }

    @Override
    public double getDouble(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().doubleValue();
    }

    @Override
    public boolean getBoolean(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().booleanValue();
    }

    @Override
    public Object getObject(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().value();
    }

    // ---

    @Override
    public ChildTopics getChildTopics(String childTypeUri) {
        return getTopic(childTypeUri).getChildTopics();
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    @Override
    public ChildTopics set(String childTypeUri, TopicModel value) {
        return _updateOne(childTypeUri, new RelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics set(String childTypeUri, Object value) {
        return _updateOne(childTypeUri, new RelatedTopicModel(childTypeUri, new SimpleValue(value)));
    }

    @Override
    public ChildTopics set(String childTypeUri, ChildTopicsModel value) {
        return _updateOne(childTypeUri, new RelatedTopicModel(childTypeUri, value));
    }

    // ---

    @Override
    public ChildTopics setRef(String childTypeUri, long refTopicId) {
        return _updateOne(childTypeUri, new TopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics setRef(String childTypeUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(childTypeUri, new TopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics setRef(String childTypeUri, String refTopicUri) {
        return _updateOne(childTypeUri, new TopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics setRef(String childTypeUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(childTypeUri, new TopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics setDeletionRef(String childTypeUri, long refTopicId) {
        return _updateOne(childTypeUri, new TopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics setDeletionRef(String childTypeUri, String refTopicUri) {
        return _updateOne(childTypeUri, new TopicDeletionModel(refTopicUri));
    }

    // --- Multiple-valued Childs ---

    @Override
    public ChildTopics add(String childTypeUri, TopicModel value) {
        return _updateMany(childTypeUri, new RelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics add(String childTypeUri, Object value) {
        return _updateMany(childTypeUri, new RelatedTopicModel(childTypeUri, new SimpleValue(value)));
    }

    @Override
    public ChildTopics add(String childTypeUri, ChildTopicsModel value) {
        return _updateMany(childTypeUri, new RelatedTopicModel(childTypeUri, value));
    }

    // ---

    @Override
    public ChildTopics addRef(String childTypeUri, long refTopicId) {
        return _updateMany(childTypeUri, new TopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics addRef(String childTypeUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(childTypeUri, new TopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics addRef(String childTypeUri, String refTopicUri) {
        return _updateMany(childTypeUri, new TopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics addRef(String childTypeUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(childTypeUri, new TopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics addDeletionRef(String childTypeUri, long refTopicId) {
        return _updateMany(childTypeUri, new TopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics addDeletionRef(String childTypeUri, String refTopicUri) {
        return _updateMany(childTypeUri, new TopicDeletionModel(refTopicUri));
    }



    // === Iterable Implementation ===

    @Override
    public Iterator<String> iterator() {
        return childTopics.keySet().iterator();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(ChildTopicsModel newComp) {
        try {
            for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
                String childTypeUri   = assocDef.getChildTypeUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                RelatedTopicModel newChildTopic        = null;  // only used for "one"
                List<RelatedTopicModel> newChildTopics = null;  // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newComp.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newComp.getTopics(childTypeUri, null);      // defaultValue=null
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
            dms.valueStorage.refreshLabel(parent.getModel());
            //
        } catch (Exception e) {
            throw new RuntimeException("Updating the child topics of " + parent.className() + " " + parent.getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    // Note: the given association definition must not necessarily originate from the parent object's type definition.
    // It may originate from a facet definition as well.
    // Called from AttachedDeepaMehtaObject.updateChildTopic() and AttachedDeepaMehtaObject.updateChildTopics().
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

    void loadChildTopics() {
        for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
            loadChildTopics(assocDef);
        }
    }

    void loadChildTopics(String childTypeUri) {
        loadChildTopics(getAssocDef(childTypeUri));
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
        String childTypeUri = assocDef.getChildTypeUri();
        if (!has(childTypeUri)) {
            logger.fine("### Lazy-loading \"" + childTypeUri + "\" child topic(s) of " + parent.className() + " " +
                parent.getId());
            dms.valueStorage.fetchChildTopics(parent.getModel(), assocDef.getModel());
            initAttachedObjectCache(childTypeUri);
        }
    }

    // --- Access this attached object cache ---

    private RelatedTopic _getTopic(String childTypeUri) {
        RelatedTopic topic = (RelatedTopic) childTopics.get(childTypeUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Child topic of type \"" + childTypeUri + "\" not found in " + childTopics);
        }
        //
        return topic;
    }

    private RelatedTopic _getTopic(String childTypeUri, RelatedTopic defaultTopic) {
        RelatedTopic topic = (RelatedTopic) childTopics.get(childTypeUri);
        return topic != null ? topic : defaultTopic;
    }

    // ---

    private List<RelatedTopic> _getTopics(String childTypeUri) {
        try {
            List<RelatedTopic> topics = (List<RelatedTopic>) childTopics.get(childTypeUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Child topics of type \"" + childTypeUri + "\" not found in " + childTopics);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    private List<RelatedTopic> _getTopics(String childTypeUri, List<RelatedTopic> defaultValue) {
        try {
            List<RelatedTopic> topics = (List<RelatedTopic>) childTopics.get(childTypeUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    // ---

    // Note 1: we need to explicitly declare the arg as RelatedTopicModel. When declared as TopicModel instead the
    // JVM would invoke the ChildTopicsModel's put()/add() which takes a TopicModel object even if at runtime a
    // RelatedTopicModel or even a TopicReferenceModel is passed. This is because Java method overloading involves
    // no dynamic dispatch. See the methodOverloading tests in JavaAPITest.java (in module dm4-test).

    // Note 2: calling parent.update(..) would not work. The JVM would call the update() method of the base class
    // (AttachedDeepaMehtaObject), not the subclass's update() method. This is related to Java's (missing) multiple
    // dispatch. Note that 2 inheritance hierarchies are involved here: the DM object hierarchy and the DM model
    // hierarchy. See the missingMultipleDispatch tests in JavaAPITest.java (in module dm4-test).

    private ChildTopics _updateOne(String childTypeUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(new ChildTopicsModel().put(childTypeUri, newChildTopic));
        return this;
    }

    private ChildTopics _updateMany(String childTypeUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(new ChildTopicsModel().add(childTypeUri, newChildTopic));
        return this;
    }

    // --- Composition ---

    private void updateCompositionOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopic(assocDef.getChildTypeUri(), null);
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            if (childTopic == null) {
                // Note: "delete child" is an idempotent operation. A delete request for an child which has been
                // deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                return;
            }
            // == delete child ==
            // update DB
            childTopic.delete();
            // update memory
            removeChildTopic(assocDef);
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModel) newChildTopic, assocDef, true);   // deleteChild=true
        } else if (childTopic != null) {
            // == update child ==
            updateChildTopic(childTopic, newChildTopic);
        } else {
            // == create child ==
            createChildTopicOne(newChildTopic, assocDef);
        }
    }

    private void updateCompositionMany(List<RelatedTopicModel> newChildTopics, AssociationDefinition assocDef) {
        for (RelatedTopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                Topic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete child" is an idempotent operation. A delete request for an child which has been
                    // deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete child ==
                // update DB
                childTopic.delete();
                // update memory
                removeFromChildTopics(childTopic, assocDef);
            } else if (newChildTopic instanceof TopicReferenceModel) {
                // == create assignment ==
                if (!createAssignmentMany((TopicReferenceModel) newChildTopic, assocDef)) {
                    continue;
                }
            } else if (childTopicId != -1) {
                // == update child ==
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                // == create child ==
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Aggregation ---

    private void updateAggregationOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopic(assocDef.getChildTypeUri(), null);
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (newChildTopic instanceof TopicDeletionModel) {
            if (childTopic == null) {
                // Note: "delete assignment" is an idempotent operation. A delete request for an assignment which
                // has been deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                return;
            }
            // == delete assignment ==
            // update DB
            childTopic.getRelatingAssociation().delete();
            // update memory
            removeChildTopic(assocDef);
        } else if (newChildTopic instanceof TopicReferenceModel) {
            createAssignmentOne(childTopic, (TopicReferenceModel) newChildTopic, assocDef, false);  // deleteChild=false
        } else if (newChildTopic.getId() != -1) {
            // == update child ==
            updateChildTopicOne(newChildTopic, assocDef);
        } else {
            // == create child ==
            // update DB
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
                RelatedTopic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete assignment" is an idempotent operation. A delete request for an assignment which
                    // has been deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete();
                // update memory
                removeFromChildTopics(childTopic, assocDef);
            } else if (newChildTopic instanceof TopicReferenceModel) {
                // == create assignment ==
                if (!createAssignmentMany((TopicReferenceModel) newChildTopic, assocDef)) {
                    continue;
                }
            } else if (childTopicId != -1) {
                // == update child ==
                updateChildTopicMany(newChildTopic, assocDef);
            } else {
                // == create child ==
                createChildTopicMany(newChildTopic, assocDef);
            }
        }
    }

    // --- Update ---

    private void updateChildTopicOne(TopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = _getTopic(assocDef.getChildTypeUri(), null);
        if (childTopic != null && childTopic.getId() == newChildTopic.getId()) {
            // update DB
            updateChildTopic(childTopic, newChildTopic);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parent.className() + " " + parent.getId() + " according to " + assocDef);
        }
    }

    private void updateChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        RelatedTopic childTopic = findChildTopic(newChildTopic.getId(), assocDef);
        if (childTopic != null) {
            // update DB
            updateChildTopic(childTopic, newChildTopic);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parent.className() + " " + parent.getId() + " according to " + assocDef);
        }
    }

    // ---

    private void updateChildTopic(RelatedTopic childTopic, TopicModel newChildTopic) {
        ((AttachedTopic) childTopic)._update(newChildTopic);
        //
        if (newChildTopic instanceof RelatedTopicModel) {
            childTopic.getRelatingAssociation().update(((RelatedTopicModel) newChildTopic).getRelatingAssociation());
        }
    }

    // --- Create ---

    private void createChildTopicOne(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        // update DB
        RelatedTopic childTopic = createChildTopic(newChildTopic, assocDef);
        // update memory
        putInChildTopics(childTopic, assocDef);
    }

    private void createChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        // update DB
        RelatedTopic childTopic = createChildTopic(newChildTopic, assocDef);
        // update memory
        addToChildTopics(childTopic, assocDef);
    }

    // ---

    private RelatedTopic createChildTopic(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        // create topic
        Topic childTopic = dms.createTopic(newChildTopic);
        // create association
        AssociationModel assoc = newChildTopic.getRelatingAssociation();
        assoc.setTypeUri(assocDef.getInstanceLevelAssocTypeUri());
        assoc.setRoleModel1(parent.getModel().createRoleModel("dm4.core.parent"));
        assoc.setRoleModel2(newChildTopic.createRoleModel("dm4.core.child"));
        dms.createAssociation(assoc);
        //
        return instantiateRelatedTopic(newChildTopic);
    }

    // --- Assignment ---

    private void createAssignmentOne(RelatedTopic childTopic, TopicReferenceModel newChildTopic,
                                     AssociationDefinition assocDef, boolean deleteChildTopic) {
        if (childTopic != null) {
            if (newChildTopic.isReferingTo(childTopic)) {
                return;
            }
            if (deleteChildTopic) {
                // == delete child ==
                // update DB
                childTopic.delete();
            } else {
                // == delete assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete();
            }
        }
        // == create assignment ==
        // update DB
        RelatedTopic topic = associateReferencedChildTopic((TopicReferenceModel) newChildTopic, assocDef);
        // update memory
        putInChildTopics(topic, assocDef);
    }

    private boolean createAssignmentMany(TopicReferenceModel newChildTopic, AssociationDefinition assocDef) {
        if (isReferingToAny(newChildTopic, assocDef)) {
            // Note: "create assignment" is an idempotent operation. A create request for an assignment which
            // exists already is not an error. Instead, nothing is performed.
            return false;
        }
        // update DB
        RelatedTopic topic = associateReferencedChildTopic(newChildTopic, assocDef);
        // update memory
        addToChildTopics(topic, assocDef);
        //
        return true;
    }

    // ---

    /**
     * Creates an association between our parent object ("Parent" role) and the referenced topic ("Child" role).
     * The association type is taken from the given association definition.
     *
     * @return  the resolved child topic.
     */
    RelatedTopic associateReferencedChildTopic(TopicReferenceModel childTopicRef, AssociationDefinition assocDef) {
        if (childTopicRef.isReferenceById()) {
            long childTopicId = childTopicRef.getId();
            // Note: the resolved topic must be fetched including its composite value.
            // It might be required at client-side. ### FIXME: had fetchComposite=true
            Topic childTopic = dms.getTopic(childTopicId);
            Association assoc = associateChildTopic(childTopicId, assocDef);
            return createRelatedTopic(childTopic, assoc);
        } else if (childTopicRef.isReferenceByUri()) {
            String childTopicUri = childTopicRef.getUri();
            // Note: the resolved topic must be fetched including its composite value.
            // It might be required at client-side. ### FIXME: had fetchComposite=true
            Topic childTopic = dms.getTopic("uri", new SimpleValue(childTopicUri));
            Association assoc = associateChildTopic(childTopicUri, assocDef);
            return createRelatedTopic(childTopic, assoc);
        } else {
            throw new RuntimeException("Invalid topic reference (" + childTopicRef + ")");
        }
    }

    private Association associateChildTopic(long childTopicId, AssociationDefinition assocDef) {
        return associateChildTopic(new TopicRoleModel(childTopicId, "dm4.core.child"), assocDef);
    }

    private Association associateChildTopic(String childTopicUri, AssociationDefinition assocDef) {
        return associateChildTopic(new TopicRoleModel(childTopicUri, "dm4.core.child"), assocDef);
    }

    // ---

    private Association associateChildTopic(TopicRoleModel child, AssociationDefinition assocDef) {
        return dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.getModel().createRoleModel("dm4.core.parent"), child);
    }

    private RelatedTopic createRelatedTopic(Topic topic, Association assoc) {
        return new AttachedRelatedTopic(new RelatedTopicModel(topic.getModel(), assoc.getModel()), dms);
    }



    // === Attached Object Cache Initialization ===

    /**
     * Initializes this attached object cache. Creates a hierarchy of attached topics (recursively) that is isomorph
     * to the underlying model.
     */
    private void initAttachedObjectCache() {
        for (String childTypeUri : model) {
            initAttachedObjectCache(childTypeUri);
        }
    }

    /**
     * Initializes this attached object cache selectively. Creates a hierarchy of attached topics (recursively) that is
     * isomorph to the underlying model, starting at the given child sub-tree.
     */
    private void initAttachedObjectCache(String childTypeUri) {
        Object value = model.get(childTypeUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return;
        }
        // Note: no direct recursion takes place here. Recursion is indirect: attached topics are created here, this
        // implies creating further AttachedChildTopics objects, which in turn calls this method again but for the next
        // child-level. Finally attached topics are created for all child-levels.
        if (value instanceof RelatedTopicModel) {
            RelatedTopicModel childTopic = (RelatedTopicModel) value;
            childTopics.put(childTypeUri, instantiateRelatedTopic(childTopic));
        } else if (value instanceof List) {
            List<RelatedTopic> topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
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
        return new AttachedRelatedTopic(model, dms);
    }



    // === Update ===

    // --- Update this attached object cache + underlying model ---

    /**
     * For single-valued childs
     */
    private void putInChildTopics(RelatedTopic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        put(childTypeUri, childTopic);                              // attached object cache
        getModel().put(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For single-valued childs
     */
    private void removeChildTopic(AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        remove(childTypeUri);                                       // attached object cache
        getModel().remove(childTypeUri);                            // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void addToChildTopics(RelatedTopic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        add(childTypeUri, childTopic);                              // attached object cache
        getModel().add(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void removeFromChildTopics(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        remove(childTypeUri, childTopic);                           // attached object cache
        getModel().remove(childTypeUri, childTopic.getModel());     // underlying model
    }

    // --- Update this attached object cache ---

    /**
     * Puts a single-valued child. An existing value is overwritten.
     */
    private void put(String childTypeUri, Topic topic) {
        childTopics.put(childTypeUri, topic);
    }

    /**
     * Removes a single-valued child.
     */
    private void remove(String childTypeUri) {
        childTopics.remove(childTypeUri);
    }

    /**
     * Adds a value to a multiple-valued child.
     */
    private void add(String childTypeUri, RelatedTopic topic) {
        List<RelatedTopic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
        }
        topics.add(topic);
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    private void remove(String childTypeUri, Topic topic) {
        List<RelatedTopic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        if (topics != null) {
            topics.remove(topic);
        }
    }



    // === Helper ===

    /**
     * For multiple-valued childs: looks in the attached object cache for a child topic by ID.
     */
    private RelatedTopic findChildTopic(long childTopicId, AssociationDefinition assocDef) {
        List<RelatedTopic> childTopics = _getTopics(assocDef.getChildTypeUri(), new ArrayList());
        for (RelatedTopic childTopic : childTopics) {
            if (childTopic.getId() == childTopicId) {
                return childTopic;
            }
        }
        return null;
    }

    /**
     * Checks weather the given topic reference refers to any of the child topics.
     *
     * @param   assocDef    the child topics according to this association definition are considered.
     */
    private boolean isReferingToAny(TopicReferenceModel topicRef, AssociationDefinition assocDef) {
        return topicRef.isReferingToAny(_getTopics(assocDef.getChildTypeUri(), new ArrayList()));
    }

    private AssociationDefinition getAssocDef(String childTypeUri) {
        // Note: doesn't work for facets
        return parent.getType().getAssocDef(childTypeUri);
    }
}
