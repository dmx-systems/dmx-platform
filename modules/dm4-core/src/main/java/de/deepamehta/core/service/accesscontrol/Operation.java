package de.deepamehta.core.service.accesscontrol;



public enum Operation {

    READ("dm4.accesscontrol.operation.read"),
    WRITE("dm4.accesscontrol.operation.write");

    public final String uri;

    private Operation(String uri) {
        this.uri = uri;
    }
}
