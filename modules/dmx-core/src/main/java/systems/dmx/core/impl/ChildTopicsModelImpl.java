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
     * Key: comp def URI (String), value: RelatedTopicModel or List<RelatedTopicModel>
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
    public final RelatedTopicModelImpl getTopic(String compDefUri) {
        RelatedTopicModelImpl topic = getTopicOrNull(compDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Comp Def URI \"" + compDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topic;
    }

    @Override
    public final RelatedTopicModelImpl getTopicOrNull(String compDefUri) {
        try {
            return (RelatedTopicModelImpl) get(compDefUri);
        } catch (ClassCastException e) {
            throwInvalidSingleAccess(compDefUri, e);
            return null;    // never reached
        }
    }

    @Override
    public final List<RelatedTopicModelImpl> getTopics(String compDefUri) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(compDefUri);
        // error check
        if (topics == null) {
            throw new RuntimeException("Comp Def URI \"" + compDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topics;
    }

    @Override
    public final List<RelatedTopicModelImpl> getTopicsOrNull(String compDefUri) {
        try {
            return (List<RelatedTopicModelImpl>) get(compDefUri);
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(compDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    @Override
    public final Object get(String compDefUri) {
        return childTopics.get(compDefUri);
    }

    @Override
    public boolean has(String compDefUri) {
        return childTopics.containsKey(compDefUri);
    }

    @Override
    public int size() {
        return childTopics.size();
    }



    // === Convenience Accessors ===

    @Override
    public final String getString(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().toString();
    }

    @Override
    public final String getString(String compDefUri, String defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    @Override
    public final int getInt(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().intValue();
    }

    @Override
    public final int getInt(String compDefUri, int defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    @Override
    public final long getLong(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().longValue();
    }

    @Override
    public final long getLong(String compDefUri, long defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    @Override
    public final double getDouble(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().doubleValue();
    }

    @Override
    public final double getDouble(String compDefUri, double defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    @Override
    public final boolean getBoolean(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().booleanValue();
    }

    @Override
    public final boolean getBoolean(String compDefUri, boolean defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    @Override
    public final Object getValue(String compDefUri) {
        return getTopic(compDefUri).getSimpleValue().value();
    }

    @Override
    public final Object getValue(String compDefUri, Object defaultValue) {
        TopicModel topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    @Override
    public final ChildTopicsModelImpl getChildTopics(String compDefUri) {
        return getTopic(compDefUri).childTopics;
    }

    @Override
    public final ChildTopicsModelImpl getChildTopics(String compDefUri, ChildTopicsModel defaultValue) {
        RelatedTopicModelImpl topic = getTopicOrNull(compDefUri);
        return topic != null ? topic.childTopics : (ChildTopicsModelImpl) defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Children ---

    @Override
    public final ChildTopicsModel set(String compDefUri, RelatedTopicModel value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to set null as a ChildTopicsModel value");
            }
            //
            checkValue(compDefUri, value);
            childTopics.put(compDefUri, value);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Setting a ChildTopicsModel value failed, compDefUri=\"" + compDefUri +
                "\", value=" + value, e);
        }
    }

    @Override
    public final ChildTopicsModel set(String compDefUri, TopicModel value) {
        return set(compDefUri, mf.newRelatedTopicModel(value));
    }

    @Override
    public final ChildTopicsModel set(String compDefUri, Object value) {
        try {
            return set(compDefUri, mf.newTopicModel(mf.childTypeUri(compDefUri), new SimpleValue(value)));
        } catch (Exception e) {
            throw new RuntimeException("Setting a ChildTopicsModel value failed, compDefUri=\"" + compDefUri +
                "\", value=" + value, e);
        }
    }

    @Override
    public final ChildTopicsModel set(String compDefUri, ChildTopicsModel value) {
        return set(compDefUri, mf.newTopicModel(mf.childTypeUri(compDefUri), value));
    }

    // ---

    @Override
    public final ChildTopicsModel setRef(String compDefUri, long refTopicId) {
        set(compDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel setRef(String compDefUri, String refTopicUri) {
        set(compDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel setDeletionRef(String compDefUri, long refTopicId) {
        set(compDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel setDeletionRef(String compDefUri, String refTopicUri) {
        set(compDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel remove(String compDefUri) {
        childTopics.remove(compDefUri);    // ### TODO: throw if not in map?
        return this;
    }

    // --- Multiple-valued Children ---

    @Override
    public final ChildTopicsModel add(String compDefUri, RelatedTopicModel value) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(compDefUri);
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(compDefUri, topics);
        }
        //
        checkValue(compDefUri, value);
        topics.add((RelatedTopicModelImpl) value);
        //
        return this;
    }

    @Override
    public final ChildTopicsModel add(String compDefUri, TopicModel value) {
        return add(compDefUri, mf.newRelatedTopicModel(value));
    }

    @Override
    public final ChildTopicsModel add(String compDefUri, Object value) {
        return add(compDefUri, mf.newTopicModel(mf.childTypeUri(compDefUri), new SimpleValue(value)));
    }

    @Override
    public ChildTopicsModel add(String compDefUri, ChildTopicsModel value) {
        return add(compDefUri, mf.newTopicModel(mf.childTypeUri(compDefUri), value));
    }

    @Override
    public final ChildTopicsModel set(String compDefUri, List<RelatedTopicModel> values) {
        // FIXME: call checkValue() on every value
        childTopics.put(compDefUri, values);
        return this;
    }

    @Override
    public final ChildTopicsModel remove(String compDefUri, TopicModel value) {
        List<RelatedTopicModelImpl> topics = getTopicsOrNull(compDefUri);
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel addRef(String compDefUri, long refTopicId) {
        add(compDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel addRef(String compDefUri, String refTopicUri) {
        add(compDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    @Override
    public final ChildTopicsModel addDeletionRef(String compDefUri, long refTopicId) {
        add(compDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    @Override
    public final ChildTopicsModel addDeletionRef(String compDefUri, String refTopicUri) {
        add(compDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this ChildTopicsModel's comp def URIs.
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
            for (String compDefUri : this) {
                Object value = get(compDefUri);
                if (value instanceof RelatedTopicModel) {
                    json.put(compDefUri, ((RelatedTopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(compDefUri, DMXUtils.toJSONArray((List<RelatedTopicModel>) value));
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
    public String toString() {
        return childTopics.toString();
    }

    @Override
    public final ChildTopicsModel clone() {
        ChildTopicsModel clone = mf.newChildTopicsModel();
        for (String compDefUri : this) {
            Object value = get(compDefUri);
            if (value instanceof RelatedTopicModel) {
                RelatedTopicModel model = (RelatedTopicModel) value;
                clone.set(compDefUri, model.clone());
            } else if (value instanceof List) {
                for (RelatedTopicModel model : (List<RelatedTopicModel>) value) {
                    clone.add(compDefUri, model.clone());
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
     * For multiple-valued children: looks in the attached object cache for a child topic by ID. ### FIXDOC
     */
    RelatedTopicModelImpl findChildTopicById(long childTopicId, CompDefModel compDef) {
        List<RelatedTopicModelImpl> childTopics = getTopicsOrNull(compDef.getCompDefUri());
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
     * For multiple-valued children: looks in the attached object cache for the child topic the given reference refers
     * to. ### FIXDOC
     *
     * @param   compDef     the child topics according to this comp def are considered.
     */
    RelatedTopicModelImpl findChildTopicByRef(TopicReferenceModelImpl topicRef, CompDefModel compDef) {
        List<? extends RelatedTopicModel> childTopics = getTopicsOrNull(compDef.getCompDefUri());
        if (childTopics != null) {
            return topicRef.findReferencedTopic(childTopics);
        }
        return null;
    }

    // --- Write Helper ---

    /**
     * For single-valued child
     */
    void putInChildTopics(RelatedTopicModel childTopic, CompDefModel compDef) {
        set(compDef.getCompDefUri(), childTopic);
    }

    /**
     * For single-valued child
     */
    void removeChildTopic(CompDefModel compDef) {
        remove(compDef.getCompDefUri());
    }

    /**
     * For multiple-valued child
     */
    void addToChildTopics(RelatedTopicModel childTopic, CompDefModel compDef) {
        add(compDef.getCompDefUri(), childTopic);
    }

    /**
     * For multiple-valued child
     */
    void removeFromChildTopics(RelatedTopicModel childTopic, CompDefModel compDef) {
        remove(compDef.getCompDefUri(), childTopic);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void throwInvalidSingleAccess(String compDefUri, ClassCastException e) {
        if (e.getMessage().startsWith("java.util.ArrayList cannot be cast to")) {
            throw new RuntimeException("\"" + compDefUri + "\" is accessed as single but is defined as multi", e);
        } else {
            throw new RuntimeException("Accessing \"" + compDefUri + "\" failed", e);
        }
    }

    private void throwInvalidMultiAccess(String compDefUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("\"" + compDefUri + "\" is accessed as multi but is defined as single", e);
        } else {
            throw new RuntimeException("Accessing \"" + compDefUri + " failed", e);
        }
    }

    // ---

    private void checkValue(String compDefUri, RelatedTopicModel value) {
        String childTypeUri = mf.childTypeUri(compDefUri);
        if (value.getTypeUri() == null) {
            value.setTypeUri(childTypeUri);     // convenience: auto-set typeUri
        } else if (!value.getTypeUri().equals(childTypeUri)) {
            throw new IllegalArgumentException("\"" + childTypeUri + "\" value expected, got \"" + value.getTypeUri() +
                "\"");
        }
    }
}
