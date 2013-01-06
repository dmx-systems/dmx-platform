package de.deepamehta.core.storage.spi;



// ### TODO: drop this. Register as OSGi service instead.
public interface DeepaMehtaStorageFactory {

    DeepaMehtaStorage createInstance(String databasePath);
}
