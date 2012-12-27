package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.spi.MehtaGraphTransaction;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;



/**
 * Adapts a Neo4j transaction to a MehtaGraph transaction.
 */
class Neo4jTransactionAdapter implements MehtaGraphTransaction {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Transaction tx;

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jTransactionAdapter(GraphDatabaseService neo4j) {
        tx = neo4j.beginTx();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void success() {
        tx.success();
    }

    @Override
    public void failure() {
        tx.failure();
    }

    @Override
    public void finish() {
        tx.finish();
    }
}
