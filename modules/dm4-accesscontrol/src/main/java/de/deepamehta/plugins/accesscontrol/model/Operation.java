package de.deepamehta.plugins.accesscontrol.model;



public enum Operation {

    WRITE("dm4.accesscontrol.operation_write"),
    CREATE("dm4.accesscontrol.operation_create");

    public final String uri;

    private Operation(String uri) {
        this.uri = uri;
    }
}
