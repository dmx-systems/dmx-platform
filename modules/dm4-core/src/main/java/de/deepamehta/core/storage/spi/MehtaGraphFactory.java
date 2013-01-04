package de.deepamehta.core.storage.spi;



// ### TODO: drop this. Register as OSGi service instead.
public interface MehtaGraphFactory {

    DeepaMehtaStorage createInstance(String databasePath);
}
