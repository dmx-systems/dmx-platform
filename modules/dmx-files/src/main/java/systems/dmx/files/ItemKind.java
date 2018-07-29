package systems.dmx.files;



public enum ItemKind {

    FILE, DIRECTORY;

    public String stringify() {
        return name().toLowerCase();
    }
}
