package systems.dmx.core.storage.spi;

import systems.dmx.core.service.ModelFactory;



/**
 * Factory for obtaining a DMX storage.
 * <p>
 * Note: the factory is only needed by the test environment.
 * The DMX Core obtains the storage as an OSGi service.
 */
public interface DMXStorageFactory {

    DMXStorage newDMXStorage(String databasePath, ModelFactory mf);
}
