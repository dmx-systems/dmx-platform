package de.deepamehta.mehtagraph.neo4j;



enum NodeType {

    TOPIC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri) {
            return new TopicRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to an Association when the caller expects a Topic";
        }
    },
    ASSOC {
        @Override
        RoleModel createRoleModel(Node node, String roleTypeUri) {
            return new AssociationRoleModel(node.getId(), roleTypeUri);
        }

        @Override
        String error(Node node) {
            return "ID " + node.getId() + " refers to a Topic when the caller expects an Association";
        }
    };

    private static NodeType of(Node node) {
        String type = node.getProperty("node_type");
        return valueOf(type.toUpperCase());
    }

    // ---

    abstract RoleModel createRoleModel(long nodeId, String roleTypeUri);

    abstract String error();
}
