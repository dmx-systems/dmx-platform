package de.deepamehta.mehtagraph.spi;



public interface MehtaGraphFactory {

    MehtaGraph createInstance(String databasePath);
}
