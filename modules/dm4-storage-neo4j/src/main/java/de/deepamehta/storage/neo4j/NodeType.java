package de.deepamehta.storage.neo4j;

import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.service.ModelFactory;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;



enum NodeType implements Label {

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

    /**
     * Returns the node type of the given node.
     * <p>
     * If the given node is not a DM node an exception is thrown.
     */
    static NodeType of(Node node) {
        NodeType nodeType = nodeType(node);
        if (nodeType == null) {
            throw new RuntimeException("Node " + node.getId() + " is not a DeepaMehta node");
        }
        return nodeType;
    }

    static boolean isDeepaMehtaNode(Node node) {
        return nodeType(node) != null;
    }

    // ---

    /**
     * Checks if the given node is of this type.
     * <p>
     * For a non-DM node <code>false</code> is returned.
     * Non-DM nodes are those created by 3rd-party Neo4j components, e.g. Neo4j Spatial.
     */
    boolean isTypeOf(Node node) {
        // a node is regarded "non-DM" if it has neither a TOPIC nor an ASSOC label.
        return nodeType(node) == this;
    }

    // ### TODO: to be dropped?
    String stringify() {
        return name().toLowerCase();
    }

    // ---

    private static NodeType nodeType(Node node) {
        if (node.hasLabel(TOPIC)) {
            return TOPIC;
        } else if (node.hasLabel(ASSOC)) {
            return ASSOC;
        }
        return null;
    }
}
