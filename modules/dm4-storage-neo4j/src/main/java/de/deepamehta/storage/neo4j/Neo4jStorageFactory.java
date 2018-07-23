package de.deepamehta.storage.neo4j;

import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DMXStorage;
import de.deepamehta.core.storage.spi.DMXStorageFactory;



/**
 * Factory for obtaining a DMX storage based on Neo4j/Lucene.
 * <p>
 * Note: the factory is only needed by the test environment.
 * The DMX Core obtains the storage as an OSGi service.
 */
public class Neo4jStorageFactory implements DMXStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DMXStorage newDMXStorage(String databasePath, ModelFactory mf) {
        return new Neo4jStorage(databasePath, mf);
    }
}
