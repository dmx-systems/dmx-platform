package de.deepamehta.storage.neo4j;

import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;



/**
 * Factory for obtaining a DeepaMehta storage based on Neo4j/Lucene.
 * <p>
 * Note: the factory is only needed by the test environment.
 * The DeepaMehta Core obtains the storage as an OSGi service.
 */
public class Neo4jStorageFactory implements DeepaMehtaStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DeepaMehtaStorage newDeepaMehtaStorage(String databasePath, ModelFactory mf) {
        return new Neo4jStorage(databasePath, mf);
    }
}
