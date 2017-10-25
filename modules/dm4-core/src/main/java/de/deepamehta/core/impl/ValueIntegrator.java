package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
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
    private DeepaMehtaObjectModelImpl refValues;    // may null
    private TypeModel type;

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueIntegrator(PersistenceLayer pl) {
        this.pl = pl;
        this.mf = pl.mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    DeepaMehtaObjectModel integrate(DeepaMehtaObjectModelImpl newValues, DeepaMehtaObjectModelImpl refValues) {
        if (newValues.getTypeUri() == null) {
            throw new IllegalArgumentException("Tried to integrate newValues whose typeUri is not set");
        }
        this.newValues = newValues;
        this.refValues = refValues;
        this.type = newValues.getType();
        if (newValues.isSimple()) {
            return integrateSimple();
        } else {
            DeepaMehtaObjectModelImpl comp = integrateComposite();
            new LabelCalculation(comp).calculate();
            return comp;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private DeepaMehtaObjectModel integrateSimple() {
        if (type.isValueType()) {
            return unifySimple();
        } else {
            if (refValues != null) {
                refValues._updateSimpleValue(newValues.getSimpleValue());    // update memory + DB
                return refValues;
            } else {
                return createSimpleTopic();
            }
        }
    }

    private TopicModel unifySimple() {
        Topic _topic = pl.getTopicByValue(type.getUri(), newValues.getSimpleValue());   // TODO: let pl return models
        TopicModel topic = _topic != null ? _topic.getModel() : null;                   // TODO: drop
        if (topic == null) {
            topic = createSimpleTopic();
        }
        return topic;
    }

    private DeepaMehtaObjectModelImpl integrateComposite() {
        try {
            Map<AssociationDefinitionModel, TopicModel> childTopics = new HashMap();
            for (AssociationDefinitionModel assocDef : type.getAssocDefs()) {
                String assocDefUri = assocDef.getAssocDefUri();
                if (assocDef.getChildCardinalityUri().equals("dm4.core.many")) {
                    throw new RuntimeException("many cardinality not yet implemented");
                }
                RelatedTopicModelImpl newChildValue = newValues.getChildTopicsModel().getTopicOrNull(assocDefUri);
                // skip if not contained in update request
                if (newChildValue == null) {
                    continue;
                }
                //
                TopicModel childTopic = integrateChildValue(newChildValue, assocDef);
                childTopics.put(assocDef, childTopic);
            }
            return unifyComposite(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Integrating a composite failed (typeUri=\"" + type.getUri() + "\")", e);
        }
    }

    /**
     * Invokes a ValueIntegrator for a child value.
     */
    private TopicModel integrateChildValue(RelatedTopicModelImpl newChildValue, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl refChildValue = null;
        if (refValues != null) {
            // Note: updating the child topics requires them to be loaded
            // TODO: load only one level deep?
            refValues.loadChildTopics(assocDef);
            refChildValue = refValues.getChildTopicsModel().getTopicOrNull(assocDef.getAssocDefUri());
        }
        return (TopicModel) new ValueIntegrator(pl).integrate(newChildValue, refChildValue);
        // updateRelatingAssociation(refChildValue, newChildValue);    // TODO
    }

    private DeepaMehtaObjectModelImpl unifyComposite(Map<AssociationDefinitionModel, TopicModel> childTopics) {
        if (type.isValueType()) {
            return unifyChildTopics(childTopics);
        } else {
            // for identity parents the child assignments are updated in-place
            DeepaMehtaObjectModelImpl parent = null;
            if (refValues != null) {
                parent = refValues;
            } else {
                // TODO: identify parent: call unifyChildTopics with only the identifying childTopics
                throw new RuntimeException("Not yet implemented: get parent (typeUri=\"" + type.getUri() +
                    "\") by identifying attributes");
            }
            updateChildRefs(parent, childTopics);
            return parent;
        }
    }

    /**
     * Updates a parent's child assignments in-place.
     */
    private void updateChildRefs(DeepaMehtaObjectModelImpl parent,
                                 Map<AssociationDefinitionModel, TopicModel> newChildTopics) {
        if (!parent.getTypeUri().equals(type.getUri())) {
            throw new RuntimeException("Type mismatch: integrator type=\"" + type.getUri() + "\" vs. parent type=\"" +
                parent.getTypeUri() + "\"");
        }
        for (AssociationDefinitionModel assocDef : newChildTopics.keySet()) {
            String assocDefUri = assocDef.getAssocDefUri();
            ChildTopicsModelImpl childTopics = parent.getChildTopicsModel();
            RelatedTopicModelImpl childTopic = childTopics.getTopicOrNull(assocDefUri);     // current one
            TopicModel newChildTopic = newChildTopics.get(assocDef);                        // new one
            // delete assignment if exists already and child has changed
            boolean deleted = false;
            if (childTopic != null && !childTopic.equals(newChildTopic)) {
                childTopic.getRelatingAssociation().delete();
                deleted = true;
            }
            // create assignment if not yet exists or child has changed
            if (childTopic == null || !childTopic.equals(newChildTopic)) {
                // update DB
                associateChildTopic(parent, newChildTopic, assocDef);
                logger.info("### Child " + newChildTopic.getId() + " (assocDefUri=\"" + assocDefUri + "\") " +
                    (deleted ? "re" : "") + "assigned to composite " + parent.getId() + " (typeUri=\"" +
                    parent.getTypeUri() + "\")");
                // update memory
                childTopics.put(assocDefUri, newChildTopic);    // FIXME: pass RelatedTopicModel
            }
        }
    }

    // ---

    private DeepaMehtaObjectModelImpl unifyChildTopics(Map<AssociationDefinitionModel, TopicModel> childTopics) {
        List<RelatedTopicModelImpl> candidates = null;
        int i = 0;
        for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
            TopicModel childTopic = childTopics.get(assocDef);
            if (i == 0) {
                candidates = candidates(childTopic, assocDef);
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
            logger.info("Creating composite " + comp.getId() + " (typeUri=\"" + typeUri + "\")");
            return comp;
        case 1:
            comp = candidates.get(0);
            logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + typeUri + "\")");
            return comp;
        default:
            throw new RuntimeException("Ambiguity: there are " + candidates.size() +
                " composites with the same values (typeUri=\"" + typeUri + "\")");
        }
    }

    private List<RelatedTopicModelImpl> candidates(TopicModel childTopic, AssociationDefinitionModel assocDef) {
        // TODO: assoc parents
        return pl.getTopicRelatedTopics(childTopic.getId(), assocDef.getInstanceLevelAssocTypeUri(), "dm4.core.child",
            "dm4.core.parent", assocDef.getParentTypeUri());
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

    private TopicModelImpl createCompositeTopic(Map<AssociationDefinitionModel, TopicModel> childTopics) {
        TopicModelImpl topic = createSimpleTopic();
        for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
            associateChildTopic(topic, childTopics.get(assocDef), assocDef);
        }
        return topic;
    }

    private void associateChildTopic(DeepaMehtaObjectModel parent, TopicModel child,
                                     AssociationDefinitionModel assocDef) {
        pl.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"),
            child.createRoleModel("dm4.core.child")
        );
    }
}
