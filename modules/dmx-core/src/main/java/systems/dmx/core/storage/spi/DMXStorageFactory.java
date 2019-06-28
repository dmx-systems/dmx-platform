package systems.dmx.core.storage.spi;

import systems.dmx.core.service.ModelFactory;



/**
 * A factory for obtaining a DMX storage.
 */
public interface DMXStorageFactory {

    DMXStorage newDMXStorage(String databasePath, ModelFactory mf);
}
