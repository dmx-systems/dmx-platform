package de.deepamehta.storage.neo4j;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;



/**
 * Factory for obtaining a Neo4j/Lucene based DeepaMehta storage.
 * <p>
 * Note: the factory in only needed by the test suites.
 * The DeepaMehta Core obtains the storage as an OSGi service.
 */
public class Neo4jStorageFactory implements DeepaMehtaStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DeepaMehtaStorage createInstance(String databasePath) {
        return new Neo4jStorage(databasePath);
    }
}
