package de.deepamehta.core.storage.neo4j;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;

import org.neo4j.helpers.Predicate;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
// import org.neo4j.graphdb.traversal.Position;
import org.neo4j.graphdb.traversal.PruneEvaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.kernel.Traversal;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Backs {@link TopicType} by Neo4j database.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class Neo4jTopicType extends TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    MetaModelClass metaClass;
    Node typeNode;
    Neo4jStorage storage;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Constructs a topic type and writes it to the database.
     */
    Neo4jTopicType(Map<String, Object> properties, List<DataField> dataFields, Neo4jStorage storage) {
        // 1) update memory
        super(properties, new ArrayList());
        // 2) update DB
        // create type
        String typeUri = (String) properties.get("de/deepamehta/core/property/TypeURI");
        this.storage = storage;
        this.metaClass = storage.createMetaClass(typeUri);
        this.typeNode = metaClass.node();
        this.id = typeNode.getId();
        logger.info("Creating topic type \"" + typeUri + "\" => ID=" + id);
        // set properties
        for (String key : properties.keySet()) {
            typeNode.setProperty(key, properties.get(key));
        }
        // add data fields
        for (DataField dataField : dataFields) {
            addDataField(dataField);
        }
    }

    /**
     * Reads a topic type from the database.
     */
    Neo4jTopicType(String typeUri, Neo4jStorage storage) {
        super(null, null);
        this.storage = storage;
        this.metaClass = storage.getMetaClass(typeUri);
        this.typeNode = metaClass.node();
        this.properties = storage.getProperties(typeNode);
        this.dataFields = readDataFields();
        this.id = typeNode.getId();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void setTypeUri(String typeUri) {
        String oldTypeUri = (String) getProperty("de/deepamehta/core/property/TypeURI");
        // 1) update memory
        storage.typeCache.remove(oldTypeUri);
        super.setTypeUri(typeUri);
        storage.typeCache.put(this);
        // 2) update DB
        typeNode.setProperty("de/deepamehta/core/property/TypeURI", typeUri);
        storage.namespace.rename(oldTypeUri, typeUri);
        // reassign data field sequence to new URI
        reassignFieldSequence(oldTypeUri, typeUri);
    }

    // ---

    @Override
    public Neo4jDataField getDataField(int index) {
        return (Neo4jDataField) super.getDataField(index);
    }

    @Override
    public Neo4jDataField getDataField(String uri) {
        return (Neo4jDataField) super.getDataField(uri);
    }

    /**
     * Adds a data field to this topic type and writes the data field to the database.
     */
    @Override
    public void addDataField(DataField dataField) {
        // 1) update DB
        String typeUri = (String) properties.get("de/deepamehta/core/property/TypeURI");
        // create data field
        Neo4jDataField field = new Neo4jDataField(dataField, storage);
        storage.getMetaClass(typeUri).getDirectProperties().add(field.getMetaProperty());
        // put in sequence
        putInFieldSequence(field.node, dataFields.size());
        // 2) update memory
        super.addDataField(field);
    }

    @Override
    public void removeDataField(String uri) {
        Neo4jDataField field = getDataField(uri);   // the data field to remove
        int index = dataFields.indexOf(field);      // the index of the data field to remove
        if (index == -1) {
            throw new RuntimeException("List.indexOf() returned -1");
        }
        // 1) update DB
        // repair sequence
        if (index == 0) {
            if (dataFields.size() > 1) {
                // The data field to remove is the first one and further data fields exist.
                // -> Make the next data field the new start data field.
                Node nextFieldNode = getDataField(index + 1).node;
                startFieldSequence(nextFieldNode);
            }
        } else {
            if (index < dataFields.size() - 1) {
                // The data field to remove is surrounded by other data fields.
                // -> Make the surrounding data fields direct neighbours.
                Node nextFieldNode = getDataField(index + 1).node;
                continueFieldSequence(nextFieldNode, index - 1);
            }
        }
        // delete the data field topic including all of its (sequence) relations
        storage.deleteTopic(field.node.getId());
        // 2) update memory
        super.removeDataField(uri);
    }

    @Override
    public void setDataFieldOrder(List uris) {
        // 1) update memory
        super.setDataFieldOrder(uris);
        // 2) update DB
        // delete sequence
        String typeUri = (String) getProperty("de/deepamehta/core/property/TypeURI");
        deleteFieldSequence(typeUri);
        // re-create sequence
        for (int i = 0; i < dataFields.size(); i++) {
            putInFieldSequence(getDataField(i).node, i);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void putInFieldSequence(Node fieldNode, int index) {
        if (index == 0) {
            startFieldSequence(fieldNode);
        } else {
            continueFieldSequence(fieldNode, index - 1);
        }
    }

    private void startFieldSequence(Node fieldNode) {
        Relationship rel = typeNode.createRelationshipTo(fieldNode, Neo4jStorage.RelType.SEQUENCE_START);
        String typeUri = (String) properties.get("de/deepamehta/core/property/TypeURI");
        rel.setProperty("type_uri", typeUri);
    }

    private void continueFieldSequence(Node fieldNode, int prevFieldIndex) {
        Node prevFieldNode = getDataField(prevFieldIndex).node;
        Relationship rel = prevFieldNode.createRelationshipTo(fieldNode, Neo4jStorage.RelType.SEQUENCE);
        String typeUri = (String) properties.get("de/deepamehta/core/property/TypeURI");
        rel.setProperty("type_uri", typeUri);
    }

    // ---

    private void reassignFieldSequence(String oldTypeUri, String typeUri) {
        logger.info("### Re-assigning data field sequence from type \"" + oldTypeUri + "\" to \"" + typeUri + "\"");
        int count = 0;
        for (Path path : getFieldSequence(oldTypeUri)) {
            path.lastRelationship().setProperty("type_uri", typeUri);
            count++;
        }
        logger.info("### " + count + " data fields re-assigned");
    }

    private void deleteFieldSequence(String typeUri) {
        logger.info("### Deleting data field sequence of type \"" + typeUri + "\"");
        int count = 0;
        for (Path path : getFieldSequence(typeUri)) {
            path.lastRelationship().delete();
            count++;
        }
        logger.info("### " + count + " relations deleted");
    }

    // ---

    /**
     * Reads the data fields of this topic type from the database.
     */
    private List<DataField> readDataFields() {
        String typeUri = (String) properties.get("de/deepamehta/core/property/TypeURI");
        // use as control group
        List propNodes = new ArrayList();
        for (MetaModelProperty metaProp : storage.getMetaClass(typeUri).getDirectProperties()) {
            propNodes.add(metaProp.node());
        }
        //
        List dataFields = new ArrayList();
        for (Path path : getFieldSequence(typeUri)) {
            Node fieldNode = path.endNode();
            logger.fine("  # Result " + fieldNode);
            // error check
            if (!propNodes.contains(fieldNode)) {
                throw new RuntimeException("Graph inconsistency for topic type \"" + typeUri + "\": " +
                    fieldNode + " appears in data field sequence but is not a meta property node");
            }
            //
            dataFields.add(new Neo4jDataField(storage.getProperties(fieldNode), fieldNode));
        }
        // error check
        if (propNodes.size() != dataFields.size()) {
            throw new RuntimeException("Graph inconsistency for topic type \"" + typeUri + "\": there are " +
                dataFields.size() + " nodes in data field sequence but " + propNodes.size() + " meta property nodes");
        }
        //
        return dataFields;
    }

    private Iterable<Path> getFieldSequence(String typeUri) {
        TraversalDescription desc = Traversal.description();
        desc = desc.relationships(Neo4jStorage.RelType.SEQUENCE_START, Direction.OUTGOING);
        desc = desc.relationships(Neo4jStorage.RelType.SEQUENCE,       Direction.OUTGOING);
        // A custom filter is used to return only the nodes of this topic type's individual path.
        // The path is recognized by the "type_uri" property of the constitutive relationships.
        desc = desc.filter(new SequenceReturnFilter(typeUri));
        // We need breadth first in order to get the nodes in proper sequence order.
        // (default is not breadth first, but probably depth first).
        desc = desc.breadthFirst();
        // We need to traverse a node more than once because it may be involved in many sequences.
        // (default uniqueness is not RELATIONSHIP_GLOBAL, but probably NODE_GLOBAL).
        desc = desc.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
        //
        return desc.traverse(typeNode);
    }

    private class SequenceReturnFilter implements Predicate {

        private String typeUri;

        private SequenceReturnFilter(String typeUri) {
            logger.fine("########## Traversing data field sequence for topic type \"" + typeUri + "\"");
            this.typeUri = typeUri;
        }

        @Override
        public boolean accept(Object item) {
            Path path = (Path) item;
            boolean doReturn = path.length() > 0 && path.lastRelationship().getProperty("type_uri").equals(typeUri);
            logger.fine("### " + path.endNode() + " " + path.lastRelationship() + " => return=" + doReturn);
            return doReturn;
        }
    }
}
