package de.deepamehta.plugins.proxy.model;

import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.net.URL;



public class ResourceInfo {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String kind;    // "file", "directory", "remote"

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Precondition: the URL is a remote URL.
     */
    public ResourceInfo(URL uri) {
        kind = "remote";
    }

    /**
     * Precondition: the file exists.
     */
    public ResourceInfo(File file) {
        kind = file.isDirectory() ? "directory" : "file";
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

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
