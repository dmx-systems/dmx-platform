package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.MehtaGraphIndexMode;
import de.deepamehta.mehtagraph.spi.MehtaNode;

import org.neo4j.graphdb.Node;



class Neo4jMehtaNode extends Neo4jMehtaObject implements MehtaNode {

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jMehtaNode(Node node, Neo4jBase base) {
        super(node, base);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === MehtaObject Implementation ===

    @Override
    public void indexAttribute(MehtaGraphIndexMode indexMode, Object value, Object oldValue) {
        indexAttribute(indexMode, null, value, oldValue);
    }

    @Override
    public void indexAttribute(MehtaGraphIndexMode indexMode, String indexKey, Object value, Object oldValue) {
        if (indexMode == MehtaGraphIndexMode.OFF) {
            return;
        } else if (indexMode == MehtaGraphIndexMode.KEY) {
            if (oldValue != null) {
                exactNodeIndex.remove(node, indexKey, oldValue);            // remove old
            }
            exactNodeIndex.add(node, indexKey, value);                      // index new
        } else if (indexMode == MehtaGraphIndexMode.FULLTEXT) {
            // Note: all the topic's FULLTEXT properties are indexed under the same key ("_fulltext").
            // So, when removing from index we must explicitley give the old value.
            if (oldValue != null) {
                fulltextNodeIndex.remove(node, KEY_FULLTEXT, oldValue);     // remove old
            }
            fulltextNodeIndex.add(node, KEY_FULLTEXT, value);               // index new
        } else if (indexMode == MehtaGraphIndexMode.FULLTEXT_KEY) {
            if (oldValue != null) {
                fulltextNodeIndex.remove(node, indexKey, oldValue);         // remove old
            }
            fulltextNodeIndex.add(node, indexKey, value);                   // index new
        } else {
            throw new RuntimeException("Index mode \"" + indexMode + "\" not implemented");
        }
    }



    // === Java API ===

    @Override
    public String toString() {
        return "mehta node " + getId() + " " + getAttributesString(node);
    }
}
