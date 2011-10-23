package de.deepamehta.core.service;



public class ChangeReport {

    public boolean typeUriChanged = false;
    public String oldTypeUri;
    public String newTypeUri;

    public void typeUriChanged(String oldTypeUri, String newTypeUri) {
        this.typeUriChanged = true;
        this.oldTypeUri = oldTypeUri;
        this.newTypeUri = newTypeUri;
    }
}
