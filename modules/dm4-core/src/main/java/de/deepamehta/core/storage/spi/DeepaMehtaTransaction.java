package de.deepamehta.core.storage.spi;



public interface DeepaMehtaTransaction extends AutoCloseable {

    void success();

    void failure();
}
