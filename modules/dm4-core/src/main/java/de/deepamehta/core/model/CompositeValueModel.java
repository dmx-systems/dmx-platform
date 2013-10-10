package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValueModel</code>. ### FIXDOC
 */
public class CompositeValueModel {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_PREFIX = "del_id:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: TopicModel or List<TopicModel>
     */
    private Map<String, Object> values = new HashMap();
    // Note: it must be List<TopicModel>, not Set<TopicModel> (like before).
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DeepaMehtaObjectModel.equals()).

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValueModel() {
    }

    public CompositeValueModel(JSONObject values) {
        try {
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String childTypeUri = i.next();
                Object value = values.get(childTypeUri);
                if (value instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) value;
                    for (int j = 0; j < valueArray.length(); j++) {
                        add(childTypeUri, createTopicModel(childTypeUri, valueArray.get(j)));
                    }
                } else {
                    put(childTypeUri, createTopicModel(childTypeUri, value));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing CompositeValueModel failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    public TopicModel getTopic(String childTypeUri) {
        TopicModel topic = (TopicModel) get(childTypeUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + childTypeUri +
                "\": no such entry in\n" + this);
        }
        //
        return topic;
    }

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child.
     */
    public TopicModel getTopic(String childTypeUri, TopicModel defaultValue) {
        TopicModel topic = (TopicModel) get(childTypeUri);
        return topic != null ? topic : defaultValue;
    }

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    public List<TopicModel> getTopics(String childTypeUri) {
        try {
            List<TopicModel> topics = (List<TopicModel>) get(childTypeUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + childTypeUri +
                    "\": no such entry in\n" + this);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child.
     */
    public List<TopicModel> getTopics(String childTypeUri, List<TopicModel> defaultValue) {
        try {
            List<TopicModel> topics = (List<TopicModel>) get(childTypeUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * Accesses a child generically, regardless of single-valued or multiple-valued.
     * Returns null if there is no such child.
     *
     * @return  A TopicModel or List<TopicModel>, or null if there is no such child.
     */
    public Object get(String childTypeUri) {
        return values.get(childTypeUri);
    }

    /**
     * Returns the type URIs of all childs directly contained in this composite value.
     * ### TODO: could be renamed to "getChildTypeURIs()"
     */
    public Iterable<String> keys() {
        return values.keySet();
    }

    /**
     * Checks if a child is directly contained in this composite value.
     * ### TODO: could be renamed to "contains()"
     */
    public boolean has(String childTypeUri) {
        return values.containsKey(childTypeUri);
    }

    /**
     * Returns the number of childs directly contained in this composite value.
     * Multiple-valued childs count as one.
     */
    public int size() {
        return values.size();
    }



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public String getString(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().toString();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public String getString(String childTypeUri, String defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public int getInt(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().intValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public int getInt(String childTypeUri, int defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public long getLong(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().longValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public long getLong(String childTypeUri, long defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public double getDouble(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().doubleValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public double getDouble(String childTypeUri, double defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public boolean getBoolean(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().booleanValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public boolean getBoolean(String childTypeUri, boolean defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public Object getObject(String childTypeUri) {
        return getTopic(childTypeUri).getSimpleValue().value();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public Object getObject(String childTypeUri, Object defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public CompositeValueModel getCompositeValueModel(String childTypeUri) {
        return getTopic(childTypeUri).getCompositeValueModel();
    }

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public CompositeValueModel getCompositeValueModel(String childTypeUri, CompositeValueModel defaultValue) {
        TopicModel topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getCompositeValueModel() : defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    /**
     * Puts a value in a single-valued child.
     * An existing value is overwritten.
     */
    public CompositeValueModel put(String childTypeUri, TopicModel value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValueModel");
            }
            //
            values.put(childTypeUri, value);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValueModel failed (childTypeUri=\"" +
                childTypeUri + "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    /**
     * Convenience method to put a *simple* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @param   value   a String, Integer, Long, Double, or a Boolean.
     *
     * @return  this CompositeValueModel.
     */
    public CompositeValueModel put(String childTypeUri, Object value) {
        try {
            values.put(childTypeUri, new TopicModel(childTypeUri, new SimpleValue(value)));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValueModel failed (childTypeUri=\"" +
                childTypeUri + "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    /**
     * Convenience method to put a *composite* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @return  this CompositeValueModel.
     */
    public CompositeValueModel put(String childTypeUri, CompositeValueModel value) {
        values.put(childTypeUri, new TopicModel(childTypeUri, value));
        return this;
    }

    // ---

    /**
     * Puts a by-ID topic reference for a single-valued child.
     * An existing reference is overwritten.
     *
     * Used to maintain the assigment of an *aggregated* child.
     * Not applicable for a *compositioned* child.
     */
    public CompositeValueModel putRef(String childTypeUri, long refTopicId) {
        put(childTypeUri, new TopicModel(refTopicId, childTypeUri));
        return this;
    }

    /**
     * Puts a by-URI topic reference for a single-valued child.
     * An existing reference is overwritten.
     *
     * Used to maintain the assigment of an *aggregated* child.
     * Not applicable for a *compositioned* child.
     */
    public CompositeValueModel putRef(String childTypeUri, String refTopicUri) {
        put(childTypeUri, new TopicModel(refTopicUri, childTypeUri));
        return this;
    }

    // ---

    /**
     * Adds a value to a multiple-valued child.
     */
    public CompositeValueModel add(String childTypeUri, TopicModel value) {
        List<TopicModel> topics = getTopics(childTypeUri, null);     // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            values.put(childTypeUri, topics);
        }
        // Note 1: we must not add a topic twice.
        // This would happen e.g. when updating multi-facets: the facet values are added while updating, and
        // would be added again through the PRE_SEND event (Kiezatlas plugin).
        //
        // Note 2: we must not add a topic twice *unless* its ID is -1.
        // This happens when adding a couple of new child topics at once e.g. by pressing the "Add" button
        // serveral times in a webclient form. These new topic models have no ID yet (-1).
        if (value.getId() == -1 || !topics.contains(value)) {
            topics.add(value);
        }
        //
        return this;
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    public CompositeValueModel remove(String childTypeUri, TopicModel value) {
        List<TopicModel> topics = getTopics(childTypeUri, null);     // defaultValue=null
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     *
     * Used to maintain the assigments of *aggregated* childs.
     * Not applicable for *compositioned* childs.
     */
    public CompositeValueModel addRef(String childTypeUri, long refTopicId) {
        add(childTypeUri, new TopicModel(refTopicId, childTypeUri));
        return this;
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     *
     * Used to maintain the assigments of *aggregated* childs.
     * Not applicable for *compositioned* childs.
     */
    public CompositeValueModel addRef(String childTypeUri, String refTopicUri) {
        add(childTypeUri, new TopicModel(refTopicUri, childTypeUri));
        return this;
    }



    // ===

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String childTypeUri : keys()) {
                Object value = get(childTypeUri);
                if (value instanceof TopicModel) {
                    json.put(childTypeUri, ((TopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(childTypeUri, DeepaMehtaUtils.objectsToJSON((List<TopicModel>) value));
                } else {
                    throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization of a CompositeValueModel failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public CompositeValueModel clone() {
        CompositeValueModel clone = new CompositeValueModel();
        for (String childTypeUri : keys()) {
            Object value = get(childTypeUri);
            if (value instanceof TopicModel) {
                TopicModel model = (TopicModel) value;
                clone.put(childTypeUri, model.clone());
            } else if (value instanceof List) {
                for (TopicModel model : (List<TopicModel>) value) {
                    clone.add(childTypeUri, model.clone());
                }
            } else {
                throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        return values.toString();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a topic model from a JSON value.
     *
     * Both topic serialization formats are supported:
     * 1) canonic format -- contains entire topic models.
     * 2) compact format -- contains the topic value only (simple or composite).
     */
    private TopicModel createTopicModel(String childTypeUri, Object value) {
        if (value instanceof JSONObject) {
            JSONObject val = (JSONObject) value;
            // we detect the canonic format by checking for a mandatory topic property
            // ### TODO: "type_uri" should not be regarded mandatory. It would simplify update requests.
            // ### Can we use another heuristic for detection: "value" exists OR "composite" exists?
            if (val.has("type_uri")) {
                // canonic format
                return new TopicModel(val);
            } else {
                // compact format (composite topic)
                return new TopicModel(childTypeUri, new CompositeValueModel(val));
            }
        } else {
            // compact format (simple topic or topic reference)
            if (value instanceof String) {
                String val = (String) value;
                if (val.startsWith(REF_ID_PREFIX)) {
                    return new TopicModel(refTopicId(val), childTypeUri);   // topic reference by-ID
                } else if (val.startsWith(REF_URI_PREFIX)) {
                    return new TopicModel(refTopicUri(val), childTypeUri);  // topic reference by-URI
                } else if (val.startsWith(DEL_PREFIX)) {
                    return new TopicDeletionModel(delTopicId(val));         // topic deletion reference
                }
            }
            // compact format (simple topic)
            return new TopicModel(childTypeUri, new SimpleValue(value));
        }
    }

    // ---

    private long refTopicId(String val) {
        return Long.parseLong(val.substring(REF_ID_PREFIX.length()));
    }

    private String refTopicUri(String val) {
        return val.substring(REF_URI_PREFIX.length());
    }

    private long delTopicId(String val) {
        return Long.parseLong(val.substring(DEL_PREFIX.length()));
    }

    // ---

    /**
     * ### TODO: should not be public. Specify interfaces also for model classes?
     */
    public void throwInvalidAccess(String childTypeUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + childTypeUri +
                "\": the caller assumes it to be multiple-value but it is single-value in\n" + this, e);
        } else {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + childTypeUri +
                "\" in\n" + this, e);
        }
    }
}
