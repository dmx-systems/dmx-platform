package systems.dmx.core.storage.spi;

import systems.dmx.core.impl.ModelFactoryImpl;



/**
 * A factory for obtaining a DMX storage.
 */
public interface DMXStorageFactory {

    DMXStorage newDMXStorage(String databasePath, ModelFactoryImpl mf);
}
