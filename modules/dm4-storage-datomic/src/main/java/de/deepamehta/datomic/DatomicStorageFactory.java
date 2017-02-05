package de.deepamehta.datomic;

import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;



/**
 * Factory for obtaining a DeepaMehta storage based on Datomic.
 * <p>
 * Note: the factory is only needed by the test environment.
 * The DeepaMehta Core obtains the storage as an OSGi service.
 */
public class DatomicStorageFactory implements DeepaMehtaStorageFactory {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public DeepaMehtaStorage newDeepaMehtaStorage(String databasePath, ModelFactory mf) {
        return new DatomicStorage(databasePath, mf);
    }
}
