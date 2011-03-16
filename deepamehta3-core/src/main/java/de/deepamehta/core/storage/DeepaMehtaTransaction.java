package de.deepamehta.core.storage;



public interface DeepaMehtaTransaction {

    void success();

    void failure();

    void finish();
}
