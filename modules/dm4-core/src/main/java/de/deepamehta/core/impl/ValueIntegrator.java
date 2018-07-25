package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DMXObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.util.DMXUtils;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Integrates new values into the DB.
 */
class ValueIntegrator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DMXObjectModelImpl newValues;
    private DMXObjectModelImpl targetObject;        // may null
    private AssociationDefinitionModel assocDef;    // may null
    private TypeModelImpl type;
    private boolean isAssoc;
    private boolean isType;
    private boolean isFacetUpdate;

    // For composites: assoc def URIs of empty child topics.
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
     * @return  the unified value; never null; its "value" field is null if there was nothing to integrate.
     */
    <M extends DMXObjectModelImpl> UnifiedValue<M> integrate(M newValues, M targetObject,
                                                             AssociationDefinitionModel assocDef) {
        this.newValues = newValues;
        this.targetObject = targetObject;
        this.assocDef = assocDef;
        this.isAssoc = newValues instanceof AssociationModel;
        this.isType  = newValues instanceof TypeModel;
        this.isFacetUpdate = assocDef != null;
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
        // integrate values
        // Note: because a facet type is composite by definition a facet update is a composite operation, even if the
        // faceted object is a simple one.
        DMXObjectModelImpl _value = !isFacetUpdate && newValues.isSimple() ? integrateSimple() : integrateComposite();
        //
        // Note: UnifiedValue instantiation saves the new value's ID *before* it is overwritten
        UnifiedValue value = new UnifiedValue(_value);
        //
        // ID transfer
        if (_value != null) {
            if (_value.id == -1) {
                throw new RuntimeException("Unification result has no ID set");
            }
            // Note: this is an ugly side effect, but we keep it for pragmatic reasons ### TODO: rethink
            newValues.id = _value.id;
        }
        //
        return value;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private UnifiedValue unifyRef() {
        TopicReferenceModelImpl ref = (TopicReferenceModelImpl) newValues;
        if (!ref.isEmptyRef()) {
            DMXObjectModelImpl object = ref.resolve();
            logger.info("Referencing " + object);
            return new UnifiedValue(object);
        } else {
            return new UnifiedValue(null);
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
        try {
            if (isAssoc || isType) {
                // Note 1: an assoc's simple value is not unified. In contrast to a topic an assoc can't be unified with
                // another assoc. (Even if 2 assocs have the same type and value they are not the same as they still have
                // different players.) An assoc's simple value is updated in-place.
                // Note 2: a type's simple value is not unified. A type is updated in-place.
                return storeAssocSimpleValue();
            } else if (newValues.getSimpleValue().toString().isEmpty()) {
                return null;
            } else {
                return unifySimple();
            }
        } catch (Exception e) {
            throw new RuntimeException("Integrating a simple value failed, newValues=" + newValues, e);
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
     * Preconditions:
     *   - this.newValues is simple
     *   - this.newValues is not empty
     *
     * @return  the unified value. Is never null.
     */
    private TopicModelImpl unifySimple() {
        SimpleValue newValue = newValues.getSimpleValue();
        // FIXME: HTML values must be tag-stripped before lookup, complementary to indexing
        TopicImpl _topic = pl.getTopicByValue(type.getUri(), newValue);     // TODO: let pl return models
        TopicModelImpl topic = _topic != null ? _topic.getModel() : null;   // TODO: drop
        if (topic != null) {
            logger.info("Reusing simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri + "\")");
        } else {
            topic = createSimpleTopic();
            logger.info("### Creating simple value " + topic.id + " \"" + newValue + "\" (typeUri=\"" + type.uri +
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
        try {
            Map<String, Object> childTopics = new HashMap();    // value: UnifiedValue or List<UnifiedValue>
            ChildTopicsModel _childTopics = newValues.getChildTopicsModel();
            // Iterate through type, not through newValues.
            // newValues might contain childs not contained in the type def, e.g. "dm4.time.modified".
            for (String assocDefUri : assocDefUris()) {
                Object newChildValue;    // RelatedTopicModelImpl or List<RelatedTopicModelImpl>
                if (isOne(assocDefUri)) {
                    newChildValue = _childTopics.getTopicOrNull(assocDefUri);
                } else {
                    // TODO: if empty?
                    newChildValue = _childTopics.getTopicsOrNull(assocDefUri);
                }
                // skip if not contained in update request
                if (newChildValue == null) {
                    continue;
                }
                //
                Object childTopic = integrateChildValue(newChildValue, assocDefUri);
                if (isOne(assocDefUri) && ((UnifiedValue) childTopic).value == null) {
                    emptyValues.add(assocDefUri);
                } else {
                    childTopics.put(assocDefUri, childTopic);
                }
            }
            DMXObjectModelImpl value = unifyComposite(childTopics);
            //
            // label calculation
            if (value != null) {
                new LabelCalculation(value).calculate();
            } else if (isAssoc) {
                storeAssocSimpleValue();
            }
            //
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Integrating a composite value failed, newValues=" + newValues, e);
        }
    }

    private Iterable<String> assocDefUris() {
        return isFacetUpdate ? asList(assocDef.getAssocDefUri()) : type;
    }

    /**
     * Integrates a child value into the DB and returns the unified value.
     *
     * @param   childValue      RelatedTopicModelImpl or List<RelatedTopicModelImpl>
     *
     * @return  UnifiedValue or List<UnifiedValue>; never null;
     */
    private Object integrateChildValue(Object childValue, String assocDefUri) {
        if (isOne(assocDefUri)) {
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
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     *
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private DMXObjectModelImpl unifyComposite(Map<String, Object> childTopics) {
        if (isValueType()) {
            return !childTopics.isEmpty() ? unifyChildTopics(childTopics, type) : null;
        } else {
            return updateAssignments(identifyParent(childTopics), childTopics);
        }
    }

    /**
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private DMXObjectModelImpl identifyParent(Map<String, Object> childTopics) {
        // TODO: 1st check identity attrs THEN target object?? => NO!
        if (targetObject != null) {
            return targetObject;
        } else if (isAssoc) {
            if (newValues.id == -1) {
                throw new RuntimeException("newValues has no ID set");
            }
            return mf.newAssociationModel(newValues.id, null, newValues.typeUri, null, null);
        } else {
            List<String> identityAssocDefUris = type.getIdentityAttrs();
            if (identityAssocDefUris.size() > 0) {
                return unifyChildTopics(identityChildTopics(childTopics, identityAssocDefUris), identityAssocDefUris);
            } else {
                DMXObjectModelImpl parent = createSimpleTopic();
                logger.info("### Creating composite (w/o identity attrs) " + parent.id + " (typeUri=\"" + type.uri +
                    "\")");
                return parent;
            }
        }
    }

    /**
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     *
     * @return  value: UnifiedValue or List<UnifiedValue>
     */
    private Map<String, Object> identityChildTopics(Map<String, Object> childTopics,
                                                    List<String> identityAssocDefUris) {
        Map<String, Object> identityChildTopics = new HashMap();
        for (String assocDefUri : identityAssocDefUris) {
            UnifiedValue childTopic;
            if (isOne(assocDefUri)) {
                childTopic = (UnifiedValue) childTopics.get(assocDefUri);
            } else {
                throw new RuntimeException("Cardinality \"many\" identity attributes not supported");
            }
            // FIXME: only throw if NO identity child topic is given.
            // If at least ONE is given it is sufficient.
            if (childTopic.value == null) {
                throw new RuntimeException("Identity child topic \"" + assocDefUri + "\" is missing in " +
                    childTopics.keySet());
            }
            identityChildTopics.put(assocDefUri, childTopic);
        }
        // logger.info("### type=\"" + type.uri + "\" ### identityChildTopics=" + identityChildTopics);
        return identityChildTopics;
    }

    /**
     * Updates a parent's child assignments in-place.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - this.type is an identity type
     *   - parent's type is this.type
     *   - assocDef's parent type is this.type
     *   - newChildTopic's type is assocDef's child type
     *
     * @param   unifiedChilds     value: UnifiedValue or List<UnifiedValue>
     */
    private DMXObjectModelImpl updateAssignments(DMXObjectModelImpl parent, Map<String, Object> unifiedChilds) {
        // sanity check
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: newValues type=\"" + type.getUri() + "\", parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        //
        for (String assocDefUri : assocDefUris()) {
            parent.loadChildTopics(assocDef(assocDefUri));    // TODO: load only one level deep
            Object unifiedChild = unifiedChilds.get(assocDefUri);
            if (isOne(assocDefUri)) {
                TopicModel _unifiedChild = (TopicModel) (unifiedChild != null ? ((UnifiedValue) unifiedChild).value :
                    null);
                updateAssignmentsOne(parent, _unifiedChild, assocDefUri);
            } else {
                // Note: for partial create/update requests unifiedChild might be null
                if (unifiedChild != null) {
                    updateAssignmentsMany(parent, (List<UnifiedValue>) unifiedChild, assocDefUri);
                }
            }
        }
        return parent;
    }

    /**
     * @param   unifiedChild    may be null
     */
    private void updateAssignmentsOne(DMXObjectModelImpl parent, TopicModel unifiedChild, String assocDefUri) {
        ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
        RelatedTopicModelImpl oldValue = childTopics.getTopicOrNull(assocDefUri);   // may be null
        boolean newValueIsEmpty = isEmptyValue(assocDefUri);
        //
        // 1) delete assignment if exists AND value has changed or emptied
        //
        boolean deleted = false;
        if (oldValue != null && (newValueIsEmpty || unifiedChild != null && !oldValue.equals(unifiedChild))) {
            // update DB
            oldValue.getRelatingAssociation().delete();
            // update memory
            if (newValueIsEmpty) {
                logger.info("### Deleting assignment (assocDefUri=\"" + assocDefUri + "\") from composite " +
                    parent.id + " (typeUri=\"" + type.uri + "\")");
                childTopics.remove(assocDefUri);
            }
            deleted = true;
        }
        // 2) create assignment if not exists OR value has changed
        // a new value must be present
        //
        AssociationModelImpl assoc = null;
        if (unifiedChild != null && (oldValue == null || !oldValue.equals(unifiedChild))) {
            // update DB
            assoc = createChildAssociation(parent, unifiedChild, assocDefUri, deleted);
            // update memory
            childTopics.put(assocDefUri, mf.newRelatedTopicModel(unifiedChild, assoc));
        }
        // 3) update relating assoc
        //
        // Note: don't update an assoc's relating assoc
        // TODO: condition needed? => yes, try remove child topic from rel assoc (e.g. "Phone Label")
        if (!isAssoc) {
            // take the old assoc if no new one is created, there is an old one, and it has not been deleted
            if (assoc == null && oldValue != null && !deleted) {
                assoc = oldValue.getRelatingAssociation();
            }
            if (assoc != null) {
                RelatedTopicModelImpl newChildValue = newValues.getChildTopicsModel().getTopicOrNull(assocDefUri);
                updateRelatingAssociation(assoc, assocDefUri, newChildValue);
            }
        }
    }

    /**
     * @param   unifiedChilds   never null; a UnifiedValue's "value" field may be null
     */
    private void updateAssignmentsMany(DMXObjectModelImpl parent, List<UnifiedValue> unifiedChilds,
                                                                  String assocDefUri) {
        ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
        List<RelatedTopicModelImpl> oldValues = childTopics.getTopicsOrNull(assocDefUri);   // may be null
        // logger.info("### assocDefUri=\"" + assocDefUri + "\", oldValues=" + oldValues);
        for (UnifiedValue _unifiedChild : unifiedChilds) {
            TopicModel unifiedChild = (TopicModel) _unifiedChild.value;
            long originalId = _unifiedChild.originalId;
            long newId = unifiedChild != null ? unifiedChild.getId() : -1;
            RelatedTopicModelImpl oldValue = null;
            if (originalId != -1) {
                oldValue = findTopic(oldValues, originalId);
            }
            //
            // 1) delete assignment if exists AND value has changed or emptied
            //
            boolean deleted = false;
            if (originalId != -1 && (newId == -1 || originalId != newId)) {
                if (newId == -1) {
                    logger.info("### Deleting assignment (assocDefUri=\"" + assocDefUri + "\") from composite " +
                        parent.id + " (typeUri=\"" + type.uri + "\")");
                }
                deleted = true;
                // update DB
                oldValue.getRelatingAssociation().delete();
                // update memory
                removeTopic(oldValues, originalId);
            }
            // 2) create assignment if not exists OR value has changed
            // a new value must be present
            //
            AssociationModelImpl assoc = null;
            if (newId != -1 && (originalId == -1 || originalId != newId)) {
                // update DB
                assoc = createChildAssociation(parent, unifiedChild, assocDefUri, deleted);
                // update memory
                childTopics.add(assocDefUri, mf.newRelatedTopicModel(unifiedChild, assoc));
            }
            // 3) update relating assoc
            //
            // Note: don't update an assoc's relating assoc
            // TODO: condition needed? => yes, try remove child topic from rel assoc (e.g. "Phone Label")
            if (!isAssoc) {
                // take the old assoc if no new one is created, there is an old one, and it has not been deleted
                if (assoc == null && oldValue != null && !deleted) {
                    assoc = oldValue.getRelatingAssociation();
                }
                if (assoc != null) {
                    RelatedTopicModelImpl newValues = (RelatedTopicModelImpl) _unifiedChild._newValues;
                    updateRelatingAssociation(assoc, assocDefUri, newValues);
                }
            }
        }
    }

    private void updateRelatingAssociation(AssociationModelImpl assoc, String assocDefUri,
                                           RelatedTopicModelImpl newValues) {
        try {
            // Note: for partial create/update requests newValues might be null
            if (newValues != null) {
                AssociationModelImpl _newValues = newValues.getRelatingAssociation();
                // Note: the roles must be suppressed from being updated. Update would fail if a new child has
                // been assigned (step 2) because the player is another one then. Here we are only interested
                // in updating the assoc value.
                _newValues.setRoleModel1(null);
                _newValues.setRoleModel2(null);
                // Note: if no relating assocs are contained in a create/update request the model factory
                // creates assocs anyways, but these are completely uninitialized. ### TODO: Refactor
                // TODO: is condition needed? => yes, try create new topic
                if (_newValues.typeUri != null) {
                    assoc.update(_newValues);
                    // TODO: access control? Note: currently the child assocs of a workspace have no workspace
                    // assignments. With strict access control, updating a workspace topic would fail.
                    // pl.updateAssociation(assoc, _newValues);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating relating assoc " + assoc.id + " (assocDefUri=\"" + assocDefUri +
                "\") failed, assoc=" + assoc, e);
        }
    }

    // ---

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     *   - childTopics map is not empty
     *
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private DMXObjectModelImpl unifyChildTopics(Map<String, Object> childTopics, Iterable<String> assocDefUris) {
        List<RelatedTopicModelImpl> candidates = parentCandidates(childTopics);
        // logger.info("### candidates (" + candidates.size() + "): " + DMXUtils.idList(candidates));
        for (String assocDefUri : assocDefUris) {
            UnifiedValue value = (UnifiedValue) childTopics.get(assocDefUri);
            eliminateParentCandidates(candidates, value != null ? value.value : null, assocDefUri);
            if (candidates.isEmpty()) {
                break;
            }
        }
        switch (candidates.size()) {
        case 0:
            // logger.info("### no composite found, childTopics=" + childTopics);
            return createCompositeTopic(childTopics);
        case 1:
            DMXObjectModelImpl comp = candidates.get(0);
            logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + type.uri + "\")");
            return comp;
        default:
            throw new RuntimeException("ValueIntegrator ambiguity: there are " + candidates.size() +
                " parents (typeUri=\"" + type.uri + "\", " + DMXUtils.idList(candidates) +
                ") which have the same " + childTopics.values().size() + " child topics " + childTopics.values());
        }
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     *   - childTopics map is not empty
     *
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private List<RelatedTopicModelImpl> parentCandidates(Map<String, Object> childTopics) {
        String assocDefUri = childTopics.keySet().iterator().next();
        // logger.info("### assocDefUri=\"" + assocDefUri + "\", childTopics=" + childTopics);
        // sanity check
        if (!type.getUri().equals(assocDef(assocDefUri).getParentTypeUri())) {
            throw new RuntimeException("Type mismatch");
        }
        //
        DMXObjectModel childTopic;
        if (isOne(assocDefUri)) {
            childTopic = ((UnifiedValue) childTopics.get(assocDefUri)).value;
        } else {
            throw new RuntimeException("Unification of cardinality \"many\" values not yet implemented");
        }
        return pl.getTopicRelatedTopics(childTopic.getId(), assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            "dm4.core.child", "dm4.core.parent", type.getUri());
    }

    /**
     * @param   childTopic      may be null
     */
    private void eliminateParentCandidates(List<RelatedTopicModelImpl> candidates, DMXObjectModel childTopic,
                                                                                   String assocDefUri) {
        AssociationDefinitionModel assocDef = assocDef(assocDefUri);
        Iterator<RelatedTopicModelImpl> i = candidates.iterator();
        while (i.hasNext()) {
            long parentId = i.next().getId();
            String assocTypeUri = assocDef.getInstanceLevelAssocTypeUri();
            if (childTopic != null) {
                // TODO: assoc parents?
                if (pl.getAssociation(assocTypeUri, parentId, childTopic.getId(), "dm4.core.parent", "dm4.core.child")
                        == null) {
                    // logger.info("### eliminate (assoc doesn't exist)");
                    i.remove();
                }
            } else {
                // TODO: assoc parents?
                if (!pl.getTopicRelatedTopics(parentId, assocTypeUri, "dm4.core.parent", "dm4.core.child",
                        assocDef.getChildTypeUri()).isEmpty()) {
                    // logger.info("### eliminate (childs exist)");
                    i.remove();
                }
            }
        }
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
        return pl._createTopic(mf.newTopicModel(newValues.uri, newValues.typeUri, newValues.value)).getModel();
    }

    /**
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private TopicModelImpl createCompositeTopic(Map<String, Object> childTopics) {
        // FIXME: construct the composite model first, then create topic as a whole. => NO! Endless recursion?
        // Otherwise the POST_CREATE_TOPIC event is fired too early, and e.g. Address topics get no geo coordinates.
        // logger.info("### childTopics=" + childTopics);
        TopicModelImpl topic = createSimpleTopic();
        logger.info("### Creating composite " + topic.id + " (typeUri=\"" + type.uri + "\")");
        for (String assocDefUri : childTopics.keySet()) {
            if (isOne(assocDefUri)) {
                DMXObjectModel childTopic = ((UnifiedValue) childTopics.get(assocDefUri)).value;
                createChildAssociation(topic, childTopic, assocDefUri);
            } else {
                for (UnifiedValue value : (List<UnifiedValue>) childTopics.get(assocDefUri)) {
                    createChildAssociation(topic, value.value, assocDefUri);
                }
            }
        }
        return topic;
    }

    private AssociationModelImpl createChildAssociation(DMXObjectModel parent, DMXObjectModel child,
                                                                               String assocDefUri) {
        return createChildAssociation(parent, child, assocDefUri, false);
    }

    private AssociationModelImpl createChildAssociation(DMXObjectModel parent, DMXObjectModel child,
                                                                               String assocDefUri, boolean deleted) {
        logger.info("### " + (deleted ? "Reassigning" : "Assigning") + " child " + child.getId() + " (assocDefUri=\"" +
            assocDefUri + "\") to composite " + parent.getId() + " (typeUri=\"" + type.uri + "\")");
        return pl.createAssociation(assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"),
            child.createRoleModel("dm4.core.child")
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

    // ---

    private AssociationDefinitionModel assocDef(String assocDefUri) {
        if (isFacetUpdate) {
            if (!assocDefUri.equals(assocDef.getAssocDefUri())) {
                throw new RuntimeException("URI mismatch: assocDefUri=\"" + assocDefUri + "\", facet assocDefUri=\"" +
                    assocDef.getAssocDefUri() + "\"");
            }
            return assocDef;
        } else {
            return type.getAssocDef(assocDefUri);
        }
    }

    private boolean isOne(String assocDefUri) {
        return assocDef(assocDefUri).getChildCardinalityUri().equals("dm4.core.one");
    }

    private boolean isValueType() {
        return type.getDataTypeUri().equals("dm4.core.value");
    }

    private boolean isEmptyValue(String assocDefUri) {
        return emptyValues.contains(assocDefUri);
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    class UnifiedValue<M extends DMXObjectModelImpl> {

        M value;                            // the resulting unified value
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
    }
}
