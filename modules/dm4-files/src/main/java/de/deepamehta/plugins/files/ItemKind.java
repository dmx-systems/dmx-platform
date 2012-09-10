package de.deepamehta.plugins.files;



public enum ItemKind {

    FILE("file"), DIRECTORY("directory");

    private final String kind;

    private ItemKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return kind;
    }

}
