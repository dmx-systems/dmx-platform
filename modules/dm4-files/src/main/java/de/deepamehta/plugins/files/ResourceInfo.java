package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;

import java.io.File;



public class ResourceInfo implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String kind;    // "file", "directory"

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Precondition: the file exists.
     */
    public ResourceInfo(File file) {
        kind = file.isDirectory() ? "directory" : "file";
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject info = new JSONObject();
            info.put("kind", kind);
            return info;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "resource info (kind=\"" + kind + "\")";
    }
}
