package de.deepamehta.files;



public enum ItemKind {

    FILE, DIRECTORY;

    public String stringify() {
        return name().toLowerCase();
    }
}
