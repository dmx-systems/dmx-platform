package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
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
            throw new RuntimeException("Tried to integrate newValues whose typeUri is not set");
        }
        this.newValues = newValues;
        this.refValues = refValues;
        this.type = newValues.getType();
        if (newValues.isSimple()) {
            return integrateSimple();
        } else {
            return integrateComposite();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private DeepaMehtaObjectModel integrateSimple() {
        SimpleValue newVal = newValues.getSimpleValue();
        if (refValues != null) {
            if (newVal != null && !newVal.equals(refValues.getSimpleValue())) {    // abort if no update is requested
                logger.info("### Changing simple value of " + refValues.objectInfo() + " from \"" +
                    refValues.getSimpleValue() + "\" -> \"" + newVal + "\"");
                if (type.isValueType()) {
                    return getOrCreateSimpleTopic(type.getUri(), newVal);
                } else {
                    refValues.updateSimpleValue(newVal);
                }
            }
            return refValues;
        } else {
            if (type.isValueType()) {
                return getOrCreateSimpleTopic(type.getUri(), newVal);
            } else {
                return createSimpleTopic(type.getUri(), newVal);
            }
        }
    }

    private DeepaMehtaObjectModel integrateComposite() {
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
            // _calculateLabelAndUpdate();  // TODO
            return unifyComposite(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Integrating a composite failed (typeUri=\"" + type.getUri() + "\")", e);
        }
    }

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
        DeepaMehtaObjectModelImpl parent = null;
        if (type.isValueType()) {
            return unifyChildTopics(childTopics);
        } else {
            if (refValues != null) {
                parent = refValues;
                for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
                    RelatedTopicModelImpl childTopic = parent.getChildTopicsModel().getTopicOrNull(
                        assocDef.getAssocDefUri());
                    if (childTopic != null) {
                        childTopic.getRelatingAssociation().delete();
                    }
                    associateChildTopic(parent, childTopics.get(assocDef), assocDef);
                }
            } else {
                throw new RuntimeException("Not yet implemented: get parent by identifying attributes");
            }
        }
        return parent;
    }

    // ---

    private DeepaMehtaObjectModelImpl unifyChildTopics(Map<AssociationDefinitionModel, TopicModel> childTopics) {
        List<RelatedTopicModelImpl> candidates = null;
        String parentTypeUri = null;
        int i = 0;
        for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
            TopicModel childTopic = childTopics.get(assocDef);
            if (i == 0) {
                candidates = candidates(childTopic, assocDef);
                parentTypeUri = assocDef.getParentTypeUri();
            } else {
                elimianteCandidates(candidates, childTopic);
            }
            if (candidates.isEmpty()) {
                break;
            }
            i++;
        }
        DeepaMehtaObjectModelImpl comp;
        switch (candidates.size()) {
        case 0:
            comp = createCompositeTopic(parentTypeUri, childTopics);
            logger.info("Creating composite " + comp.getId() + " (typeUri=\"" + parentTypeUri + "\")");
            return comp;
        case 1:
            comp = candidates.get(0);
            logger.info("Reusing composite " + comp.getId() + " (typeUri=\"" + parentTypeUri + "\")");
            return comp;
        default:
            throw new RuntimeException("Ambiguity: there are " + candidates.size() +
                " composites with the same values (typeUri=\"" + parentTypeUri + "\")");
        }
    }

    private List<RelatedTopicModelImpl> candidates(TopicModel childTopic, AssociationDefinitionModel assocDef) {
        // TODO: assoc parents
        return pl.getTopicRelatedTopics(childTopic.getId(), assocDef.getInstanceLevelAssocTypeUri(), "dm4.core.child",
            "dm4.core.parent", assocDef.getParentTypeUri());
    }

    private void elimianteCandidates(List<RelatedTopicModelImpl> candidates, TopicModel childTopic) {
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

    private TopicModel getOrCreateSimpleTopic(String typeUri, SimpleValue value) {
        Topic _topic = pl.getTopicByValue(typeUri, value);              // TODO: pl returns models, no objects
        TopicModel topic = _topic != null ? _topic.getModel() : null;   // TODO: drop
        if (topic == null) {
            topic = createSimpleTopic(typeUri, value);
        }
        return topic;
    }

    private TopicModelImpl createSimpleTopic(String typeUri) {
        return createSimpleTopic(typeUri, null);
    }

    private TopicModelImpl createSimpleTopic(String typeUri, SimpleValue value) {
        return pl.createTopic(mf.newTopicModel(typeUri, value)).getModel();
    }

    // ---

    private TopicModelImpl createCompositeTopic(String typeUri,
                                                Map<AssociationDefinitionModel, TopicModel> childTopics) {
        TopicModelImpl topic = createSimpleTopic(typeUri);
        for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
            associateChildTopic(topic, childTopics.get(assocDef), assocDef);
        }
        return topic;
    }

    private void associateChildTopic(DeepaMehtaObjectModelImpl parent, TopicModel child,
                                     AssociationDefinitionModel assocDef) {
        pl.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"),
            child.createRoleModel("dm4.core.child")
        );
    }
}
