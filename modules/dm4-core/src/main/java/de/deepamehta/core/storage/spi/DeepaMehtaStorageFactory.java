package de.deepamehta.core.storage.spi;



/**
 * Factory for obtaining a DeepaMehta storage.
 * <p>
 * Note: the factory in only needed by the test suites.
 * The DeepaMehta Core obtains the storage as an OSGi service.
 */
public interface DeepaMehtaStorageFactory {

    DeepaMehtaStorage createInstance(String databasePath);
}
