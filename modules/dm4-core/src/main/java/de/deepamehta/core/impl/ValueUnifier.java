package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TypeModel;

import java.util.List;
import java.util.logging.Logger;



class ValueUnifier {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModelImpl updateModel;
    private DeepaMehtaObjectModelImpl refObj;       // may null

    private PersistenceLayer pl;
    private ModelFactoryImpl mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueUnifier(DeepaMehtaObjectModelImpl updateModel, PersistenceLayer pl) {
        this.updateModel = updateModel;
        this.pl = pl;
        this.mf = pl.mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    long unify() {
        return unify(null);
    }

    long unify(DeepaMehtaObjectModelImpl refObj) {
        this.refObj = refObj;
        if (updateModel.isSimple()) {
            return unifySimple(updateModel.getSimpleValue());
        } else {
            return unifyComposite(updateModel.getType(), updateModel.getChildTopicsModel());
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private long unifySimple(SimpleValue newValue) {
        if (refObj != null) {
            if (newValue != null && !newValue.equals(refObj.value)) {          // abort if no update is requested
                logger.info("### Changing simple value of " + refObj.objectInfo() + " from \"" + refObj.value +
                    "\" -> \"" + newValue + "\"");
                return getOrCreateSimpleTopic(newValue).getId();
            } else {
                return refObj.id;
            }
        } else {
            return -1;  // TODO
        }
    }

    private long unifyComposite(TypeModelImpl type, ChildTopicsModelImpl updateModel) {
        try {
            for (AssociationDefinitionModel assocDef : type.getAssocDefs()) {
                String assocDefUri = assocDef.getAssocDefUri();
                if (assocDef.getChildCardinalityUri().equals("dm4.core.many")) {
                    throw new RuntimeException("many cardinality not yet implemented");
                }
                RelatedTopicModelImpl childUpdateModel = updateModel.getTopicOrNull(assocDefUri);
                // skip if not contained in update request
                if (childUpdateModel == null) {
                    continue;
                }
                //
                unifyChild(childUpdateModel, assocDef);      // TODO: collect returned ids
            }
            //
            // _calculateLabelAndUpdate();  // TODO
            //
            return -1;
            // TODO: unify composite based on collected child ids, and return its id.
            // The unified composite is either an existing one, or newly created.
        } catch (Exception e) {
            throw new RuntimeException("Unifying composite failed", e);
        }
    }

    private long unifyChild(RelatedTopicModelImpl childUpdateModel, AssociationDefinitionModel assocDef) {
        RelatedTopicModelImpl childTopic = null;
        if (refObj != null) {
            // Note: updating the child topics requires them to be loaded
            // TODO: load only one level deep
            refObj.loadChildTopics(assocDef);
            childTopic = refObj.childTopics.getTopicOrNull(assocDef.getAssocDefUri());
        }
        logger.info("Unify \"" + childUpdateModel.typeUri + "\" of " + childTopic + " with " + childUpdateModel);
        return new ValueUnifier(childUpdateModel, pl).unify(childTopic);
        // updateRelatingAssociation(childTopic, childUpdateModel);    // TODO
    }

    private TopicImpl getOrCreateSimpleTopic(SimpleValue value) {
        TopicImpl topic = pl.getTopicByValue(refObj.typeUri, value);
        if (topic == null) {
            topic = pl.createTopic(mf.newTopicModel(refObj.typeUri, value));
        }
        return topic;
    }
}
