package de.deepamehta.core.impl;

import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.JavaUtils;

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

    void createTx(ContainerRequest request) {
        logger.fine("### Creating transaction for request " + request);
        threadLocalTx.set(pl.beginTx());
    }

    void commitTx(ContainerRequest request, ContainerResponse response) {
        try (DeepaMehtaTransaction tx = threadLocalTx.get()) {
            boolean success = response.getMappedThrowable() == null;
            if (success) {
                logger.fine("### Comitting transaction of request \"" + JavaUtils.requestInfo(request) + "\"");
                tx.success();
            }
            // Note: with Neo4j 3.0 we're creating transactions also for read requests. A read request might be
            // responded with 304 Not Modified. In this case success is false (the CachingPlugin throwed a
            // WebApplicationException). If success is false we don't log any warning here. Errors are logged
            // already by the UniversalExceptionMapper. ### TODO: rethink about rollback on 304
        } catch (Exception e) {
            throw new RuntimeException("Comitting transaction of request \"" + JavaUtils.requestInfo(request) +
                "\" failed", e);
        } finally {
            threadLocalTx.remove();
        }
    }
}
