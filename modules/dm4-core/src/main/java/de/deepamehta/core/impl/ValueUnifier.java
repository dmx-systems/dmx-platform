package de.deepamehta.core.impl;

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



class ValueUnifier {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModelImpl updateModel;
    private DeepaMehtaObjectModelImpl refObj;       // may null
    private TypeModel type;

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueUnifier(PersistenceLayer pl) {
        this.pl = pl;
        this.mf = pl.mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    DeepaMehtaObjectModel unify(DeepaMehtaObjectModelImpl updateModel, DeepaMehtaObjectModelImpl refObj) {
        if (updateModel.getTypeUri() == null) {
            throw new RuntimeException("Tried to unify an updateModel whose typeUri is not set");
        }
        this.updateModel = updateModel;
        this.refObj = refObj;
        this.type = updateModel.getType();
        if (updateModel.isSimple()) {
            return unifySimple();
        } else {
            return unifyComposite();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private DeepaMehtaObjectModel unifySimple() {
        SimpleValue newValue = updateModel.getSimpleValue();
        if (refObj != null) {
            if (newValue != null && !newValue.equals(refObj.getSimpleValue())) {    // abort if no update is requested
                logger.info("### Changing simple value of " + refObj.objectInfo() + " from \"" +
                    refObj.getSimpleValue() + "\" -> \"" + newValue + "\"");
                if (type.isValueType()) {
                    return getOrCreateSimpleTopic(type.getUri(), newValue);
                } else {
                    refObj.updateSimpleValue(newValue);
                }
            }
            return refObj;
        } else {
            if (type.isValueType()) {
                return getOrCreateSimpleTopic(type.getUri(), newValue);
            } else {
                return createSimpleTopic(type.getUri(), newValue);
            }
        }
    }

    private DeepaMehtaObjectModel unifyComposite() {
        try {
            Map<AssociationDefinitionModel, TopicModel> childTopics = new HashMap();
            for (AssociationDefinitionModel assocDef : type.getAssocDefs()) {
                String assocDefUri = assocDef.getAssocDefUri();
                if (assocDef.getChildCardinalityUri().equals("dm4.core.many")) {
                    throw new RuntimeException("many cardinality not yet implemented");
                }
                RelatedTopicModelImpl childUpdateModel = updateModel.getChildTopicsModel().getTopicOrNull(assocDefUri);
                // skip if not contained in update request
                if (childUpdateModel == null) {
                    continue;
                }
                //
                TopicModel childTopic = unifyChild(childUpdateModel, assocDef);
                childTopics.put(assocDef, childTopic);
            }
            // _calculateLabelAndUpdate();  // TODO
            return updateParentRefs(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Unifying composite failed", e);
        }
    }

    private TopicModel unifyChild(RelatedTopicModelImpl childUpdateModel, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = null;
        if (refObj != null) {
            // Note: updating the child topics requires them to be loaded
            // TODO: load only one level deep?
            refObj.loadChildTopics(assocDef);
            childTopic = refObj.getChildTopicsModel().getTopicOrNull(assocDef.getAssocDefUri());
        }
        return (TopicModel) new ValueUnifier(pl).unify(childUpdateModel, childTopic);
        // updateRelatingAssociation(childTopic, childUpdateModel);    // TODO
    }

    private DeepaMehtaObjectModelImpl updateParentRefs(Map<AssociationDefinitionModel, TopicModel> childTopics) {
        DeepaMehtaObjectModelImpl parent = null;
        if (type.isValueType()) {
            unifyChildTopics(childTopics);
            throw new RuntimeException("Not yet implemented: update value types");
        } else {
            if (refObj != null) {
                parent = refObj;
                for (AssociationDefinitionModel assocDef : childTopics.keySet()) {
                    RelatedTopicModelImpl childTopic = parent.getChildTopicsModel().getTopicOrNull(
                        assocDef.getAssocDefUri());
                    if (childTopic != null) {
                        childTopic.getRelatingAssociation().delete();
                    }
                    associateChildTopic(parent, childTopics.get(assocDef), assocDef);
                }
            } else {
                throw new RuntimeException("Not yet implemented: unify parent by identifying attributes");
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
        switch (candidates.size()) {
        case 0:
            return createCompositeTopic(parentTypeUri, childTopics);
        case 1:
            return candidates.get(0);
        default:
            throw new RuntimeException("Ambiguity: there are " + candidates.size() + " topics with the same values");
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
        TopicModel topic = pl.getTopicByValue(typeUri, value).getModel();
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
