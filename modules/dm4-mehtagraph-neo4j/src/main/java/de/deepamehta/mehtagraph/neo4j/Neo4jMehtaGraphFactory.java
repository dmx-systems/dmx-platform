package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.spi.MehtaGraph;
import de.deepamehta.mehtagraph.spi.MehtaGraphFactory;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.util.logging.Logger;



public class Neo4jMehtaGraphFactory implements MehtaGraphFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public MehtaGraph createInstance(String databasePath) {
        GraphDatabaseService neo4j = null;
        try {
            neo4j = new GraphDatabaseFactory().newEmbeddedDatabase(databasePath);
            //
            return new Neo4jMehtaGraph(neo4j);
            //
        } catch (Exception e) {
            if (neo4j != null) {
                logger.info("Shutdown Neo4j");
                neo4j.shutdown();
            }
            throw new RuntimeException("Creating a Neo4jMehtaGraph instance failed", e);
        }
    }
}
