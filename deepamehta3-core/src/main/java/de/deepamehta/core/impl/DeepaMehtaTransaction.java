package de.deepamehta.core.impl;

import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.Transaction;



/**
 * Wraps a HyperGraph transaction.
 */
class DeepaMehtaTransaction implements de.deepamehta.core.storage.Transaction {

    private Transaction tx;

    DeepaMehtaTransaction(HyperGraph hg) {
        tx = hg.beginTx();
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
