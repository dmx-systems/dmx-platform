package de.deepamehta.plugins.files;



public enum ItemKind {

    FILE, DIRECTORY;

    public String stringify() {
        return name().toLowerCase();
    }
}
