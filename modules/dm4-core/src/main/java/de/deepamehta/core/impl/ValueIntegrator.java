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
     * @return  the unified value, or null if there was nothing to integrate.
     */
    DeepaMehtaObjectModelImpl integrate(DeepaMehtaObjectModelImpl newValues, DeepaMehtaObjectModelImpl targetObject) {
        // logger.info("##### newValues=" + newValues + " ### targetObject=" + targetObject);
        // resolve ref
        if (newValues instanceof TopicReferenceModelImpl) {
            TopicReferenceModelImpl ref = (TopicReferenceModelImpl) newValues;
            if (!ref.isEmptyRef()) {
                DeepaMehtaObjectModelImpl object = ref.resolve();
                logger.info("Referencing " + object);
                return object;
            } else {
                return null;
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
        DeepaMehtaObjectModelImpl unified;
        if (newValues.isSimple()) {
            unified = integrateSimple();
        } else {
            unified = integrateComposite();
            //
            // label calculation
            if (unified != null) {
                new LabelCalculation(unified).calculate();
            } else if (isAssoc) {
                storeAssocSimpleValue();
            }
        }
        // ID transfer
        if (unified != null) {
            if (unified.id == -1) {
                throw new RuntimeException("Unification result has no ID set");
            }
            newValues.id = unified.id;
        }
        //
        return unified;
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
     * Preconditions:
     *   - this.newValues is composite
     *
     * @return  the unified value, or null if there was nothing to integrate.
     */
    private DeepaMehtaObjectModelImpl integrateComposite() {
        try {
            Map<String, TopicModel> childTopics = new HashMap();
            for (String assocDefUri : type) {
                if (assocDef(assocDefUri).getChildCardinalityUri().equals("dm4.core.many")) {
                    throw new RuntimeException("many cardinality not yet implemented");
                }
                RelatedTopicModelImpl newChildValue = newValues.getChildTopicsModel().getTopicOrNull(assocDefUri);
                // skip if not contained in update request
                if (newChildValue == null) {
                    continue;
                }
                //
                TopicModel childTopic = integrateChildValue(newChildValue);
                if (childTopic == null) {
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
     */
    private TopicModel integrateChildValue(RelatedTopicModelImpl newChildValue) {
        return (TopicModel) new ValueIntegrator(pl).integrate(newChildValue, null);     // targetObject=null
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     */
    private DeepaMehtaObjectModelImpl unifyComposite(Map<String, TopicModel> childTopics) {
        if (isValueType()) {
            // TODO: update relating assoc values?
            return !childTopics.isEmpty() ? unifyChildTopics(childTopics, type) : null;
        } else {
            return updateChildRefs(identifyParent(childTopics), childTopics);
        }
    }

    private DeepaMehtaObjectModelImpl identifyParent(Map<String, TopicModel> childTopics) {
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

    private Map<String, TopicModel> identityChildTopics(Map<String, TopicModel> childTopics,
                                                        List<String> identityAssocDefUris) {
        Map<String, TopicModel> identityChildTopics = new HashMap();
        for (String assocDefUri : identityAssocDefUris) {
            TopicModel childTopic = childTopics.get(assocDefUri);
            // FIXME: only throw if NO identity child topic is given.
            // If at least ONE is given it is sufficient.
            if (childTopic == null) {
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
     */
    private DeepaMehtaObjectModelImpl updateChildRefs(DeepaMehtaObjectModelImpl parent,
                                                      Map<String, TopicModel> newChildTopics) {
        // sanity check
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: integrator type=\"" + type.getUri() + "\" vs. parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        //
        ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
        for (String assocDefUri : type) {
            parent.loadChildTopics(assocDefUri);    // TODO: load only one level deep?
            RelatedTopicModelImpl oldValue = childTopics.getTopicOrNull(assocDefUri);   // may be null
            TopicModel newValue = newChildTopics.get(assocDefUri);                      // may be null
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
                logger.info("### " + (deleted ? "Reassigning" : "Assigning") + " child " + newValue.getId() +
                    " (assocDefUri=\"" + assocDefUri + "\") to composite " + parent.getId() + " (typeUri=\"" +
                    type.uri + "\")");
                // update DB
                assoc = createChildAssociation(parent, newValue, assocDefUri);
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
        return parent;
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
     */
    private DeepaMehtaObjectModelImpl unifyChildTopics(Map<String, TopicModel> childTopics,
                                                       Iterable<String> assocDefUris) {
        List<RelatedTopicModelImpl> candidates = parentCandidates(childTopics);
        // logger.info("### candidates (" + candidates.size() + "): " + DeepaMehtaUtils.idList(candidates));
        for (String assocDefUri : assocDefUris) {
            eliminateParentCandidates(candidates, childTopics.get(assocDefUri), assocDefUri);
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
     */
    private List<RelatedTopicModelImpl> parentCandidates(Map<String, TopicModel> childTopics) {
        String assocDefUri = childTopics.keySet().iterator().next();
        // sanity check
        if (!type.getUri().equals(assocDef(assocDefUri).getParentTypeUri())) {
            throw new RuntimeException("Type mismatch");
        }
        //
        TopicModel childTopic = childTopics.get(assocDefUri);
        // TODO: assoc parents?
        return pl.getTopicRelatedTopics(childTopic.getId(), assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            "dm4.core.child", "dm4.core.parent", type.getUri());
    }

    /**
     * @param   childTopic      may be null
     */
    private void eliminateParentCandidates(List<RelatedTopicModelImpl> candidates, TopicModel childTopic,
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

    // ---

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

    private TopicModelImpl createCompositeTopic(Map<String, TopicModel> childTopics) {
        // FIXME: construct the composite model first, then create topic as a whole.
        // Otherwise the POST_CREATE_TOPIC event is fired too early, and e.g. Address topics get no geo coordinates.
        // logger.info("### childTopics=" + childTopics);
        TopicModelImpl topic = createSimpleTopic();
        logger.info("### Creating composite " + topic.id + " (typeUri=\"" + type.uri + "\")");
        for (String assocDefUri : childTopics.keySet()) {
            TopicModel childTopic = childTopics.get(assocDefUri);
            logger.info("### Assigning child " + childTopic.getId() + " (assocDefUri=\"" + assocDefUri +
                "\") to composite " + topic.id + " (typeUri=\"" + type.uri + "\")");
            createChildAssociation(topic, childTopic, assocDefUri);
        }
        return topic;
    }

    private AssociationModelImpl createChildAssociation(DeepaMehtaObjectModel parent, TopicModel child,
                                                                                      String assocDefUri) {
        return pl.createAssociation(assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"),
            child.createRoleModel("dm4.core.child")
        ).getModel();
    }

    // ---

    private AssociationDefinitionModel assocDef(String assocDefUri) {
        return type.getAssocDef(assocDefUri);
    }

    private boolean isValueType() {
        return type.getDataTypeUri().equals("dm4.core.value");
    }

    private boolean isEmptyValue(String assocDefUri) {
        return emptyValues.contains(assocDefUri);
    }
}
