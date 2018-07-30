package systems.dmx.core.service.accesscontrol;



public enum Operation {

    READ("dmx.accesscontrol.operation.read"),
    WRITE("dmx.accesscontrol.operation.write");

    public final String uri;

    private Operation(String uri) {
        this.uri = uri;
    }
}
