package de.deepamehta.datomic;

import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.service.ModelFactory;

import datomic.Entity;



enum EntityType {

    TOPIC(":dm4.entity-type/topic") {
        @Override
        RoleModel createRoleModel(long topicId, String roleTypeUri, ModelFactory mf) {
            return mf.newTopicRoleModel(topicId, roleTypeUri);
        }
    },
    ASSOC(":dm4.entity-type/assoc") {
        @Override
        RoleModel createRoleModel(long assocId, String roleTypeUri, ModelFactory mf) {
            return mf.newAssociationRoleModel(assocId, roleTypeUri);
        }
    };

    String ident;

    private EntityType(String ident) {
        this.ident = ident;
    }

    abstract RoleModel createRoleModel(long playerId, String roleTypeUri, ModelFactory mf);

    static EntityType of(Entity e) {
        String type = e.get(":dm4/entity-type").toString();
        return valueOf(type.substring(":dm4.entity-type/".length()).toUpperCase());
    }
}
