package de.deepamehta.core.storage.spi;

import de.deepamehta.core.service.ModelFactory;



/**
 * Factory for obtaining a DeepaMehta storage.
 * <p>
 * Note: the factory is only needed by the test environment.
 * The DeepaMehta Core obtains the storage as an OSGi service.
 */
public interface DeepaMehtaStorageFactory {

    DeepaMehtaStorage newDeepaMehtaStorage(String databaseUri, ModelFactory mf);
}
