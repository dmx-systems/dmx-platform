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



    // *******************
    // *** ChildTopics ***
    // *******************



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

    @Override
    public boolean has(String compDefUri) {
        return model.has(compDefUri);
    }

    @Override
    public int size() {
        return model.size();
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



    // === Java API ===

    @Override
    public String toString() {
        return model.toString();
    }

    // Iterable
    @Override
    public Iterator<String> iterator() {
        return model.iterator();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

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
