package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.util.DeepaMehtaUtils;

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
    // ID equality (see DeepaMehtaObjectModel.equals()).

    /**
     * The parent object this child topics belong to.
     * Only initialized while update operation. ### FIXME: inject
     */
    private DeepaMehtaObjectModelImpl parent;

    private PersistenceLayer pl;
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsModelImpl(Map<String, Object> childTopics, PersistenceLayer pl) {
        this.childTopics = childTopics;
        this.pl = pl;
        this.mf = pl.mf;
    }

    ChildTopicsModelImpl(ChildTopicsModelImpl childTopics) {
        this(childTopics.childTopics, childTopics.pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    @Override
    public RelatedTopicModel getTopic(String assocDefUri) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topic;
    }

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicOrNull(), catch ClassCastException
     */
    @Override
    public RelatedTopicModel getTopic(String assocDefUri, RelatedTopicModel defaultValue) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        return topic != null ? topic : defaultValue;
    }

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    @Override
    public List<RelatedTopicModel> getTopics(String assocDefUri) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " +
                    childTopics.keySet());
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicsOrNull()
     */
    @Override
    public List<RelatedTopicModel> getTopics(String assocDefUri, List<RelatedTopicModel> defaultValue) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * Accesses a child generically, regardless of single-valued or multiple-valued.
     * Returns null if there is no such child.
     *
     * @return  A RelatedTopicModel or List<RelatedTopicModel>, or null if there is no such child.
     */
    @Override
    public Object get(String assocDefUri) {
        return childTopics.get(assocDefUri);
    }

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    @Override
    public boolean has(String assocDefUri) {
        return childTopics.containsKey(assocDefUri);
    }

    /**
     * Returns the number of childs contained in this ChildTopicsModel.
     * Multiple-valued childs count as one.
     */
    @Override
    public int size() {
        return childTopics.size();
    }



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public String getString(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().toString();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public String getString(String assocDefUri, String defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public int getInt(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().intValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public int getInt(String assocDefUri, int defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public long getLong(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().longValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public long getLong(String assocDefUri, long defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public double getDouble(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().doubleValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public double getDouble(String assocDefUri, double defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public boolean getBoolean(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().booleanValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public boolean getBoolean(String assocDefUri, boolean defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public Object getObject(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().value();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public Object getObject(String assocDefUri, Object defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public ChildTopicsModel getChildTopicsModel(String assocDefUri) {
        return getTopic(assocDefUri).getChildTopicsModel();
    }

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public ChildTopicsModel getChildTopicsModel(String assocDefUri, ChildTopicsModel defaultValue) {
        RelatedTopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getChildTopicsModel() : defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    /**
     * Puts a value in a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, RelatedTopicModel value) {
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
    public ChildTopicsModel put(String assocDefUri, TopicModel value) {
        return put(assocDefUri, mf.newRelatedTopicModel(value));
    }

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @param   value   a String, Integer, Long, Double, or a Boolean.
     *
     * @return  this ChildTopicsModel.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, Object value) {
        try {
            return put(assocDefUri, mf.newTopicModel(mf.childTypeUri(assocDefUri), new SimpleValue(value)));
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a ChildTopicsModel failed (assocDefUri=\"" +
                assocDefUri + "\", value=" + value + ")", e);
        }
    }

    /**
     * Convenience method to put a *composite* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @return  this ChildTopicsModel.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, ChildTopicsModel value) {
        return put(assocDefUri, mf.newTopicModel(mf.childTypeUri(assocDefUri), value));
    }

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    @Override
    public ChildTopicsModel putRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    @Override
    public ChildTopicsModel putRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Puts a by-ID topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel putDeletionRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel putDeletionRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Removes a single-valued child.
     */
    @Override
    public ChildTopicsModel remove(String assocDefUri) {
        childTopics.remove(assocDefUri);    // ### TODO: throw if not in map?
        return this;
    }

    // --- Multiple-valued Childs ---

    /**
     * Adds a value to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel add(String assocDefUri, RelatedTopicModel value) {
        List<RelatedTopicModel> topics = getTopics(assocDefUri, null);      // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(assocDefUri, topics);
        }
        //
        topics.add(value);
        //
        return this;
    }

    @Override
    public ChildTopicsModel add(String assocDefUri, TopicModel value) {
        return add(assocDefUri, mf.newRelatedTopicModel(value));
    }

    /**
     * Sets the values of a multiple-valued child.
     * Existing values are overwritten.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, List<RelatedTopicModel> values) {
        childTopics.put(assocDefUri, values);
        return this;
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    @Override
    public ChildTopicsModel remove(String assocDefUri, TopicModel value) {
        List<RelatedTopicModel> topics = getTopics(assocDefUri, null);      // defaultValue=null
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, mf.newTopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, mf.newTopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addDeletionRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, mf.newTopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic deletion reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addDeletionRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, mf.newTopicDeletionModel(refTopicUri));
        return this;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this ChildTopicsModel's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return childTopics.keySet().iterator();
    }



    // ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String assocDefUri : this) {
                Object value = get(assocDefUri);
                if (value instanceof RelatedTopicModel) {
                    json.put(assocDefUri, ((RelatedTopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(assocDefUri, DeepaMehtaUtils.toJSONArray((List<RelatedTopicModel>) value));
                } else {
                    throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization of a ChildTopicsModel failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public ChildTopicsModel clone() {
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

    @Override
    public String toString() {
        return childTopics.toString();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void throwInvalidSingleAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().startsWith("java.util.ArrayList cannot be cast to")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as single but is defined as multi", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + "\" failed", e);
        }
    }

    void throwInvalidMultiAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as multi but is defined as single", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + " failed", e);
        }
    }



    // === Update ===

    void update(ChildTopicsModel newModel) {
        try {
            for (AssociationDefinitionModel assocDef : parent.getType().getAssocDefs()) {
                String assocDefUri    = assocDef.getAssocDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                RelatedTopicModel newChildTopic        = null;  // only used for "one"
                List<RelatedTopicModel> newChildTopics = null;  // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newModel.getTopic(assocDefUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newModel.getTopics(assocDefUri, null);      // defaultValue=null
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
    void loadChildTopics() {
        for (AssociationDefinitionModel assocDef : parent.getType().getAssocDefs()) {
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
    private void loadChildTopics(AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        if (!has(assocDefUri)) {
            logger.fine("### Lazy-loading \"" + assocDefUri + "\" child topic(s) of " + parentInfo());
            pl.valueStorage.fetchChildTopics(parent, assocDef);
        }
    }

    private void recalculateParentLabel() {
        List<String> labelAssocDefUris = null;
        try {
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



    // === Update Child Topics ===

    // --- Composition ---

    private void updateCompositionOne(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
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

    private void updateCompositionMany(List<RelatedTopicModel> newChildTopics, AssociationDefinitionModel assocDef) {
        for (RelatedTopicModel newChildTopic : newChildTopics) {
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

    private void updateAggregationOne(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
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

    private void updateAggregationMany(List<RelatedTopicModel> newChildTopics, AssociationDefinitionModel assocDef) {
        for (RelatedTopicModel newChildTopic : newChildTopics) {
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

    private void updateChildTopicOne(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = _getTopicOrNull(assocDef.getAssocDefUri());
        //
        if (childTopic == null || childTopic.getId() != newChildTopic.getId()) {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parentInfo() + " according to " + assocDef);
        }
        //
        updateRelatedTopic(childTopic, newChildTopic);
        // Note: memory is already up-to-date. The child topic is updated in-place of parent.
    }

    private void updateChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = findChildTopicById(newChildTopic.getId(), assocDef);
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

    private void updateRelatedTopic(RelatedTopicModelImpl childTopic, RelatedTopicModel newChildTopic) {
        // update topic
        childTopic.update(newChildTopic);
        // update association
        updateRelatingAssociation(childTopic, newChildTopic);
    }

    private void updateRelatingAssociation(RelatedTopicModelImpl childTopic, RelatedTopicModel newChildTopic) {
        childTopic.getRelatingAssociation().update(newChildTopic.getRelatingAssociation());
    }

    // --- Create ---

    private void createChildTopicOne(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        // update DB
        createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        putInChildTopics(newChildTopic, assocDef);
    }

    private void createChildTopicMany(RelatedTopicModel newChildTopic, AssociationDefinitionModel assocDef) {
        // update DB
        createAndAssociateChildTopic(newChildTopic, assocDef);
        // update memory
        addToChildTopics(newChildTopic, assocDef);
    }

    // ---

    private void createAndAssociateChildTopic(RelatedTopicModel childTopic, AssociationDefinitionModel assocDef) {
        pl.createTopic(childTopic);
        associateChildTopic(childTopic, assocDef);
    }

    // --- Assignment ---

    private void createAssignmentOne(RelatedTopicModelImpl childTopic, TopicReferenceModel newChildTopic,
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
        putInChildTopics(newChildTopic, assocDef);
    }

    private void createAssignmentMany(TopicReferenceModelImpl newChildTopic, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = findChildTopicByRef(newChildTopic, assocDef);
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
        addToChildTopics(newChildTopic, assocDef);
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
        pl.valueStorage.associateChildTopic(parent, childTopic, assocDef);
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
        removeChildTopic(assocDef);
    }

    private void deleteChildTopicMany(long childTopicId, AssociationDefinitionModel assocDef,
                                                         boolean deleteChildTopic) {
        RelatedTopicModelImpl childTopic = findChildTopicById(childTopicId, assocDef);
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



    // === ChildTopics Model ===

    // --- Access ---

    // ### Drop these getters. Revise the standard getters.

    private RelatedTopicModelImpl _getTopic(String assocDefUri) {
        RelatedTopicModelImpl topic = _getTopicOrNull(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet() +
                " (" + parentInfo() + ")");
        }
        //
        return topic;
    }

    private RelatedTopicModelImpl _getTopicOrNull(String assocDefUri) {
        try {
            return (RelatedTopicModelImpl) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            throwInvalidSingleAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    private List<RelatedTopicModelImpl> _getTopics(String assocDefUri) {
        List<RelatedTopicModelImpl> topics = _getTopicsOrNull(assocDefUri);
        // error check
        if (topics == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet() +
                " (" + parentInfo() + ")");
        }
        //
        return topics;
    }

    private List<RelatedTopicModelImpl> _getTopicsOrNull(String assocDefUri) {
        try {
            return (List<RelatedTopicModelImpl>) childTopics.get(assocDefUri);
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * For multiple-valued childs: looks in the attached object cache for a child topic by ID. ### FIXDOC
     */
    private RelatedTopicModelImpl findChildTopicById(long childTopicId, AssociationDefinitionModel assocDef) {
        List<RelatedTopicModelImpl> childTopics = _getTopicsOrNull(assocDef.getAssocDefUri());
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
    private RelatedTopicModelImpl findChildTopicByRef(TopicReferenceModelImpl topicRef,
                                                      AssociationDefinitionModel assocDef) {
        List<? extends RelatedTopicModel> childTopics = _getTopicsOrNull(assocDef.getAssocDefUri());
        if (childTopics != null) {
            return topicRef.findReferencedTopic(childTopics);
        }
        return null;
    }

    // ---

    private AssociationDefinitionModel getAssocDef(String assocDefUri) {
        // Note: doesn't work for facets
        return parent.getType().getAssocDef(assocDefUri);
    }

    // --- Update ---

    /**
     * For single-valued childs
     */
    private void putInChildTopics(RelatedTopicModel childTopic, AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        put(assocDefUri, childTopic);
    }

    /**
     * For single-valued childs
     */
    private void removeChildTopic(AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        remove(assocDefUri);
    }

    /**
     * For multiple-valued childs
     */
    private void addToChildTopics(RelatedTopicModel childTopic, AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        add(assocDefUri, childTopic);
    }

    /**
     * For multiple-valued childs
     */
    private void removeFromChildTopics(RelatedTopicModel childTopic, AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getAssocDefUri();
        remove(assocDefUri, childTopic);
    }

    // ---

    private String parentInfo() {
        return parent.className() + " " + parent.id;
    }
}
