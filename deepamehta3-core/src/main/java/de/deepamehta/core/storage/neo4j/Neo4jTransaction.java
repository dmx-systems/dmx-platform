package de.deepamehta.core.storage.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;



class Neo4jTransaction implements de.deepamehta.core.storage.Transaction {

    private Transaction tx;

    Neo4jTransaction(GraphDatabaseService graphDb) {
        tx = graphDb.beginTx();
    }

    public void success() {
        tx.success();
    }

    public void failure() {
        tx.failure();
    }

    public void finish() {
        tx.finish();
    }
}
