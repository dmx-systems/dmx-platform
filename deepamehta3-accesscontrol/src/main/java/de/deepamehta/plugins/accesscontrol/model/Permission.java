package de.deepamehta.plugins.accesscontrol.model;



public enum Permission {

    WRITE, CREATE;

    public String s() {
        return name().toLowerCase();
    }
}
