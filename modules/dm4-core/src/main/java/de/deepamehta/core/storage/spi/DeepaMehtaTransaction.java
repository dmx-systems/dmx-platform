package de.deepamehta.core.storage.spi;



public interface DeepaMehtaTransaction {

    void success();

    void failure();

    void finish();
}
