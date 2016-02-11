package de.deepamehta.storage.neo4j;

import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.service.ModelFactory;

import org.neo4j.graphdb.Node;



enum NodeType {

    TOPIC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf) {
            return mf.newTopicRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to an Association when the caller expects a Topic";
        }
    },
    ASSOC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf) {
            return mf.newAssociationRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to a Topic when the caller expects an Association";
        }
    };

    // ---

    abstract RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf);

    abstract String error(Node node);

    // ---

    static NodeType of(Node node) {
        String type = (String) node.getProperty("node_type");
        return valueOf(type.toUpperCase());
    }

    boolean isTypeOf(Node node) {
        return node.getProperty("node_type").equals(stringify());
    }

    String stringify() {
        return name().toLowerCase();
    }
}
