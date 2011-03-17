package de.deepamehta.core.storage.impl;

import de.deepamehta.core.storage.DeepaMehtaTransaction;

import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperGraphTransaction;



/**
 * Adapts a HyperGraph transaction to a DeepaMehta transaction.
 */
class HGTransactionAdapter implements DeepaMehtaTransaction {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private HyperGraphTransaction tx;

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGTransactionAdapter(HyperGraph hg) {
        tx = hg.beginTx();
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
