package systems.dmx.core.impl;

import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.ModelFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * A child topics model that is attached to the DB.
 */
class ChildTopicsImpl implements ChildTopics {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ChildTopicsModelImpl model;     // underlying model

    private DMXObjectModelImpl parent;      // the parent object this ChildTopics belongs to

    private AccessLayer al;
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsImpl(ChildTopicsModelImpl model, DMXObjectModelImpl parent, AccessLayer al) {
        this.model = model;
        this.parent = parent;
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** ChildTopics Implementation ***
    // **********************************



    // === Accessors ===

    @Override
    public RelatedTopic getTopic(String compDefUri) {
        loadChildTopics(compDefUri);
        return _getTopic(compDefUri);
    }

    @Override
    public RelatedTopic getTopicOrNull(String compDefUri) {
        loadChildTopics(compDefUri);
        return _getTopicOrNull(compDefUri);
    }

    @Override
    public List<RelatedTopic> getTopics(String compDefUri) {
        loadChildTopics(compDefUri);
        return _getTopics(compDefUri);
    }

    @Override
    public List<RelatedTopic> getTopicsOrNull(String compDefUri) {
        loadChildTopics(compDefUri);
        return _getTopicsOrNull(compDefUri);
    }

    // ---

    @Override
    public Object get(String compDefUri) {
        Object value = model.get(compDefUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return null;
        }
        // Note: no direct recursion takes place here. Recursion is indirect: attached topics are created here, this
        // implies creating further ChildTopicsImpl objects, which in turn calls this method again but for the next
        // child-level. Finally attached topics are created for all child-levels. ### FIXME
        if (value instanceof RelatedTopicModel) {
            return instantiate((RelatedTopicModel) value);
        } else if (value instanceof List) {
            return instantiate((List<RelatedTopicModel>) value);
        } else {
            throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
        }
    }

    // ---

    @Override
    public ChildTopicsModel getModel() {
        return model;
    }



    // === Convenience Accessors ===

    @Override
    public String getString(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getString(compDefUri);
    }

    @Override
    public String getString(String compDefUri, String defaultValue) {
        loadChildTopics(compDefUri);
        return model.getString(compDefUri, defaultValue);
    }

    @Override
    public int getInt(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getInt(compDefUri);
    }

    @Override
    public int getInt(String compDefUri, int defaultValue) {
        loadChildTopics(compDefUri);
        return model.getInt(compDefUri, defaultValue);
    }

    @Override
    public long getLong(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getLong(compDefUri);
    }

    @Override
    public long getLong(String compDefUri, long defaultValue) {
        loadChildTopics(compDefUri);
        return model.getLong(compDefUri, defaultValue);
    }

    @Override
    public double getDouble(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getDouble(compDefUri);
    }

    @Override
    public double getDouble(String compDefUri, double defaultValue) {
        loadChildTopics(compDefUri);
        return model.getDouble(compDefUri, defaultValue);
    }

    @Override
    public boolean getBoolean(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getBoolean(compDefUri);
    }

    @Override
    public boolean getBoolean(String compDefUri, boolean defaultValue) {
        loadChildTopics(compDefUri);
        return model.getBoolean(compDefUri, defaultValue);
    }

    @Override
    public Object getValue(String compDefUri) {
        loadChildTopics(compDefUri);
        return model.getValue(compDefUri);
    }

    @Override
    public Object getValue(String compDefUri, Object defaultValue) {
        loadChildTopics(compDefUri);
        return model.getValue(compDefUri, defaultValue);
    }

    // ---

    @Override
    public ChildTopics getChildTopics(String compDefUri) {
        loadChildTopics(compDefUri);
        TopicModelImpl topic = model.getTopic(compDefUri);
        return new ChildTopicsImpl(topic.childTopics, topic, al);
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Children ---

    @Override
    public ChildTopics set(String compDefUri, TopicModel value) {
        return _updateOne(compDefUri, mf.newRelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics set(String compDefUri, Object value) {
        return _updateOne(compDefUri, mf.newRelatedTopicModel(mf.childTypeUri(compDefUri), new SimpleValue(value)));
    }

    @Override
    public ChildTopics set(String compDefUri, ChildTopicsModel value) {
        return _updateOne(compDefUri, mf.newRelatedTopicModel(mf.childTypeUri(compDefUri), value));
    }

    // ---

    @Override
    public ChildTopics setRef(String compDefUri, long refTopicId) {
        return _updateOne(compDefUri, mf.newTopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics setRef(String compDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(compDefUri, mf.newTopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics setRef(String compDefUri, String refTopicUri) {
        return _updateOne(compDefUri, mf.newTopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics setRef(String compDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateOne(compDefUri, mf.newTopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics setDeletionRef(String compDefUri, long refTopicId) {
        return _updateOne(compDefUri, mf.newTopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics setDeletionRef(String compDefUri, String refTopicUri) {
        return _updateOne(compDefUri, mf.newTopicDeletionModel(refTopicUri));
    }

    // --- Multiple-valued Children ---

    @Override
    public ChildTopics add(String compDefUri, TopicModel value) {
        return _updateMany(compDefUri, mf.newRelatedTopicModel(value));
    }

    // ---

    @Override
    public ChildTopics add(String compDefUri, Object value) {
        return _updateMany(compDefUri, mf.newRelatedTopicModel(mf.childTypeUri(compDefUri), new SimpleValue(value)));
    }

    @Override
    public ChildTopics add(String compDefUri, ChildTopicsModel value) {
        return _updateMany(compDefUri, mf.newRelatedTopicModel(mf.childTypeUri(compDefUri), value));
    }

    // ---

    @Override
    public ChildTopics addRef(String compDefUri, long refTopicId) {
        return _updateMany(compDefUri, mf.newTopicReferenceModel(refTopicId));
    }

    @Override
    public ChildTopics addRef(String compDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(compDefUri, mf.newTopicReferenceModel(refTopicId, relatingAssocChildTopics));
    }

    @Override
    public ChildTopics addRef(String compDefUri, String refTopicUri) {
        return _updateMany(compDefUri, mf.newTopicReferenceModel(refTopicUri));
    }

    @Override
    public ChildTopics addRef(String compDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics) {
        return _updateMany(compDefUri, mf.newTopicReferenceModel(refTopicUri, relatingAssocChildTopics));
    }

    // ---

    @Override
    public ChildTopics addDeletionRef(String compDefUri, long refTopicId) {
        return _updateMany(compDefUri, mf.newTopicDeletionModel(refTopicId));
    }

    @Override
    public ChildTopics addDeletionRef(String compDefUri, String refTopicUri) {
        return _updateMany(compDefUri, mf.newTopicDeletionModel(refTopicUri));
    }



    // === Iterable Implementation ===

    @Override
    public Iterator<String> iterator() {
        return model.iterator();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    boolean has(String compDefUri) {
        return model.has(compDefUri);
    }

    int size() {
        return model.size();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // Note 1: we need to explicitly declare the arg as RelatedTopicModel. When declared as TopicModel instead the
    // JVM would invoke the ChildTopicsModel's set()/add() which takes a TopicModel object even if at runtime a
    // RelatedTopicModel or even a TopicReferenceModel is passed. This is because Java method overloading involves
    // no dynamic dispatch. See the methodOverloading tests in JavaAPITest.java (in module dmx-test). ### still true?

    // Note 2: calling parent.update(..) would not work. The JVM would call the update() method of the base class
    // (DMXObjectImpl), not the subclass's update() method. This is related to Java's (missing) multiple
    // dispatch. Note that 2 inheritance hierarchies are involved here: the DM object hierarchy and the DM model
    // hierarchy. See the missingMultipleDispatch tests in JavaAPITest.java (in module dmx-test). ### still true?

    private ChildTopics _updateOne(String compDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().set(compDefUri, newChildTopic));
        return this;
    }

    private ChildTopics _updateMany(String compDefUri, RelatedTopicModel newChildTopic) {
        parent.updateChildTopics(mf.newChildTopicsModel().add(compDefUri, newChildTopic));
        return this;
    }

    // ---

    /**
     * Loads the child topics for the given comp def, provided they are not loaded already.
     */
    private void loadChildTopics(String compDefUri) {
        parent.loadChildTopics(compDefUri, false);       // deep=false, FIXME?
    }



    // === Instantiation ===

    private RelatedTopic _getTopic(String compDefUri) {
        return instantiate(model.getTopic(compDefUri));
    }

    private RelatedTopic _getTopicOrNull(String compDefUri) {
        RelatedTopicModel topic = model.getTopicOrNull(compDefUri);
        return topic != null ? instantiate(topic) : null;
    }

    // ---

    private List<RelatedTopic> _getTopics(String compDefUri) {
        return instantiate(model.getTopics(compDefUri));
    }

    private List<RelatedTopic> _getTopicsOrNull(String compDefUri) {
        List<? extends RelatedTopicModel> topics = model.getTopicsOrNull(compDefUri);
        return topics != null ? instantiate(topics) : null;
    }

    // ---

    private List<RelatedTopic> instantiate(List<? extends RelatedTopicModel> models) {
        List<RelatedTopic> topics = new ArrayList();
        for (RelatedTopicModel model : models) {
            topics.add(instantiate(model));
        }
        return topics;
    }

    private RelatedTopic instantiate(RelatedTopicModel model) {
        try {
            return new RelatedTopicImpl((RelatedTopicModelImpl) model, al);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating a RelatedTopic failed (" + model + ")", e);
        }
    }
}
