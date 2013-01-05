package de.deepamehta.storage.neo4j;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.MehtaGraphFactory;



// ### TODO: drop this. Register as OSGi service instead.
public class Neo4jMehtaGraphFactory implements MehtaGraphFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DeepaMehtaStorage createInstance(String databasePath) {
        return new Neo4jStorage(databasePath);
    }
}
