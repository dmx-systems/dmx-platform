package de.deepamehta.core.impl.storage;

import de.deepamehta.core.DeepaMehtaTransaction;

import de.deepamehta.mehtagraph.spi.MehtaGraph;
import de.deepamehta.mehtagraph.spi.MehtaGraphTransaction;



/**
 * Adapts a MehtaGraph transaction to a DeepaMehta transaction.
 */
class MGTransactionAdapter implements DeepaMehtaTransaction {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private MehtaGraphTransaction tx;

    // ---------------------------------------------------------------------------------------------------- Constructors

    MGTransactionAdapter(MehtaGraph hg) {
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
