package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class UploadResult implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject uploadResult = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    UploadResult(String fileName) {
        try {
            uploadResult.put("file_name", fileName);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return uploadResult;
    }
}
