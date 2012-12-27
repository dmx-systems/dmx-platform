package de.deepamehta.mehtagraph.spi;



public interface MehtaGraphTransaction {

    void success();

    void failure();

    void finish();
}
