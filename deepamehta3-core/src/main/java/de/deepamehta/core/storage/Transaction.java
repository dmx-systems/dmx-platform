package de.deepamehta.core.storage;



public interface Transaction {

    void success();

    void failure();

    void finish();
}
