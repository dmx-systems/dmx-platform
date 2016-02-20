package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ResultList;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



class TopicModelImpl extends DeepaMehtaObjectModelImpl implements TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicModelImpl(DeepaMehtaObjectModel object) {
        super(object);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return mf.newTopicRoleModel(id, roleTypeUri);
    }



    // === Java API ===

    @Override
    public TopicModel clone() {
        try {
            return (TopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TopicModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "topic (" + super.toString() + ")";
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    TopicTypeModel getType() {
        return pl.typeStorage.getTopicType(typeUri);
    }

    @Override
    List<AssociationModel> getAssociations() {
        return pl.fetchTopicAssociations(id);
    }

    @Override
    ResultList<RelatedTopicModel> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                   String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }
}
