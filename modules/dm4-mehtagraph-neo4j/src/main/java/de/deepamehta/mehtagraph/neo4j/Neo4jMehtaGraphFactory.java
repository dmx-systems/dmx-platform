package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.spi.MehtaGraph;
import de.deepamehta.mehtagraph.spi.MehtaGraphFactory;



public class Neo4jMehtaGraphFactory implements MehtaGraphFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public MehtaGraph createInstance(String databasePath) {
        return new Neo4jMehtaGraph(databasePath);
    }
}
