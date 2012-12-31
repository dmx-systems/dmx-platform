package de.deepamehta.core.storage.spi;



public interface MehtaGraphTransaction {

    void success();

    void failure();

    void finish();
}
