package systems.dmx.core.impl;

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

    // For composites: comp def URIs of empty child topics.
    // Evaluated when deleting child-assignments, see updateAssignments().
    // Not having null entries in the unified child topics simplifies candidate determination.
    // ### TODO: to be dropped?
    private List<String> emptyValues = new ArrayList();

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueIntegrator(PersistenceLayer pl) {
        this.pl = pl;
        this.mf = pl.mf;
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
            // argument check
            if (newValues.getTypeUri() == null) {
                throw new IllegalArgumentException("Tried to integrate values whose typeUri is not set, newValues=" +
                    newValues + ", targetObject=" + targetObject);
            }
            // Note: we must get type *after* processing refs. Refs might have no type set.
            this.type = newValues.getType();
            //
            // value integration
            // Note: because a facet type is composite by definition a facet update is always a composite operation,
            // even if the faceted object is a simple one.
            DMXObjectModelImpl _value = !isFacetUpdate && newValues.isSimple() ? integrateSimple() :
                                                                                 integrateComposite();
            // Note: UnifiedValue instantiation saves the new value's ID *before* it is overwritten
            UnifiedValue value = new UnifiedValue(_value);
            //
            idTransfer(_value);
            //
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

    // Simple

    /**
     * Integrates a simple value into the DB and returns the unified simple value.
     *
     * Preconditions:
     *   - this.newValues is not null
     *   - this.newValues is simple
     *
     * @return  the unified value, or null if there was nothing to integrate.
     *          The latter is the case if this.newValues is the empty string.
     */
    private DMXObjectModelImpl integrateSimple() {
        if (isAssoc || isType) {
            // Note 1: an assoc's simple value is not unified. In contrast to a topic an assoc can't be unified with
            // another assoc. (Even if 2 assocs have the same type and value they are not the same as they still
            // have different players.) An assoc's simple value is updated in-place.
            // Note 2: a type's simple value is not unified. A type is updated in-place.
            return storeAssocSimpleValue();
        } else if (newValues.getSimpleValue().toString().isEmpty()) {
            return null;
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
     * Finds a simple topic by-value in the DB, or creates it.
     *
     * Preconditions:
     *   - this.newValues is simple
     *   - this.newValues is not empty
     *
     * @return  the unified value; never null.
     */
    private TopicModelImpl unifySimple() {
        SimpleValue newValue = newValues.getSimpleValue();
        // FIXME: HTML values must be tag-stripped before lookup, complementary to indexing
        TopicImpl _topic = pl.getTopicByValue(type.getUri(), newValue);     // TODO: let pl return models
        TopicModelImpl topic = _topic != null ? _topic.getModel() : null;   // TODO: drop
        if (topic != null) {
            logger.fine("Reusing simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri + "\")");
        } else {
            topic = createSimpleTopic();
            logger.fine("### Creating simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri +
                "\")");
        }
        return topic;
    }

    // Composite

    /**
     * Integrates a composite value into the DB and returns the unified composite value.
     *
     * Preconditions:
     *   - this.newValues is composite
     *
     * @return  the unified value, or null if there was nothing to integrate.
     */
    private DMXObjectModelImpl integrateComposite() {
        Map<String, Object> childValues = new HashMap();    // value: UnifiedValue or List<UnifiedValue>
        ChildTopicsModel _childTopics = newValues.getChildTopicsModel();
        // Iterate through type, not through newValues.
        // newValues might contain childs not contained in the type def, e.g. "dmx.timestamps.modified".
        for (String compDefUri : compDefUris()) {
            Object newChildValue;    // RelatedTopicModelImpl or List<RelatedTopicModelImpl>
            if (isOne(compDefUri)) {
                newChildValue = _childTopics.getTopicOrNull(compDefUri);
            } else {
                // TODO: if empty?
                newChildValue = _childTopics.getTopicsOrNull(compDefUri);
            }
            // skip if not contained in update request
            if (newChildValue == null) {
                continue;
            }
            //
            Object childTopic = integrateChildValue(newChildValue, compDefUri);
            // childTopic: UnifiedValue or List<UnifiedValue>; never null
            if (isOne(compDefUri) && ((UnifiedValue) childTopic).value == null) {
                emptyValues.add(compDefUri);
            } else {
                childValues.put(compDefUri, childTopic);
            }
        }
        DMXObjectModelImpl value = unifyComposite(childValues);
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
        return value;
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
            return new ValueIntegrator(pl).integrate((RelatedTopicModelImpl) childValue, null, null);
        } else {
            List<UnifiedValue> values = new ArrayList();
            for (RelatedTopicModelImpl value : (List<RelatedTopicModelImpl>) childValue) {
                values.add(new ValueIntegrator(pl).integrate(value, null, null));
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
     * @param   childValues     value: UnifiedValue or List<UnifiedValue>
     *
     * @return  the unified value, or null if there was nothing to integrate.
     */
    private DMXObjectModelImpl unifyComposite(Map<String, Object> childValues) {
        // Note: because a facet does not contribute to the value of a value object
        // a facet update is always an in-place modification
        if (isValueType() && !isFacetUpdate) {
            return !childValues.isEmpty() ? unifyChildTopics(childValues, type) : null;
        } else {
            DMXObjectModelImpl parent = identifyParent(childValues);
            return parent != null ? updateAssignments(parent, childValues) : null;
        }
    }

    /**
     * Identifies the parent to be updated in-place, or creates it.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - this.type is an identity type OR this is a facet update
     *
     * @param   childValues     value: UnifiedValue or List<UnifiedValue>
     *
     * @return  the parent object, or null if there is no parent
     */
    private DMXObjectModelImpl identifyParent(Map<String, Object> childValues) {
        // TODO: 1st check identity attrs THEN target object?? => NO!
        if (targetObject != null) {
            return targetObject;
        } else if (isAssoc) {
            if (newValues.id == -1) {
                throw new RuntimeException("newValues has no ID set");
            }
            // TODO: partial updates. URI and role models must not expected to be part of the update model.
            AssocModelImpl _newValues = (AssocModelImpl) newValues;
            return mf.newAssocModel(newValues.id, newValues.uri, newValues.typeUri, _newValues.player1,
                                                                                    _newValues.player2);
        } else {
            List<String> identityCompDefUris = type.getIdentityAttrs();
            if (identityCompDefUris.size() > 0) {
                return !childValues.isEmpty() ?
                    unifyChildTopics(identityChildTopics(childValues, identityCompDefUris), identityCompDefUris) : null;
            } else {
                // FIXME: when the POST_CREATE_TOPIC event is fired, the child topics should exist already.
                // Note: for value-types this is fixed meanwhile, but not for identity-types.
                DMXObjectModelImpl parent = createSimpleTopic();
                logger.fine("### Creating composite (w/o identity attrs) " + parent.id + " (typeUri=\"" + type.uri +
                    "\")");
                return parent;
            }
        }
    }

    /**
     * From the given child topics selects these ones which made up the parent topic's identity.
     *
     * @param   childValues             the map of the child topics to select from; not empty
     *                                      key: compDefUri
     *                                      value: UnifiedValue or List<UnifiedValue>
     * @param   identityCompDefUris     not empty
     *
     * @return  A map of the identity child topics; not empty
     *              key: compDefUri
     *              value: UnifiedValue
     */
    private Map<String, Object> identityChildTopics(Map<String, Object> childValues,
                                                    List<String> identityCompDefUris) {
        try {
            Map<String, Object> identityChildTopics = new HashMap();
            for (String compDefUri : identityCompDefUris) {
                if (!isOne(compDefUri)) {
                    throw new RuntimeException("Cardinality \"many\" identity attributes not supported");
                }
                UnifiedValue childTopic = (UnifiedValue) childValues.get(compDefUri);
                if (childTopic == null) {
                    throw new RuntimeException("Identity value \"" + compDefUri + "\" is missing in " +
                        childValues.keySet() + " (childTopic=null)");
                }
                if (childTopic.value == null) {
                    // FIXME: can this happen?
                    throw new RuntimeException("Identity value \"" + compDefUri + "\" is missing in " +
                        childValues.keySet() + " (childTopic.value=null)");
                }
                identityChildTopics.put(compDefUri, childTopic);
            }
            // logger.fine("### type=\"" + type.uri + "\" ### identityChildTopics=" + identityChildTopics);
            return identityChildTopics;
        } catch (Exception e) {
            throw new RuntimeException("Selecting identity childs " + identityCompDefUris + " failed, childValues=" +
                childValues, e);
        }
    }

    /**
     * Updates a parent's child assignments in-place.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - this.type is an identity type OR this is a facet update
     *   - parent's type is this.type
     *   - compDef's parent type is this.type
     *   - newChildTopic's type is compDef's child type
     *
     * @param   parent          to parent to be updated; not null
     * @param   childValues     value: UnifiedValue or List<UnifiedValue>
     *
     * @return  the updated parent (passed through)
     */
    private DMXObjectModelImpl updateAssignments(DMXObjectModelImpl parent, Map<String, Object> childValues) {
        // sanity check
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: newValues type=\"" + type.getUri() + "\", parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        //
        for (String compDefUri : compDefUris()) {
            // TODO: possible optimization: load only ONE child level here (deep=false). Later on, when updating the
            // assignments, load the remaining levels only IF the assignment did not change. In contrast if the
            // assignment changes, a new subtree is attached. The subtree is fully constructed already (through all
            // levels) as it is build bottom-up (starting from the simple values at the leaves).
            parent.loadChildTopics(compDef(compDefUri), true);    // deep=true
            Object childTopic = childValues.get(compDefUri);
            if (isOne(compDefUri)) {
                TopicModel _childTopic = (TopicModel) (childTopic != null ? ((UnifiedValue) childTopic).value : null);
                updateAssignmentsOne(parent, _childTopic, compDefUri);
            } else {
                // Note: for partial create/update requests childTopic might be null
                if (childTopic != null) {
                    updateAssignmentsMany(parent, (List<UnifiedValue>) childTopic, compDefUri);
                }
            }
        }
        return parent;
    }

    /**
     * @param   childTopic    may be null
     */
    private void updateAssignmentsOne(DMXObjectModelImpl parent, TopicModel childTopic, String compDefUri) {
        try {
            ChildTopicsModelImpl oldChildTopics = parent.getChildTopicsModel();
            RelatedTopicModelImpl oldValue = oldChildTopics.getTopicOrNull(compDefUri);   // may be null
            if (oldValue != null && oldValue.id == -1) {
                throw new RuntimeException("Old value's ID is not initialized, oldValue=" + oldValue);
            }
            boolean newValueIsEmpty = isEmptyValue(compDefUri);
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
                oldChildTopics.put(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
            }
            // 3) update relating assoc
            //
            // take the old assoc if no new one is created, there is an old one, and it has not been deleted
            if (assoc == null && oldValue != null && !deleted) {
                assoc = oldValue.getRelatingAssoc();
            }
            if (assoc != null) {
                RelatedTopicModelImpl newChildValue = newValues.getChildTopicsModel().getTopicOrNull(compDefUri);
                updateRelatingAssoc(assoc, compDefUri, newChildValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating assigment failed, parent=" + parent + ", childTopic=" + childTopic +
                ", compDefUri=\"" + compDefUri + "\"", e);
        }
    }

    /**
     * @param   childValues   never null; a UnifiedValue's "value" field may be null
     */
    private void updateAssignmentsMany(DMXObjectModelImpl parent, List<UnifiedValue> childValues, String compDefUri) {
        ChildTopicsModelImpl oldChildTopics = parent.getChildTopicsModel();
        List<RelatedTopicModelImpl> oldValues = oldChildTopics.getTopicsOrNull(compDefUri);   // may be null
        for (UnifiedValue childValue : childValues) {
            TopicModel childTopic = (TopicModel) childValue.value;
            long originalId = childValue.originalId;
            long newId = childTopic != null ? childTopic.getId() : -1;
            RelatedTopicModelImpl oldValue = null;
            if (originalId != -1) {
                if (oldValues == null) {
                    throw new RuntimeException("Tried to replace original topic " + originalId +
                        " when there are no old topics (null)");
                }
                oldValue = findTopic(oldValues, originalId);
            }
            //
            // 1) delete assignment if exists AND value has changed or emptied
            //
            boolean deleted = false;
            if (originalId != -1 && (newId == -1 || originalId != newId)) {
                if (newId == -1) {
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
            if (newId != -1 && (originalId == -1 || originalId != newId)) {
                // update DB
                assoc = createChildAssoc(parent, childTopic, compDefUri, deleted);
                // update memory
                oldChildTopics.add(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
            }
            // 3) update relating assoc
            //
            // take the old assoc if no new one is created, there is an old one, and it has not been deleted
            if (assoc == null && oldValue != null && !deleted) {
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
                // Note: the roles must be suppressed from being updated. Update would fail if a new child has
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
                    // pl.updateAssoc(assoc, _newValues);
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
     * @return  the found (or created) parent topic; never null.
     */
    private DMXObjectModelImpl unifyChildTopics(Map<String, Object> childValues, Iterable<String> compDefUris) {
        List<? extends TopicModelImpl> candidates = parentCandidates(childValues);
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
        switch (candidates.size()) {
        case 0:
            // logger.info("### no composite found, childValues=" + childValues);
            return createCompositeTopic(childValues);
        case 1:
            DMXObjectModelImpl comp = candidates.get(0);
            // logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + type.uri + "\")");
            return comp;
        default:
            throw new RuntimeException("ValueIntegrator ambiguity: there are " + candidates.size() +
                " parents (typeUri=\"" + type.uri + "\", " + DMXUtils.idList(candidates) +
                ") which have the same " + childValues.values().size() + " child topics " + childValues.values());
        }
    }

    /**
     * Determine parent topics ("candidates") which might match the given child topics.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - compDef's parent type is this.type
     *   - childTopic's type is compDef's child type
     *
     * @param   childValues     Child topics to unify; not empty
     *                              key: compDefUri
     *                              value: UnifiedValue or List<UnifiedValue>
     */
    private List<? extends TopicModelImpl> parentCandidates(Map<String, Object> childValues) {
        // TODO: drop "emptyValues" and search for the first non-null value.value
        String compDefUri = childValues.keySet().iterator().next();
        // logger.fine("### compDefUri=\"" + compDefUri + "\", childValues=" + childValues);
        // sanity check
        if (!type.getUri().equals(compDef(compDefUri).getParentTypeUri())) {
            throw new RuntimeException("Type mismatch: type=\"" + type.getUri() + "\", comp def's parent type=\"" +
                compDef(compDefUri).getParentTypeUri() + "\"");
        }
        //
        DMXObjectModel childTopic;
        if (isOne(compDefUri)) {
            childTopic = ((UnifiedValue) childValues.get(compDefUri)).value;
        } else {
            childTopic = ((List<UnifiedValue>) childValues.get(compDefUri)).get(0).value;
        }
        return pl.getTopicRelatedTopics(childTopic.getId(), compDef(compDefUri).getInstanceLevelAssocTypeUri(),
            "dmx.core.child", "dmx.core.parent", type.getUri());
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
                AssocImpl assoc = pl.getAssoc(assocTypeUri, parent.id, childTopic.id, "dmx.core.parent",
                    "dmx.core.child");
                if (assoc != null) {
                    // update memory
                    parent.getChildTopicsModel().put(
                        compDefUri,
                        mf.newRelatedTopicModel(childTopic, assoc.getModel())
                    );
                } else {
                    // logger.info("### eliminate (assoc doesn't exist)");
                    i.remove();
                }
            } else {
                // TODO: assoc parents?
                if (!pl.getTopicRelatedTopics(parent.id, assocTypeUri, "dmx.core.parent", "dmx.core.child",
                        compDef.getChildTypeUri()).isEmpty()) {
                    // logger.info("### eliminate (childs exist)");
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
            List<? extends TopicModel> childTopics = parent.getChildTopicsModel().getTopics(compDefUri);
            if (!matches(childTopics, childValues)) {
                i.remove();
            }
        }
    }

    /**
     * @param   childValues     value: UnifiedValue or List<UnifiedValue>
     */
    private TopicModelImpl createCompositeTopic(Map<String, Object> childValues) {
        TopicModelImpl model = mf.newTopicModel(newValues.uri, newValues.typeUri, newValues.value);
        ChildTopicsModelImpl childTopics = model.getChildTopicsModel();
        TopicImpl topic = pl.createSingleTopic(model, false);       // firePostCreate=false
        logger.info("### Creating composite " + model.id + " (typeUri=\"" + type.uri + "\")");
        for (String compDefUri : childValues.keySet()) {
            if (isOne(compDefUri)) {
                TopicModel childTopic = ((UnifiedValue<TopicModelImpl>) childValues.get(compDefUri)).value;
                // update DB
                AssocModelImpl assoc = createChildAssoc(model, childTopic, compDefUri);
                // update memory
                childTopics.put(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
            } else {
                for (UnifiedValue<TopicModelImpl> value : (List<UnifiedValue>) childValues.get(compDefUri)) {
                    TopicModel childTopic = value.value;
                    // update DB
                    AssocModelImpl assoc = createChildAssoc(model, childTopic, compDefUri);
                    // update memory
                    childTopics.add(compDefUri, mf.newRelatedTopicModel(childTopic, assoc));
                }
            }
        }
        pl.em.fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
        return model;
    }

    // --- DB Access ---

    /**
     * Preconditions:
     *   - this.newValues is a topic model.
     */
    private TopicModelImpl createSimpleTopic() {
        // sanity check
        if (isAssoc) {
            throw new RuntimeException("Tried to create a topic from an assoc model");
        }
        //
        TopicModelImpl model = mf.newTopicModel(newValues.uri, newValues.typeUri, newValues.value);
        pl.createSingleTopic(model, true);      // firePostCreate=true
        return model;
    }

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
        return pl.createAssoc(compDef(compDefUri).getInstanceLevelAssocTypeUri(),
            parent.createPlayerModel("dmx.core.parent"),
            child.createPlayerModel("dmx.core.child")
        ).getModel();
    }

    // --- Memory Access ---

    // TODO: make generic utility
    private RelatedTopicModelImpl findTopic(List<RelatedTopicModelImpl> topics, long topicId) {
        for (RelatedTopicModelImpl topic : topics) {
            if (topic.id == topicId) {
                return topic;
            }
        }
        throw new RuntimeException("Topic " + topicId + " not found in " + topics);
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
        return compDef(compDefUri).getChildCardinalityUri().equals("dmx.core.one");
    }

    private boolean isValueType() {
        return type.getDataTypeUri().equals("dmx.core.value");
    }

    private boolean isEmptyValue(String compDefUri) {
        return emptyValues.contains(compDefUri);
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
}
