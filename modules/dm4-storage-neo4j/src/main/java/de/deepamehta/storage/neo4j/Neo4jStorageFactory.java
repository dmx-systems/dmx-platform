package de.deepamehta.storage.neo4j;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;



// ### TODO: drop this. Register as OSGi service instead.
public class Neo4jStorageFactory implements DeepaMehtaStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DeepaMehtaStorage createInstance(String databasePath) {
        return new Neo4jStorage(databasePath);
    }
}
