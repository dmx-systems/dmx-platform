package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicDeletionModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicReferenceModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Integrates new values into the DB and maintains the references.
 *
 * Note: this class is not thread-safe. A ValueIntegrator instance must not be shared between threads.
 */
class ValueIntegrator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DMXObjectModelImpl newValues;
    private DMXObjectModelImpl targetObject;        // may null
    private CompDefModel compDef;                   // may null
    private TypeModelImpl type;
    private boolean isAssoc;
    private boolean isType;
    private boolean isFacetUpdate;

    private AccessLayer al;
    private ModelFactoryImpl mf;
    private EventManager em;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueIntegrator(AccessLayer al) {
        this.al = al;
        this.mf = al.mf;
        this.em = al.em;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Integrates new values into the DB and returns the unified value.
     *
     * @param   newValues       the "update model": the values to be integrated.
     * @param   targetObject    may be null. TODO: explain
     * @param   compDef         for facet value updates: the facet type's comp def.
     *                          <code>null</code> for non-facet updates.
     *
     * @return  the unified value; never null; its "value" field is null if there was nothing to integrate.
     */
    <M extends DMXObjectModelImpl> UnifiedValue<M> integrate(M newValues, M targetObject, CompDefModel compDef) {
        try {
            this.newValues = newValues;
            this.targetObject = targetObject;
            this.compDef = compDef;
            this.isAssoc = newValues instanceof AssocModel;
            this.isType  = newValues instanceof TypeModel;
            this.isFacetUpdate = compDef != null;
            //
            // process refs
            if (newValues instanceof TopicReferenceModel) {
                return unifyRef();
            }
            if (newValues instanceof TopicDeletionModel) {
                return new UnifiedValue(null);
            }
            // convenience: take newValues's typeUri from target object (if available)
            if (newValues.typeUri == null && targetObject != null) {
                newValues.typeUri = targetObject.typeUri;
            }
            // argument check
            if (newValues.typeUri == null) {
                throw new IllegalArgumentException("Tried to integrate values whose typeUri is not set, newValues=" +
                    newValues + ", targetObject=" + targetObject);
            }
            // Note: we must get type *after* processing refs. Refs might have no type set.
            this.type = newValues.getType();
            //
            // --- Value Integration ---
            // Note: because a facet type is composite by definition a facet update is always a composite operation,
            // even if the faceted object is a simple one.
            Result r = !isFacetUpdate && newValues.isSimple() ? integrateSimple() : integrateComposite();
            DMXObjectModelImpl _value = r.value;
            //
            if (r.created) {
                _value.postCreate();
                em.fireEvent(CoreEvent.POST_CREATE_TOPIC, _value.instantiate());
            }
            // Note: UnifiedValue instantiation saves the new value's ID *before* it is overwritten
            UnifiedValue value = new UnifiedValue(_value);
            idTransfer(_value);
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Value integration failed, newValues=" + newValues + ", targetObject=" +
                targetObject + ", compDef=" + compDef, e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private UnifiedValue unifyRef() {
        TopicReferenceModelImpl ref = (TopicReferenceModelImpl) newValues;
        if (!ref.isEmptyRef()) {
            DMXObjectModelImpl object = ref.resolve();
            logger.fine("Referencing " + object);
            return new UnifiedValue(object, ref.originalId);
        } else {
            return new UnifiedValue(null);
        }
    }

    // Sets the ID of the value integration result into the update model.
    //
    // Note: this is a side effect, but we keep it for pragmatic reasons.
    //
    // In DM4 the create topic/assoc methods have the side effect of setting the generated ID into the update model.
    // In DM5 the update model is not passed directly to the storage layer, but a new model object is created (see
    // createSimpleTopic()). The ID transfer is here to emulate the DM4 behavior.
    //
    // Without this side effect e.g. managing view configs would be more hard. When creating a type through a migration
    // (e.g. while bootstrap) the read type model is put in the type cache as is. Without the side effect the view
    // config topic models would have no ID. Updating view config values later on would fail.
    private void idTransfer(DMXObjectModelImpl value) {
        if (value != null) {
            if (value.id == -1) {
                throw new RuntimeException("ID of unification result is not initialized: " + value);
            }
            newValues.id = value.id;
        }
    }

    // --- Simple ---

    /**
     * Integrates a simple value into the DB and returns the unified simple value.
     *
     * Preconditions:
     *   - this.newValues is not null
     *   - this.newValues is simple
     *
     * @return  Result value: the unified value, or null if there was nothing to integrate.
     *          The latter is the case if this.newValues is the empty string.
     */
    private Result integrateSimple() {
        if (isAssoc || isType) {
            // Note 1: an assoc's simple value is not unified. In contrast to a topic an assoc can't be unified with
            // another assoc. (Even if 2 assocs have the same type and value they are not the same as they still
            // have different players.) An assoc's simple value is updated in-place.
            // Note 2: a type's simple value is not unified. A type is updated in-place.
            return new Result(storeAssocSimpleValue());
        } else if (newValues.getSimpleValue().toString().isEmpty()) {
            return new Result();
        } else {
            return unifySimple();
        }
    }

    /**
     * Preconditions:
     *   - this.newValues is an assoc model.
     */
    private DMXObjectModelImpl storeAssocSimpleValue() {
        if (targetObject != null) {
            // update
            targetObject._updateSimpleValue(newValues.getSimpleValue());
            return targetObject;
        } else {
            // create
            newValues.storeSimpleValue();
            return newValues;
        }
    }

    /**
     * Fetches a simple topic by-value, or creates it.
     *
     * Preconditions:
     *   - this.newValues is simple
     *   - this.newValues is not empty
     *
     * @return  Result value: the fetched or created topic; never null.
     */
    private Result unifySimple() {
        TopicModelImpl topic;
        boolean created = false;
        SimpleValue newValue = newValues.getSimpleValue();
        // Note: only readable topics can be reused (access control is applied)
        List<TopicModelImpl> topics = al.getTopicsByValue(type.uri, newValue);
        int size = topics.size();
        if (size > 0) {
            topic = topics.get(0);
            // Note: values are not globally unique due to asymmetric access realms (user A's content might be readable
            // by user B but not vice versa)
            if (size > 1) {
                logger.warning("ValueIntegrator ambiguity: there are " + size + " readable \"" + newValue +
                    "\" topics (typeUri=\"" + type.uri + "\", " + DMXUtils.idList(topics) + ") => using " + topic.id);
            }
            logger.fine("Reusing simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri + "\")");
        } else {
            topic = al.createSingleTopic(mf.newTopicModel(newValues.uri, newValues.typeUri, newValues.value));
            created = true;
            logger.fine("### Creating simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri +
                "\")");
        }
        return new Result(topic, created);
    }

    // --- Composite ---

    /**
     * Integrates a composite value into the DB and returns the unified composite value.
     *
     * Preconditions:
     *   - this.newValues is composite
     *
     * @return  Result value: the unified value, or null if there was nothing to integrate.
     */
    private Result integrateComposite() {
        Map<String, Object> childValues = new HashMap();    // value: UnifiedValue or List<UnifiedValue>
        ChildTopicsModel childTopics = newValues.getChildTopics();
        // Iterate through type, not through newValues.
        // newValues might contain children not contained in the type def, e.g. "dmx.timestamps.modified".
        for (String compDefUri : compDefUris()) {
            Object newChildValue;    // RelatedTopicModelImpl or List<RelatedTopicModelImpl>
            if (isOne(compDefUri)) {
                newChildValue = childTopics.getTopicOrNull(compDefUri);
            } else {
                // TODO: if empty?
                newChildValue = childTopics.getTopicsOrNull(compDefUri);
            }
            // skip if not contained in (partial) update request
            if (newChildValue == null) {
                continue;
            }
            //
            Object childValue = integrateChildValue(newChildValue, compDefUri); // childValue: UnifiedValue or
                                                                                // List<UnifiedValue>; never null
            childValues.put(compDefUri, childValue);
        }
        Result r = unifyComposite(childValues);
        DMXObjectModelImpl value = r.value;
        //
        // label calculation
        if (!isFacetUpdate) {
            if (value != null) {
                new LabelCalculation(value).calculate();
            } else if (isAssoc) {
                storeAssocSimpleValue();
            }
        }
        //
        return r;
    }

    private Iterable<String> compDefUris() {
        return !isFacetUpdate ? type : asList(compDef.getCompDefUri());
    }

    /**
     * Integrates a child value into the DB and returns the unified value.
     *
     * @param   childValue      RelatedTopicModelImpl or List<RelatedTopicModelImpl>
     *
     * @return  UnifiedValue or List<UnifiedValue>; never null.
     */
    private Object integrateChildValue(Object childValue, String compDefUri) {
        if (isOne(compDefUri)) {
            return new ValueIntegrator(al).integrate((RelatedTopicModelImpl) childValue, null, null);
        } else {
            List<UnifiedValue> values = new ArrayList();
            for (RelatedTopicModelImpl value : (List<RelatedTopicModelImpl>) childValue) {
                values.add(new ValueIntegrator(al).integrate(value, null, null));
            }
            return values;
        }
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - compDef's parent type is this.type
     *   - childTopic's type is compDef's child type
     *
     * @param   childValues     key: compDefUri
     *                          value: UnifiedValue or List<UnifiedValue>
     *
     * @return  Result value: the unified value, or null if there was nothing to integrate.
     */
    private Result unifyComposite(Map<String, Object> childValues) {
        // Note: because a facet does not contribute to the value of a value object
        // a facet update is always an in-place modification
        if (isValueType() && !isFacetUpdate) {
            return unifyChildTopics(childValues, type);
        } else {
            Result r = identifyParent(childValues);
            DMXObjectModelImpl parent = r.value;
            if (parent != null) {
                updateAssignments(parent, childValues);
                return r;
            }
            return new Result();
        }
    }

    /**
     * Identifies the parent to be updated in-place, or creates it.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - this.type is an entity type OR this is a facet update
     *
     * @param   childValues     value: UnifiedValue or List<UnifiedValue>
     *
     * @return  Result value: the parent object, or null if there is no parent
     */
    private Result identifyParent(Map<String, Object> childValues) {
        // TODO: 1st check identity attrs THEN target object?? => NO!
        if (targetObject != null) {
            return new Result(targetObject);
        } else if (isAssoc) {
            if (newValues.id == -1) {
                throw new RuntimeException("newValues has no ID set");
            }
            // TODO: partial updates. URI and player models must not expected to be part of the update model.
            AssocModelImpl _newValues = (AssocModelImpl) newValues;
            return new Result(mf.newAssocModel(newValues.id, newValues.uri, newValues.typeUri, _newValues.player1,
                                                                                               _newValues.player2));
        } else {
            List<String> identityCompDefUris = type.getIdentityAttrs();
            if (identityCompDefUris.size() > 0) {
                if (childValues.isEmpty()) {
                    return new Result();
                }
                Map<String, Object> childTopics = identityChildValues(childValues, identityCompDefUris);
                return unifyChildTopics(childTopics, identityCompDefUris);
            } else {
                return new Result(createCompositeTopic(childValues), true);
            }
        }
    }

    /**
     * From the given child values selects these ones which made up the parent topic's identity.
     *
     * @param   childValues             the map of the child values to select from; not empty
     *                                      key: compDefUri
     *                                      value: UnifiedValue or List<UnifiedValue>
     * @param   identityCompDefUris     not empty
     *
     * @return  A map of the identity child values; may be empty
     *              key: compDefUri
     *              value: UnifiedValue
     */
    private Map<String, Object> identityChildValues(Map<String, Object> childValues, List<String> identityCompDefUris) {
        try {
            Map<String, Object> identityChildValues = new HashMap();
            for (String compDefUri : identityCompDefUris) {
                if (!isOne(compDefUri)) {
                    throw new RuntimeException("Cardinality \"many\" identity attributes not supported");
                }
                UnifiedValue childValue = (UnifiedValue) childValues.get(compDefUri);
                if (childValue == null) {
                    throw new RuntimeException("Identity value \"" + compDefUri + "\" is missing in " +
                        childValues.keySet());
                }
                // Note: having an empty value is not an error.
                // Consider e.g. an embedded identity form left empty by the user.
                if (childValue.value != null) {
                    identityChildValues.put(compDefUri, childValue);
                }
            }
            return identityChildValues;
        } catch (Exception e) {
            throw new RuntimeException("Selecting identity values " + identityCompDefUris + " failed, childValues=" +
                childValues, e);
        }
    }

    /**
     * Updates a parent's child assignments in-place.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - this.type is an entity type OR this is a facet update
     *   - parent's type is this.type
     *   - compDef's parent type is this.type
     *   - newChildTopic's type is compDef's child type
     *
     * @param   parent          to parent to be updated; not null
     * @param   childValues     key: compDefUri
     *                          value: UnifiedValue or List<UnifiedValue>
     */
    private void updateAssignments(DMXObjectModelImpl parent, Map<String, Object> childValues) {
        for (String compDefUri : compDefUris()) {
            // TODO: possible optimization: load only ONE child level here (deep=false). Later on, when updating the
            // assignments, load the remaining levels only IF the assignment did not change. In contrast if the
            // assignment changes, a new subtree is attached. The subtree is fully constructed already (through all
            // levels) as it is build bottom-up (starting from the simple values at the leaves).
            parent.loadChildTopics(compDef(compDefUri), true);    // deep=true
            Object childValue = childValues.get(compDefUri);
            // Note: for partial create/update requests childValue might be null
            if (childValue != null) {
                if (isOne(compDefUri)) {
                    updateAssignmentsOne(parent, (UnifiedValue) childValue, compDefUri);
                } else {
                    updateAssignmentsMany(parent, (List<UnifiedValue>) childValue, compDefUri);
                }
            }
        }
    }

    /**
     * @param   childValue      never null; a UnifiedValue's "value" field may be null
     */
    private void updateAssignmentsOne(DMXObjectModelImpl parent, UnifiedValue<TopicModelImpl> childValue,
                                                                 String compDefUri) {
        try {
            ChildTopicsModelImpl oldChildTopics = parent.getChildTopics();
            RelatedTopicModelImpl oldValue = oldChildTopics.getTopicOrNull(compDefUri);     // may be null
            if (oldValue != null && oldValue.id == -1) {
                throw new RuntimeException("Old value's ID is not initialized, oldValue=" + oldValue);
            }
            TopicModel childTopic = childValue.value;
            boolean newValueIsEmpty = childTopic == null;
            //
            // 1) delete assignment if exists AND value has changed or emptied
            //
            boolean deleted = false;
            if (oldValue != null && (newValueIsEmpty || childTopic != null && !oldValue.equals(childTopic))) {
                // update DB
                oldValue.getRelatingAssoc().delete();
                // update memory
                if (newValueIsEmpty) {
                    logger.fine("### Deleting assignment (compDefUri=\"" + compDefUri + "\") from composite " +
                        parent.id + " (typeUri=\"" + type.uri + "\")");
                    oldChildTopics.remove(compDefUri);
                }
                deleted = true;
            }
            // 2) create assignment if not exists OR value has changed
            // a new value must be present
            //
            AssocModelImpl assoc = null;
            if (childTopic != null && (oldValue == null || !oldValue.equals(childTopic))) {
                // update DB
                assoc = createChildAssoc(parent, childTopic, compDefUri, deleted);
                // update memory
                oldChildTopics.set(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
            }
            // 3) update relating assoc
            //
            // take the old assoc if no new one is created, there is an old one, and it has not been deleted
            if (assoc == null && oldValue != null && !deleted) {
                assoc = oldValue.getRelatingAssoc();
            }
            if (assoc != null) {
                RelatedTopicModelImpl newChildValue = newValues.getChildTopics().getTopicOrNull(compDefUri);
                updateRelatingAssoc(assoc, compDefUri, newChildValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating assigment failed, parent=" + parent + ", childValue=" + childValue +
                ", compDefUri=\"" + compDefUri + "\"", e);
        }
    }

    /**
     * @param   childValues   never null; a UnifiedValue's "value" field may be null
     */
    private void updateAssignmentsMany(DMXObjectModelImpl parent, List<UnifiedValue> childValues, String compDefUri) {
        ChildTopicsModelImpl oldChildTopics = parent.getChildTopics();
        List<RelatedTopicModelImpl> oldValues = oldChildTopics.getTopicsOrNull(compDefUri);   // may be null
        for (UnifiedValue childValue : childValues) {
            TopicModel childTopic = (TopicModel) childValue.value;
            long originalId = childValue.originalId;
            long newId = childTopic != null ? childTopic.getId() : -1;
            boolean valueEmptied = newId == -1;
            RelatedTopicModelImpl oldValue = null;
            if (originalId != -1) {
                if (oldValues == null) {
                    throw new RuntimeException("Tried to replace original topic " + originalId +
                        " when there are no old topics (null)");
                }
                oldValue = findTopic(oldValues, originalId);
            } else if (!valueEmptied && oldValues != null) {
                oldValue = findTopicOrNull(oldValues, newId);
            }
            boolean oldValueExists = oldValue != null;
            boolean valueChanged = oldValueExists && oldValue.id != newId;      // true if changed or emptied
            //
            // 1) delete assignment if exists AND value has changed or emptied
            //
            boolean deleted = false;
            if (valueChanged) {
                if (valueEmptied) {
                    logger.fine("### Deleting assignment (compDefUri=\"" + compDefUri + "\") from composite " +
                        parent.id + " (typeUri=\"" + type.uri + "\")");
                }
                deleted = true;
                // update DB
                oldValue.getRelatingAssoc().delete();
                // update memory
                removeTopic(oldValues, originalId);
            }
            // 2) create assignment if not exists OR value has changed
            // a new value must be present
            //
            AssocModelImpl assoc = null;
            if (!valueEmptied && (!oldValueExists || valueChanged)) {
                // update DB
                assoc = createChildAssoc(parent, childTopic, compDefUri, deleted);
                // update memory
                oldChildTopics.add(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
            }
            // 3) update relating assoc
            //
            // take the old assoc if no new one is created, there is an old one, and it has not been deleted
            if (assoc == null && oldValueExists && !deleted) {
                assoc = oldValue.getRelatingAssoc();
            }
            if (assoc != null) {
                RelatedTopicModelImpl newValues = (RelatedTopicModelImpl) childValue._newValues;
                updateRelatingAssoc(assoc, compDefUri, newValues);
            }
        }
    }

    private void updateRelatingAssoc(AssocModelImpl assoc, String compDefUri, RelatedTopicModelImpl newValues) {
        try {
            // Note: for partial create/update requests newValues might be null
            if (newValues != null) {
                AssocModelImpl _newValues = newValues.getRelatingAssoc();
                // Note: the players must be suppressed from being updated. Update would fail if a new child has
                // been assigned (step 2) because the player is another one then. Here we are only interested
                // in updating the assoc value.
                _newValues.setPlayer1(null);
                _newValues.setPlayer2(null);
                // Note: if no relating assocs are contained in a create/update request the model factory
                // creates assocs anyways, but these are completely uninitialized. ### TODO: Refactor
                // TODO: is condition needed? => yes, try create new topic
                if (_newValues.typeUri != null) {
                    assoc.update(_newValues);
                    // TODO: access control? Note: currently the child assocs of a workspace have no workspace
                    // assignments. With strict access control, updating a workspace topic would fail.
                    // al.updateAssoc(assoc, _newValues);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating relating assoc " + assoc.id + " failed, compDefUri=\"" + compDefUri +
                "\", assoc=" + assoc, e);
        }
    }

    // ---

    /**
     * Finds a parent topic in the DB that has the given child topics, or creates it.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - compDef's parent type is this.type
     *   - childTopic's type is compDef's child type
     *
     * @param   childValues     Child topics to unify; not empty
     *                              key: compDefUri
     *                              value: UnifiedValue or List<UnifiedValue>
     *
     * @param   compDefUris     only these child topics are respected
     *
     * @return  Result value: the found (or created) parent topic, or null if there was nothing to integrate.
     */
    private Result unifyChildTopics(Map<String, Object> childValues, Iterable<String> compDefUris) {
        List<? extends TopicModelImpl> candidates = parentCandidates(childValues);
        if (candidates == null) {
            return new Result();
        }
        // logger.info("### " + candidates.size() + " candidates " + DMXUtils.idList(candidates));
        for (String compDefUri : compDefUris) {
            if (isOne(compDefUri)) {
                UnifiedValue<TopicModelImpl> value = (UnifiedValue) childValues.get(compDefUri);
                // Note: value is null if added to emptyValues (see integrateComposite())
                eliminateParentCandidates(candidates, value != null ? value.value : null, compDefUri);
            } else {
                List<UnifiedValue> values = (List<UnifiedValue>) childValues.get(compDefUri);
                eliminateParentCandidates(candidates, values, compDefUri);
            }
            if (candidates.isEmpty()) {
                break;
            }
        }
        int size = candidates.size();
        if (size > 0) {
            DMXObjectModelImpl comp = candidates.get(0);
            // Note: values are not globally unique due to asymmetric access realms (user A's content might be readable
            // by user B but not vice versa)
            if (size > 1) {
                logger.warning("ValueIntegrator ambiguity: there are " + candidates.size() + " parents (typeUri=\"" +
                    type.uri + "\", " + DMXUtils.idList(candidates) + ") which have the same " + childValues.values()
                    .size() + " child topics => using " + comp.id + ", child topics " + childValues.values());
            }
            // logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + type.uri + "\")");
            return new Result(comp);
        } else {
            // logger.info("### no composite found, childValues=" + childValues);
            return new Result(createCompositeTopic(childValues), true);
        }
    }

    /**
     * Determine parent topics ("candidates") which might match the given child values.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - compDef's parent type is this.type
     *   - childTopic's type is compDef's child type
     *
     * @param   childValues     key: compDefUri
     *                          value: UnifiedValue or List<UnifiedValue>
     *
     * @return  the list of parent candidates (may be empty), or null if there was nothing to integrate.
     */
    private List<? extends TopicModelImpl> parentCandidates(Map<String, Object> childValues) {
        DMXObjectModel childTopic = null;
        String compDefUri = null;
        for (String _compDefUri : childValues.keySet()) {
            childTopic = findNonEmptyChildTopic(childValues.get(_compDefUri), _compDefUri);
            if (childTopic != null) {
                compDefUri = _compDefUri;
                break;
            }
        }
        return childTopic == null ? null : al.getTopicRelatedTopics(childTopic.getId(),
            compDef(compDefUri).getInstanceLevelAssocTypeUri(), CHILD, PARENT, type.getUri());
    }

    /**
     * Finds the first non-empty value.
     *
     * @param   childValue      UnifiedValue or List<UnifiedValue>
     *
     * @return  the first non-empty value, or null
     */
    private DMXObjectModel findNonEmptyChildTopic(Object childValue, String compDefUri) {
        DMXObjectModel childTopic = null;
        if (isOne(compDefUri)) {
            childTopic = ((UnifiedValue) childValue).value;
        } else {
            for (UnifiedValue value : (List<UnifiedValue>) childValue) {
                if (value.value != null) {
                    childTopic = value.value;
                    break;
                }
            }
        }
        return childTopic;
    }

    /**
     * Eliminates parent candidates which do not match the given child topic.
     *
     * @param   candidates      the parent candidates; non-matching candidates are removed in-place.
     * @param   childTopic      the child topic to check; may be null
     * @param   compDefUri      the comp def underlying the child topic
     */
    private void eliminateParentCandidates(List<? extends TopicModelImpl> candidates, TopicModelImpl childTopic,
                                                                                      String compDefUri) {
        CompDefModel compDef = compDef(compDefUri);
        Iterator<? extends TopicModelImpl> i = candidates.iterator();
        while (i.hasNext()) {
            TopicModelImpl parent = i.next();
            String assocTypeUri = compDef.getInstanceLevelAssocTypeUri();
            if (childTopic != null) {
                // TODO: assoc parents?
                AssocModelImpl assoc = al.getAssocBetweenTopicAndTopic(assocTypeUri, parent.id, childTopic.id, PARENT,
                    CHILD);
                if (assoc != null) {
                    // update memory
                    parent.getChildTopics().set(
                        compDefUri,
                        mf.newRelatedTopicModel(childTopic, assoc)
                    );
                } else {
                    // logger.info("### eliminate (assoc doesn't exist)");
                    i.remove();
                }
            } else {
                // TODO: assoc parents?
                if (!al.getTopicRelatedTopics(parent.id, assocTypeUri, PARENT, CHILD,
                        compDef.getChildTypeUri()).isEmpty()) {
                    // logger.info("### eliminate (children exist)");
                    i.remove();
                }
            }
        }
    }

    private void eliminateParentCandidates(List<? extends TopicModelImpl> candidates, List<UnifiedValue> childValues,
                                                                                      String compDefUri) {
        Iterator<? extends TopicModelImpl> i = candidates.iterator();
        while (i.hasNext()) {
            TopicModelImpl parent = i.next();
            parent.loadChildTopics(compDefUri, false);     // deep=false
            List<? extends TopicModel> childTopics = parent.getChildTopics().getTopics(compDefUri);
            if (!matches(childTopics, childValues)) {
                i.remove();
            }
        }
    }

    /**
     * @param   childValues     key: compDefUri
     *                          value: UnifiedValue or List<UnifiedValue>
     */
    private TopicModelImpl createCompositeTopic(Map<String, Object> childValues) {
        TopicModelImpl parent = al.createSingleTopic(
            mf.newTopicModel(newValues.uri, newValues.typeUri, newValues.value)
        );
        logger.info("### Creating composite " + parent.id + " (typeUri=\"" + type.uri + "\")");
        ChildTopicsModelImpl childTopics = parent.getChildTopics();
        for (String compDefUri : childValues.keySet()) {
            if (isOne(compDefUri)) {
                TopicModel childTopic = ((UnifiedValue<TopicModelImpl>) childValues.get(compDefUri)).value;
                if (childTopic != null) {
                    AssocModelImpl assoc = createChildAssoc(parent, childTopic, compDefUri);         // update DB
                    childTopics.set(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));        // update memory
                }
            } else {
                for (UnifiedValue<TopicModelImpl> value : (List<UnifiedValue>) childValues.get(compDefUri)) {
                    TopicModel childTopic = value.value;
                    if (childTopic != null) {
                        AssocModelImpl assoc = createChildAssoc(parent, childTopic, compDefUri);     // update DB
                        childTopics.add(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));    // update memory
                    }
                }
            }
        }
        return parent;
    }

    // --- DB Access ---

    /**
     * Convenience
     */
    private AssocModelImpl createChildAssoc(DMXObjectModel parent, DMXObjectModel child, String compDefUri) {
        return createChildAssoc(parent, child, compDefUri, false);
    }

    private AssocModelImpl createChildAssoc(DMXObjectModel parent, DMXObjectModel child, String compDefUri,
                                            boolean deleted) {
        logger.fine("### " + (deleted ? "Reassigning" : "Assigning") + " child " + child.getId() + " (compDefUri=\"" +
            compDefUri + "\") to composite " + parent.getId() + " (typeUri=\"" + type.uri + "\")");
        return al.createAssoc(compDef(compDefUri).getInstanceLevelAssocTypeUri(),
            parent.createPlayerModel(PARENT),
            child.createPlayerModel(CHILD)
        );
    }

    // --- Memory Access ---

    // TODO: make generic utility
    private RelatedTopicModelImpl findTopic(List<RelatedTopicModelImpl> topics, long topicId) {
        RelatedTopicModelImpl topic = findTopicOrNull(topics, topicId);
        if (topic == null) {
            throw new RuntimeException("Topic " + topicId + " not found in " + topics);
        }
        return topic;
    }

    // TODO: make generic utility
    private RelatedTopicModelImpl findTopicOrNull(List<RelatedTopicModelImpl> topics, long topicId) {
        for (RelatedTopicModelImpl topic : topics) {
            if (topic.id == topicId) {
                return topic;
            }
        }
        return null;
    }

    private void removeTopic(List<RelatedTopicModelImpl> topics, long topicId) {
        Iterator<RelatedTopicModelImpl> i = topics.iterator();
        while (i.hasNext()) {
            RelatedTopicModelImpl topic = i.next();
            if (topic.id == topicId) {
                i.remove();
                return;
            }
        }
        throw new RuntimeException("Topic " + topicId + " not found in " + topics);
    }

    /**
     * Checks whether the 1st list contains the same topics as represented by the 2nd list of values,
     * based on topic ID, regardless of order. Only non-empty values are compared.
     */
    private static boolean matches(List<? extends TopicModel> topics, List<UnifiedValue> values) {
        int valueCount = 0;
        for (UnifiedValue value : values) {
            if (value.value != null) {
                if (!topics.contains(value)) {
                    return false;
                }
                valueCount++;
            }
        }
        return topics.size() == valueCount;
    }

    // ---

    private CompDefModel compDef(String compDefUri) {
        if (!isFacetUpdate) {
            return type.getCompDef(compDefUri);
        } else {
            // sanity check
            if (!compDefUri.equals(compDef.getCompDefUri())) {
                throw new RuntimeException("URI mismatch: compDefUri=\"" + compDefUri + "\", facet compDefUri=\"" +
                    compDef.getCompDefUri() + "\"");
            }
            //
            return compDef;
        }
    }

    private boolean isOne(String compDefUri) {
        return compDef(compDefUri).getChildCardinalityUri().equals(ONE);
    }

    private boolean isValueType() {
        return type.getDataTypeUri().equals(VALUE);
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    class UnifiedValue<M extends DMXObjectModelImpl> implements JSONEnabled {

        M value;                            // the resulting unified value; may be null
        DMXObjectModelImpl _newValues;      // the original new values
        long originalId;                    // the original ID, saved here cause it is overwritten (see integrate())

        /**
         * @param   value   may be null
         */
        private UnifiedValue(M value) {
            this.value = value;
            this._newValues = newValues;
            this.originalId = newValues.id;
        }

        private UnifiedValue(M value, long originalId) {
            this.value = value;
            this._newValues = newValues;
            this.originalId = originalId;
        }

        @Override
        public JSONObject toJSON() {
            try {
                return new JSONObject()
                    .put("value", value != null ? value.toJSON() : null)
                    .put("originalId", originalId);
            } catch (Exception e) {
                throw new RuntimeException("Serialization failed", e);
            }
        }

        // ---

        // Note: we compare only to object models
        @Override
        public boolean equals(Object o) {
            return ((DMXObjectModelImpl) o).id == value.id;
        }

        // TODO
        /* @Override
        public int hashCode() {
            return ((Long) id).hashCode();
        } */

        // TODO: copy in DMXObjectModelImpl
        @Override
        public String toString() {
            try {
                return getClass().getSimpleName() + " " + toJSON().toString(4);
            } catch (Exception e) {
                throw new RuntimeException("Prettyprinting failed", e);
            }
        }
    }

    private static class Result {

        private DMXObjectModelImpl value;
        private boolean created;

        private Result() {
            this(null);
        }

        private Result(DMXObjectModelImpl value) {
            this(value, false);
        }

        private Result(DMXObjectModelImpl value, boolean created) {
            this.value = value;
            this.created = created;
        }
    }
}
