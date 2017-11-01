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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



class ValueIntegrator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModelImpl newValues;
    private DeepaMehtaObjectModelImpl targetObject;    // may null
    private TypeModelImpl type;

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueIntegrator(PersistenceLayer pl) {
        this.pl = pl;
        this.mf = pl.mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    DeepaMehtaObjectModel integrate(DeepaMehtaObjectModelImpl newValues, DeepaMehtaObjectModelImpl targetObject) {
        if (newValues.getTypeUri() == null) {
            throw new IllegalArgumentException("Tried to integrate newValues whose typeUri is not set (newValues=" +
                newValues + ")");
        }
        this.newValues = newValues;
        this.targetObject = targetObject;
        this.type = newValues.getType();
        if (newValues.isSimple()) {
            return integrateSimple();
        } else {
            DeepaMehtaObjectModelImpl comp = integrateComposite();
            if (comp != null) {
                new LabelCalculation(comp).calculate();
            }
            return comp;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Preconditions:
     *   - this.newValues is simple
     */
    private DeepaMehtaObjectModel integrateSimple() {
        SimpleValue newValue = newValues.getSimpleValue();
        if (newValue.toString().isEmpty()) {
            // TODO: remove parent assignment if exists
            return null;
        }
        return unifySimple();
    }

    private TopicModel unifySimple() {
        SimpleValue newValue = newValues.getSimpleValue();
        String typeUri = type.getUri();
        // FIXME: HTML values must be tag-stripped before lookup, complementary to indexing
        Topic _topic = pl.getTopicByValue(type.getUri(), newValue);     // TODO: let pl return models
        TopicModel topic = _topic != null ? _topic.getModel() : null;   // TODO: drop
        if (topic != null) {
            logger.info("Reusing simple value " + topic.getId() + " \"" + newValue + "\" (typeUri=\"" + typeUri +
                "\")");
        } else {
            topic = createSimpleTopic();
            logger.info("### Creating simple value " + topic.getId() + " \"" + newValue + "\" (typeUri=\"" + typeUri +
                "\")");
        }
        return topic;
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
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
                if (childTopic != null) {
                    childTopics.put(assocDefUri, childTopic);
                }
            }
            return !childTopics.isEmpty() ? unifyComposite(childTopics) : null;
        } catch (Exception e) {
            throw new RuntimeException("Integrating a composite failed (typeUri=\"" + type.getUri() + "\")", e);
        }
    }

    /**
     * Invokes a ValueIntegrator for a child value.
     */
    private TopicModel integrateChildValue(RelatedTopicModelImpl newChildValue) {
        return (TopicModel) new ValueIntegrator(pl).integrate(newChildValue, null);     // targetObject=null
        // updateRelatingAssociation(refChildValue, newChildValue);    // TODO
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     *   - childTopics map is not empty
     */
    private DeepaMehtaObjectModelImpl unifyComposite(Map<String, TopicModel> childTopics) {
        if (type.isValueType()) {
            return unifyChildTopics(childTopics);
        } else {
            DeepaMehtaObjectModelImpl parent = targetObject != null ? targetObject : identifyParent(childTopics);
            updateChildRefs(parent, childTopics);
            return parent;
        }
    }

    private DeepaMehtaObjectModelImpl identifyParent(Map<String, TopicModel> childTopics) {
        return unifyChildTopics(identityChildTopics(childTopics));
    }

    private Map<String, TopicModel> identityChildTopics(Map<String, TopicModel> childTopics) {
        Map<String, TopicModel> identityChildTopics = new HashMap();
        for (String assocDefUri : type.getLabelAssocDefUris()) {   // TODO: repurpose label config as "Part of identity"
            TopicModel childTopic = childTopics.get(assocDefUri);
            if (childTopic == null) {
                throw new RuntimeException("Identity child topic \"" + assocDefUri + "\" is missing in " +
                    childTopics.keySet());
            }
            identityChildTopics.put(assocDefUri, childTopic);
        }
        return identityChildTopics;
    }

    /**
     * Updates a parent's child assignments in-place.
     *
     * Preconditions:
     *   - this.newValues is composite
     *   - parent's type is this.type
     *   - assocDef's parent type is this.type
     *   - newChildTopic's type is assocDef's child type
     */
    private void updateChildRefs(DeepaMehtaObjectModelImpl parent, Map<String, TopicModel> newChildTopics) {
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: integrator type=\"" + type.getUri() + "\" vs. parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        for (String assocDefUri : newChildTopics.keySet()) {
            ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
            parent.loadChildTopics(assocDefUri);    // TODO: load only one level deep?
            RelatedTopicModelImpl childTopic = childTopics.getTopicOrNull(assocDefUri);     // current one
            TopicModel newChildTopic = newChildTopics.get(assocDefUri);                     // new one
            // delete assignment if exists already and child has changed
            boolean deleted = false;
            if (childTopic != null && !childTopic.equals(newChildTopic)) {
                childTopic.getRelatingAssociation().delete();
                deleted = true;
            }
            // create assignment if not yet exists or child has changed
            if (childTopic == null || !childTopic.equals(newChildTopic)) {
                // update DB
                AssociationModel assoc = associateChildTopic(parent, newChildTopic, assocDefUri);
                logger.info("### " + (deleted ? "Reassigning" : "Assigning") + " child " + newChildTopic.getId() +
                    " (assocDefUri=\"" + assocDefUri + "\") to composite " + parent.getId() + " (typeUri=\"" +
                    parent.getTypeUri() + "\")");
                // update memory
                childTopics.put(assocDefUri, mf.newRelatedTopicModel(newChildTopic, assoc));
            }
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
    private DeepaMehtaObjectModelImpl unifyChildTopics(Map<String, TopicModel> childTopics) {
        List<RelatedTopicModelImpl> candidates = null;
        int i = 0;
        for (String assocDefUri : childTopics.keySet()) {
            TopicModel childTopic = childTopics.get(assocDefUri);
            if (i == 0) {
                candidates = candidates(childTopic, assocDefUri);
            } else {
                eliminateCandidates(candidates, childTopic);
            }
            if (candidates.isEmpty()) {
                break;
            }
            i++;
        }
        DeepaMehtaObjectModelImpl comp;
        String typeUri = type.getUri();
        switch (candidates.size()) {
        case 0:
            comp = createCompositeTopic(childTopics);
            logger.info("### Creating composite " + comp.getId() + " (typeUri=\"" + typeUri + "\")");
            return comp;
        case 1:
            comp = candidates.get(0);
            logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + typeUri + "\")");
            return comp;
        default:
            throw new RuntimeException("Ambiguity: there are " + candidates.size() +
                " composites with the same values (typeUri=\"" + typeUri + "\") (" + childTopics.values().size() +
                " values: " + childTopics.values() + ")");
        }
    }

    /**
     * Preconditions:
     *   - this.newValues is composite
     *   - assocDef's parent type is this.type
     *   - childTopic's type is assocDef's child type
     */
    private List<RelatedTopicModelImpl> candidates(TopicModel childTopic, String assocDefUri) {
        if (!type.getUri().equals(assocDef(assocDefUri).getParentTypeUri())) {
            throw new RuntimeException("Type mismatch");
        }
        // TODO: assoc parents
        return pl.getTopicRelatedTopics(childTopic.getId(), assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            "dm4.core.child", "dm4.core.parent", type.getUri());
    }

    private void eliminateCandidates(List<RelatedTopicModelImpl> candidates, TopicModel childTopic) {
        Iterator<RelatedTopicModelImpl> i = candidates.iterator();
        while (i.hasNext()) {
            RelatedTopicModel parent = i.next();
            String assocTypeUri = parent.getRelatingAssociation().getTypeUri();
            // TODO: assoc parents
            if (pl.getAssociation(assocTypeUri, parent.getId(), childTopic.getId(), "dm4.core.parent", "dm4.core.child")
                    == null) {
                i.remove();
            }
        }
    }

    // ---

    private TopicModelImpl createSimpleTopic() {
        return pl.createTopic(mf.newTopicModel(type.getUri(), newValues.getSimpleValue())).getModel();
    }

    private TopicModelImpl createCompositeTopic(Map<String, TopicModel> childTopics) {
        // FIXME: construct the composite model first, then create topic as a whole.
        // Otherwise the POST_CREATE_TOPIC event is fired too early, and e.g. Address topics get no geo coordinates.
        TopicModelImpl topic = createSimpleTopic();
        for (String assocDefUri : childTopics.keySet()) {
            associateChildTopic(topic, childTopics.get(assocDefUri), assocDefUri);
        }
        return topic;
    }

    private AssociationModel associateChildTopic(DeepaMehtaObjectModel parent, TopicModel child, String assocDefUri) {
        return pl.createAssociation(assocDef(assocDefUri).getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"),
            child.createRoleModel("dm4.core.child")
        ).getModel();
    }

    // ---

    private AssociationDefinitionModel assocDef(String assocDefUri) {
        return type.getAssocDef(assocDefUri);
    }
}
