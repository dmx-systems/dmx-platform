package systems.dmx.storage.neo4j;

import systems.dmx.core.model.RoleModel;
import systems.dmx.core.service.ModelFactory;

import org.neo4j.graphdb.Node;



enum NodeType {

    TOPIC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf) {
            return mf.newTopicRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to an Assoc when the caller expects a Topic";
        }
    },
    ASSOC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf) {
            return mf.newAssociationRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to a Topic when the caller expects an Assoc";
        }
    };

    // ---

    abstract RoleModel createRoleModel(Node node, String roleTypeUri, ModelFactory mf);

    abstract String error(Node node);

    // ---

    static NodeType of(Node node) {
        String type = (String) node.getProperty("nodeType");
        return valueOf(type.toUpperCase());
    }

    /**
     * Checks if the given node is of this type.
     * <p>
     * For a non-DM node <code>false</code> is returned.
     * Non-DM nodes are those created by 3rd-party Neo4j components, e.g. Neo4j Spatial.
     */
    boolean isTypeOf(Node node) {
        // a node is regarded "non-DM" if it has no "nodeType" property.
        return node.getProperty("nodeType", "").equals(stringify());
    }

    String stringify() {
        return name().toLowerCase();
    }
}
