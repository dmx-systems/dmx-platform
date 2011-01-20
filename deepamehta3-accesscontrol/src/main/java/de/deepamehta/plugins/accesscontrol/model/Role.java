package de.deepamehta.plugins.accesscontrol.model;



public enum Role {

    CREATOR, OWNER, MEMBER, EVERYONE;
    
    public String s() {
        return name().toLowerCase();
    }
}
