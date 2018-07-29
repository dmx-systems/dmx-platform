package systems.dmx.storage.neo4j;

import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXStorageFactory;



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
