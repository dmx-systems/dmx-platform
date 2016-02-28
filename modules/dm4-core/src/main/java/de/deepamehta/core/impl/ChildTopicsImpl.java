package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
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

    private ChildTopicsModelImpl model;                         // underlying model

    private DeepaMehtaObjectImpl parent;                        // attached object cache

    /**
     * Attached object cache.
     * Key: assoc def URI (String), value: RelatedTopic or List<RelatedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();    // attached object cache

    // ### TODO: completely drop all attached object caches. Internal Core operations (e.g. update/delete) must
    // not rely on attached objects. Attached objects embody userland semantics, e.g. access restrictions.
    // Construct attached objects on demand only, that is when passed to the userland.

    private PersistenceLayer pl;
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsImpl(ChildTopicsModelImpl model, DeepaMehtaObjectImpl parent, PersistenceLayer pl) {
        this.model = model;
        this.parent = parent;
        this.pl = pl;
        this.mf = pl.mf;
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
    public ChildTopicsModelImpl getModel() {
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

    // ------------------------------------------------------------------------------------------------- Private Methods

    // Note 1: we need to explicitly declare the arg as RelatedTopicModel. When declared as TopicModel instead the
    // JVM would invoke the ChildTopicsModel's put()/add() which takes a TopicModel object even if at runtime a
    // RelatedTopicModel or even a TopicReferenceModel is passed. This is because Java method overloading involves
    // no dynamic dispatch. See the methodOverloading tests in JavaAPITest.java (in module dm4-test). ### still true?

    // Note 2: calling parent.update(..) would not work. The JVM would call the update() method of the base class
    // (DeepaMehtaObjectImpl), not the subclass's update() method. This is related to Java's (missing) multiple
    // dispatch. Note that 2 inheritance hierarchies are involved here: the DM object hierarchy and the DM model
    // hierarchy. See the missingMultipleDispatch tests in JavaAPITest.java (in module dm4-test). ### still true?

    private ChildTopics _updateOne(String assocDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().put(assocDefUri, newChildTopic));
        return this;
    }

    private ChildTopics _updateMany(String assocDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().add(assocDefUri, newChildTopic));
        return this;
    }

    // ---

    /**
     * Loads the child topics for the given assoc def, provided they are not loaded already.
     */
    private void loadChildTopics(String assocDefUri) {
        parent.loadChildTopics(assocDefUri);
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
            model.throwInvalidSingleAccess(assocDefUri, e);
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
            model.throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // --- Initialization ---

    /**
     * Initializes this attached object cache. Creates a hierarchy of attached topics (recursively) that is isomorph
     * to the underlying model.
     */
    private void initChildTopics() {
        // ### TODO: explain
        if (parent.getUri().equals("dm4.core.meta_meta_type") || parent.getUri().equals("dm4.core.meta_type")) {
            return;
        }
        // Note: we can't just iterate the assoc def URIs contained in the ChildTopicsModel as it may contain
        // syntetic childs like "dm4.time.created". The Relating Association of these is uninitialized and can not
        // be instantiated. That's why we do type-driven iteration here.
        for (AssociationDefinitionModel assocDef : parent.getModel().getType().getAssocDefs()) {
            initChildTopics(assocDef.getAssocDefUri());
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
            return new RelatedTopicImpl((RelatedTopicModelImpl) model, pl);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating a RelatedTopic failed (" + model + ")", e);
        }
    }

    // ---

    private String parentInfo() {
        return parent.className() + " " + parent.getId();
    }
}
