package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.util.DeepaMehtaUtils;

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

    private DeepaMehtaObjectModelImpl newValues;
    private DeepaMehtaObjectModelImpl targetObject;    // may null
    private TypeModelImpl type;
    private boolean isAssoc;

    // For composites: assoc def URIs of empty child topics.
    // Evaluated when deleting child-assignments, see updateChildRefs().
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
    UnifiedValue integrate(DeepaMehtaObjectModelImpl newValues, DeepaMehtaObjectModelImpl targetObject) {
        // logger.info("##### newValues=" + newValues + " ### targetObject=" + targetObject);
        long originalId = newValues.id;
        // resolve ref
        if (newValues instanceof TopicReferenceModelImpl) {
            TopicReferenceModelImpl ref = (TopicReferenceModelImpl) newValues;
            if (!ref.isEmptyRef()) {
                DeepaMehtaObjectModelImpl object = ref.resolve();
                logger.info("Referencing " + object);
                return new UnifiedValue(object, originalId);
            } else {
                return new UnifiedValue(null, originalId);
            }
        }
        // argument check
        if (newValues.getTypeUri() == null) {
            throw new IllegalArgumentException("Tried to integrate values whose typeUri is not set, newValues=" +
                newValues + ", targetObject=" + targetObject);
        }
        //
        this.newValues = newValues;
        this.targetObject = targetObject;
        this.type = newValues.getType();
        this.isAssoc = newValues instanceof AssociationModel;
        //
        // value integration
        //
        DeepaMehtaObjectModelImpl value;
        if (newValues.isSimple()) {
            value = integrateSimple();
        } else {
            value = integrateComposite();
            // label calculation
            if (value != null) {
                new LabelCalculation(value).calculate();
            } else if (isAssoc) {
                storeAssocSimpleValue();
            }
        }
        // ID transfer ### TODO: drop it?
        if (value != null) {
            if (value.id == -1) {
                throw new RuntimeException("Unification result has no ID set");
            }
            newValues.id = value.id;
        }
        //
        return new UnifiedValue(value, originalId);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Preconditions:
     *   - this.newValues is not null
     *   - this.newValues is simple
     *
     * @return  the unified value, or null if there was nothing to integrate.
     *          The latter is the case if this.newValues is the empty string.
     */
    private DeepaMehtaObjectModelImpl integrateSimple() {
        if (isAssoc) {
            // Note: an assoc's simple value is not unified. In contrast to a topic an assoc can't be unified with
            // another assoc. (Even if 2 assocs have the same type and value they are not the same as they still have
            // different players.) An assoc's simple value is updated in-place.
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
    private DeepaMehtaObjectModelImpl storeAssocSimpleValue() {
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

    /**
     * Integrates a composite value into the DB and returns the unified composite value.
     *
     * Preconditions:
     *   - this.newValues is composite
     *
     * @return  the unified value, or null if there was nothing to integrate.
     */
    private DeepaMehtaObjectModelImpl integrateComposite() {
        try {
            Map<String, Object> childTopics = new HashMap();    // value: UnifiedValue or List<UnifiedValue>
            ChildTopicsModel _childTopics = newValues.getChildTopicsModel();
            // Iterate through type, not through newValues.
            // newValues might contain childs not contained in the type def, e.g. "dm4.time.modified".
            for (String assocDefUri : type) {
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
                    // TODO: many?
                    emptyValues.add(assocDefUri);
                } else {
                    childTopics.put(assocDefUri, childTopic);
                }
            }
            return unifyComposite(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Integrating a composite value failed, newValues=" + newValues, e);
        }
    }

    /**
     * Invokes a ValueIntegrator for a child value.
     *
     * @param   childValue      RelatedTopicModelImpl or List<RelatedTopicModelImpl>
     *
     * @return  UnifiedValue or List<UnifiedValue>; never null;
     */
    private Object integrateChildValue(Object childValue, String assocDefUri) {
        if (isOne(assocDefUri)) {
            return new ValueIntegrator(pl).integrate((RelatedTopicModelImpl) childValue, null); // targetObject=null
        } else {
            List<UnifiedValue> values = new ArrayList();
            for (RelatedTopicModelImpl value : (List<RelatedTopicModelImpl>) childValue) {
                values.add(new ValueIntegrator(pl).integrate(value, null));                     // targetObject=null
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
    private DeepaMehtaObjectModelImpl unifyComposite(Map<String, Object> childTopics) {
        if (isValueType()) {
            // TODO: update relating assoc values?
            return !childTopics.isEmpty() ? unifyChildTopics(childTopics, type) : null;
        } else {
            return updateChildRefs(identifyParent(childTopics), childTopics);
        }
    }

    /**
     * @param   childTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private DeepaMehtaObjectModelImpl identifyParent(Map<String, Object> childTopics) {
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
                DeepaMehtaObjectModelImpl parent = createSimpleTopic();
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
     * @param   newChildTopics     value: UnifiedValue or List<UnifiedValue>
     */
    private DeepaMehtaObjectModelImpl updateChildRefs(DeepaMehtaObjectModelImpl parent,
                                                      Map<String, Object> newChildTopics) {
        // sanity check
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: integrator type=\"" + type.getUri() + "\" vs. parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        //
        for (String assocDefUri : type) {
            parent.loadChildTopics(assocDefUri);    // TODO: load only one level deep?
            Object newValue = newChildTopics.get(assocDefUri);
            if (isOne(assocDefUri)) {
                TopicModel _newValue = (TopicModel) (newValue != null ? ((UnifiedValue) newValue).value : null);
                updateChildRefsOne(parent, _newValue, assocDefUri);
            } else {
                updateChildRefsMany(parent, (List<UnifiedValue>) newValue, assocDefUri);
            }
        }
        return parent;
    }

    /**
     * @param   newValue    may be null
     */
    private void updateChildRefsOne(DeepaMehtaObjectModelImpl parent, TopicModel newValue, String assocDefUri) {
        ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
        RelatedTopicModelImpl oldValue = childTopics.getTopicOrNull(assocDefUri);   // may be null
        boolean newValueIsEmpty = isEmptyValue(assocDefUri);
        //
        // 1) delete assignment if exists AND value has changed or emptied
        //
        boolean deleted = false;
        if (oldValue != null && (newValueIsEmpty || newValue != null && !oldValue.equals(newValue))) {
            // update DB
            oldValue.getRelatingAssociation().delete();
            // update memory
            if (newValueIsEmpty) {
                logger.info("### Delete assignment (assocDefUri=\"" + assocDefUri + "\") from composite " +
                    parent.getId() + " (typeUri=\"" + type.uri + "\")");
                childTopics.remove(assocDefUri);
            }
            deleted = true;
        }
        // 2) create assignment if not exists OR value has changed
        // a new value must be present
        //
        AssociationModelImpl assoc = null;
        if (newValue != null && (oldValue == null || !oldValue.equals(newValue))) {
            // update DB
            assoc = createChildAssociation(parent, newValue, assocDefUri, deleted ? "Reassigning" : "Assigning");
            // update memory
            childTopics.put(assocDefUri, mf.newRelatedTopicModel(newValue, assoc));
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
                updateRelatingAssociation(assoc, assocDefUri);
            }
        }
    }

    /**
     * @param   newValues   never null; a UnifiedValue's "value" field may be null
     */
    private void updateChildRefsMany(DeepaMehtaObjectModelImpl parent, List<UnifiedValue> newValues,
                                                                       String assocDefUri) {
        ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
        List<RelatedTopicModelImpl> oldValues = childTopics.getTopicsOrNull(assocDefUri);   // may be null
        // logger.info("### assocDefUri=\"" + assocDefUri + "\", oldValues=" + oldValues);
        for (UnifiedValue _newValue : newValues) {
            TopicModel newValue = (TopicModel) _newValue.value;
            long originalId = _newValue.originalId;
            long newId = newValue != null ? newValue.getId() : -1;
            //
            // 1) delete assignment if exists AND value has changed or emptied
            //
            boolean deleted = false;
            if (originalId != -1 && (newId == -1 || originalId != newId)) {
                if (newId == -1) {
                    logger.info("### Delete assignment (assocDefUri=\"" + assocDefUri + "\") from composite " +
                        parent.getId() + " (typeUri=\"" + type.uri + "\")");
                }
                deleted = true;
                // update DB
                findTopic(oldValues, originalId).getRelatingAssociation().delete();
                // update memory
                removeTopic(oldValues, originalId);
            }
            // 2) create assignment if not exists OR value has changed
            // a new value must be present
            //
            AssociationModelImpl assoc;
            if (newId != -1 && (originalId == -1 || originalId != newId)) {
                // update DB
                assoc = createChildAssociation(parent, newValue, assocDefUri, deleted ? "Reassigning" : "Assigning");
                // update memory
                childTopics.add(assocDefUri, mf.newRelatedTopicModel(newValue, assoc));
            }
            // 3) update relating assoc
            //
            // TODO
        }
    }

    private void updateRelatingAssociation(AssociationModelImpl assoc, String assocDefUri) {
        try {
            RelatedTopicModelImpl newChildValue = newValues.getChildTopicsModel().getTopicOrNull(assocDefUri);
            // Note: for partial create/update requests newChildValue might be null
            if (newChildValue != null) {
                AssociationModelImpl updateModel = newChildValue.getRelatingAssociation();
                // Note: the roles must be suppressed from being updated. Update would fail if a new child has
                // been assigned (step 2) because the player is another one then. Here we are only interested
                // in updating the assoc value.
                updateModel.setRoleModel1(null);
                updateModel.setRoleModel2(null);
                // Note: if no relating assocs are contained in a create/update request the model factory
                // creates assocs anyways, but these are completely uninitialized. ### TODO: Refactor
                // TODO: is condition needed? => yes, try create new topic
                if (updateModel.typeUri != null) {
                    assoc.update(updateModel);
                    // TODO: access control? Note: currently the child assocs of a workspace have no workspace
                    // assignments. With strict access control, updating a workspace topic would fail.
                    // pl.updateAssociation(assoc, updateModel);
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
    private DeepaMehtaObjectModelImpl unifyChildTopics(Map<String, Object> childTopics, Iterable<String> assocDefUris) {
        List<RelatedTopicModelImpl> candidates = parentCandidates(childTopics);
        // logger.info("### candidates (" + candidates.size() + "): " + DeepaMehtaUtils.idList(candidates));
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
            DeepaMehtaObjectModelImpl comp = candidates.get(0);
            logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + type.uri + "\")");
            return comp;
        default:
            throw new RuntimeException("Value Integrator Ambiguity: there are " + candidates.size() +
                " parents (typeUri=\"" + type.uri + "\", " + DeepaMehtaUtils.idList(candidates) +
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
        DeepaMehtaObjectModel childTopic;
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
    private void eliminateParentCandidates(List<RelatedTopicModelImpl> candidates, DeepaMehtaObjectModel childTopic,
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
                DeepaMehtaObjectModel childTopic = ((UnifiedValue) childTopics.get(assocDefUri)).value;
                createChildAssociation(topic, childTopic, assocDefUri);
            } else {
                for (UnifiedValue value : (List<UnifiedValue>) childTopics.get(assocDefUri)) {
                    createChildAssociation(topic, value.value, assocDefUri);
                }
            }
        }
        return topic;
    }

    private AssociationModelImpl createChildAssociation(DeepaMehtaObjectModel parent, DeepaMehtaObjectModel child,
                                                                                      String assocDefUri) {
        return createChildAssociation(parent, child, assocDefUri, "Assigning");
    }

    private AssociationModelImpl createChildAssociation(DeepaMehtaObjectModel parent, DeepaMehtaObjectModel child,
                                                                                      String assocDefUri, String info) {
        logger.info("### " + info + " child " + child.getId() + " (assocDefUri=\"" + assocDefUri +
            "\") to composite " + parent.getId() + " (typeUri=\"" + type.uri + "\")");
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
        return type.getAssocDef(assocDefUri);
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

    class UnifiedValue {

        DeepaMehtaObjectModelImpl value;
        long originalId;

        /**
         * @param   value   may be null
         */
        private UnifiedValue(DeepaMehtaObjectModelImpl value, long originalId) {
            this.value = value;
            this.originalId = originalId;
        }
    }
}
