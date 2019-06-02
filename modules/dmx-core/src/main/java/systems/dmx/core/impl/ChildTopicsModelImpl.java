package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



class ChildTopicsModelImpl implements ChildTopicsModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: assoc def URI (String), value: RelatedTopicModel or List<RelatedTopicModel>
     */
    private Map<String, Object> childTopics;
    // Note: it must be List<RelatedTopicModel>, not Set<RelatedTopicModel>.
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DMXObjectModel.equals()).

    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsModelImpl(Map<String, Object> childTopics, ModelFactory mf) {
        this.childTopics = childTopics;
        this.mf = mf;
    }

    ChildTopicsModelImpl(ChildTopicsModelImpl childTopics) {
        this(childTopics.childTopics, childTopics.mf);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Accessors ===

    @Override
    public final RelatedTopicModelImpl getTopic(String assocDefUri) {
        RelatedTopicModelImpl topic = getTopicOrNull(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topic;
    }

    @Override
    public final RelatedTopicModelImpl getTopicOrNull(String assocDefUri) {
        try {
            return (RelatedTopicModelImpl) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            throwInvalidSingleAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    @Override
    public final List<RelatedTopicModelImpl> getTopics(String assocDefUri) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(assocDefUri);
        // error check
        if (topics == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topics;
    }

    @Override
    public final List<RelatedTopicModelImpl> getTopicsOrNull(String assocDefUri) {
        try {
            return (List<RelatedTopicModelImpl>) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    @Override
    public final Object get(String assocDefUri) {
        return childTopics.get(assocDefUri);
    }



    // === Convenience Accessors ===

    @Override
    public final String getString(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().toString();
    }

    @Override
    public final String getString(String assocDefUri, String defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    @Override
    public final int getInt(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().intValue();
    }

    @Override
    public final int getInt(String assocDefUri, int defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    @Override
    public final long getLong(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().longValue();
    }

    @Override
    public final long getLong(String assocDefUri, long defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    @Override
    public final double getDouble(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().doubleValue();
    }

    @Override
    public final double getDouble(String assocDefUri, double defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    @Override
    public final boolean getBoolean(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().booleanValue();
    }

    @Override
    public final boolean getBoolean(String assocDefUri, boolean defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    @Override
    public final Object getObject(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().value();
    }

    @Override
    public final Object getObject(String assocDefUri, Object defaultValue) {
        TopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    @Override
    public final ChildTopicsModel getChildTopicsModel(String assocDefUri) {
        return getTopic(assocDefUri).getChildTopicsModel();
    }

    @Override
    public final ChildTopicsModel getChildTopicsModel(String assocDefUri, ChildTopicsModel defaultValue) {
        RelatedTopicModel topic = getTopicOrNull(assocDefUri);
        return topic != null ? topic.getChildTopicsModel() : defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    @Override
    public final ChildTopicsModel put(String assocDefUri, RelatedTopicModel value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a ChildTopicsModel");
            }
            //
            childTopics.put(assocDefUri, value);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a ChildTopicsModel failed (assocDefUri=\"" +
                assocDefUri + "\", value=" + value + ")", e);
        }
    }

    @Override
    public final ChildTopicsModel put(String assocDefUri, TopicModel value) {
        return put(assocDefUri, mf.newRelatedTopicModel(value));
    }

    @Override
    public final ChildTopicsModel put(String assocDefUri, Object value) {
        try {
            return put(assocDefUri, mf.newTopicModel(mf.childTypeUri(assocDefUri), new SimpleValue(value)));
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a ChildTopicsModel failed (assocDefUri=\"" +
                assocDefUri + "\", value=" + value + ")", e);
        }
    }

    @Override
    public final ChildTopicsModel put(String assocDefUri, ChildTopicsModel value) {
        return put(assocDefUri, mf.newTopicModel(mf.childTypeUri(assocDefUri), value));
    }

    // ---

    @Override
    public final ChildTopicsModel putRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel putRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel putDeletionRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel putDeletionRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel remove(String assocDefUri) {
        childTopics.remove(assocDefUri);    // ### TODO: throw if not in map?
        return this;
    }

    // --- Multiple-valued Childs ---

    @Override
    public final ChildTopicsModel add(String assocDefUri, RelatedTopicModel value) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(assocDefUri);
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(assocDefUri, topics);
        }
        //
        topics.add((RelatedTopicModelImpl) value);
        //
        return this;
    }

    @Override
    public final ChildTopicsModel add(String assocDefUri, TopicModel value) {
        return add(assocDefUri, mf.newRelatedTopicModel(value));
    }

    @Override
    public final ChildTopicsModel add(String assocDefUri, Object value) {
        return add(assocDefUri, mf.newTopicModel(mf.childTypeUri(assocDefUri), new SimpleValue(value)));
    }

    @Override
    public final ChildTopicsModel put(String assocDefUri, List<RelatedTopicModel> values) {
        childTopics.put(assocDefUri, values);
        return this;
    }

    @Override
    public final ChildTopicsModel remove(String assocDefUri, TopicModel value) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(assocDefUri);
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel addRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel addRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel addDeletionRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel addDeletionRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this ChildTopicsModel's assoc def URIs.
     */
    @Override
    public final Iterator<String> iterator() {
        return childTopics.keySet().iterator();
    }



    // ===

    @Override
    public final JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String assocDefUri : this) {
                Object value = get(assocDefUri);
                if (value instanceof RelatedTopicModel) {
                    json.put(assocDefUri, ((RelatedTopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(assocDefUri, DMXUtils.toJSONArray((List<RelatedTopicModel>) value));
                } else {
                    throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public final ChildTopicsModel clone() {
        ChildTopicsModel clone = mf.newChildTopicsModel();
        for (String assocDefUri : this) {
            Object value = get(assocDefUri);
            if (value instanceof RelatedTopicModel) {
                RelatedTopicModel model = (RelatedTopicModel) value;
                clone.put(assocDefUri, model.clone());
            } else if (value instanceof List) {
                for (RelatedTopicModel model : (List<RelatedTopicModel>) value) {
                    clone.add(assocDefUri, model.clone());
                }
            } else {
                throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
            }
        }
        return clone;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Memory Access ===

    // --- Read ---

    /**
     * For multiple-valued childs: looks in the attached object cache for a child topic by ID. ### FIXDOC
     */
    RelatedTopicModelImpl findChildTopicById(long childTopicId, CompDefModel assocDef) {
        List<RelatedTopicModelImpl> childTopics = getTopicsOrNull(assocDef.getCompDefUri());
        if (childTopics != null) {
            for (RelatedTopicModelImpl childTopic : childTopics) {
                if (childTopic.getId() == childTopicId) {
                    return childTopic;
                }
            }
        }
        return null;
    }

    /**
     * For multiple-valued childs: looks in the attached object cache for the child topic the given reference refers to.
     * ### FIXDOC
     *
     * @param   assocDef    the child topics according to this association definition are considered.
     */
    RelatedTopicModelImpl findChildTopicByRef(TopicReferenceModelImpl topicRef, CompDefModel assocDef) {
        List<? extends RelatedTopicModel> childTopics = getTopicsOrNull(assocDef.getCompDefUri());
        if (childTopics != null) {
            return topicRef.findReferencedTopic(childTopics);
        }
        return null;
    }

    // ---

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    boolean has(String assocDefUri) {
        return childTopics.containsKey(assocDefUri);
    }

    /**
     * Returns the number of childs contained in this ChildTopicsModel.
     * Multiple-valued childs count as one.
     */
    int size() {
        return childTopics.size();
    }

    // --- Write Helper ---

    /**
     * For single-valued childs
     */
    void putInChildTopics(RelatedTopicModel childTopic, CompDefModel assocDef) {
        put(assocDef.getCompDefUri(), childTopic);
    }

    /**
     * For single-valued childs
     */
    void removeChildTopic(CompDefModel assocDef) {
        remove(assocDef.getCompDefUri());
    }

    /**
     * For multiple-valued childs
     */
    void addToChildTopics(RelatedTopicModel childTopic, CompDefModel assocDef) {
        add(assocDef.getCompDefUri(), childTopic);
    }

    /**
     * For multiple-valued childs
     */
    void removeFromChildTopics(RelatedTopicModel childTopic, CompDefModel assocDef) {
        remove(assocDef.getCompDefUri(), childTopic);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void throwInvalidSingleAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().startsWith("java.util.ArrayList cannot be cast to")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as single but is defined as multi", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + "\" failed", e);
        }
    }

    private void throwInvalidMultiAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as multi but is defined as single", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + " failed", e);
        }
    }
}
