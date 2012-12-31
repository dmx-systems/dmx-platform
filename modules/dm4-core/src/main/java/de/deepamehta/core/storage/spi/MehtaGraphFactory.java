package de.deepamehta.core.storage.spi;



public interface MehtaGraphFactory {

    MehtaGraph createInstance(String databasePath);
}
