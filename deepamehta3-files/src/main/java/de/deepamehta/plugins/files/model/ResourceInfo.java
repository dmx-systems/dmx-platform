package de.deepamehta.plugins.files.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.io.File;
import java.net.URL;



public class ResourceInfo {
    
    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String kind;    // "file", "directory", "remote"
    private boolean error;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResourceInfo(URL url) {
        if (url.getProtocol().equals("file")) {
            File file = new File(url.getPath());
            if (!file.exists()) {
                error = true;
            } else if (file.isDirectory()) {
                kind = "directory";
            } else {
                kind = "file";
            }
        } else {
            kind = "remote";
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public JSONObject toJSON() {
        try {
            JSONObject info = new JSONObject();
            if (error) {
                info.put("error", "not found");
            } else {
                info.put("kind", kind);
            }
            return info;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "resource info (kind=\"" + kind + "\" error=" + error + ")";
    }
}
