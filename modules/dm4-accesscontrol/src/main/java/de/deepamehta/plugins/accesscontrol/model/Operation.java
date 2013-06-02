package de.deepamehta.plugins.accesscontrol.model;



public enum Operation {

    WRITE("dm4.accesscontrol.operation.write"),
    CREATE("dm4.accesscontrol.operation.create");

    public final String uri;

    private Operation(String uri) {
        this.uri = uri;
    }
}
