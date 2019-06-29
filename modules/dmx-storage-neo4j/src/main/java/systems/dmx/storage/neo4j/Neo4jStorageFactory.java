package systems.dmx.storage.neo4j;

import systems.dmx.core.impl.ModelFactoryImpl;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXStorageFactory;



/**
 * A factory for obtaining a DMX storage based on Neo4j/Lucene.
 */
public class Neo4jStorageFactory implements DMXStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DMXStorage newDMXStorage(String databasePath, ModelFactoryImpl mf) {
        return new Neo4jStorage(databasePath, mf);
    }
}
