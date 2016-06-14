package de.deepamehta.core.impl;

import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import java.util.logging.Logger;



class TransactionFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final ThreadLocal<DeepaMehtaTransaction> threadLocalTx = new ThreadLocal();

    private final PersistenceLayer pl;

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static final Logger logger = Logger.getLogger(TransactionFactory.class.getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TransactionFactory(PersistenceLayer pl) {
        this.pl = pl;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void create(ContainerRequest request) {
        logger.fine("### Creating transaction for request " + request);
        threadLocalTx.set(pl.beginTx());
    }

    void close(ContainerRequest request, ContainerResponse response) {
        try (DeepaMehtaTransaction tx = threadLocalTx.get()) {
            boolean success = response.getMappedThrowable() == null;
            if (success) {
                logger.fine("### Comitting transaction of request " + request);
                tx.success();
            } else {
                logger.warning("### Rollback transaction of request " + request);
            }
        } catch (Exception e) {
            throw new RuntimeException("Closing transaction of request " + request + " failed", e);
        } finally {
            threadLocalTx.remove();
        }
    }
}
