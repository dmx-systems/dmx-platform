package de.deepamehta.core.storage.impl;

import de.deepamehta.core.storage.DeepaMehtaTransaction;

import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperGraphTransaction;



/**
 * Adapts a HyperGraph transaction to a DeepaMehta transaction.
 */
class HGTransactionAdapter implements DeepaMehtaTransaction {

    private HyperGraphTransaction tx;

    HGTransactionAdapter(HyperGraph hg) {
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
