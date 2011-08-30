package de.deepamehta.core.impl.service;



class ChangeReport {

    boolean typeUriChanged = false;
    String oldTypeUri;
    String newTypeUri;

    void typeUriChanged(String oldTypeUri, String newTypeUri) {
        this.typeUriChanged = true;
        this.oldTypeUri = oldTypeUri;
        this.newTypeUri = newTypeUri;
    }
}
