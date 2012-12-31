package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.core.storage.spi.MehtaGraph;
import de.deepamehta.core.storage.spi.MehtaGraphFactory;



public class Neo4jMehtaGraphFactory implements MehtaGraphFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public MehtaGraph createInstance(String databasePath) {
        return new Neo4jMehtaGraph(databasePath);
    }
}
