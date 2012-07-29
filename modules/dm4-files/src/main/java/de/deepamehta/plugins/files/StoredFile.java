package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class StoredFile implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject storedFile = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    StoredFile(String fileName, long fileTopicId) {
        try {
            storedFile.put("file_name", fileName);
            storedFile.put("topic_id", fileTopicId);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return storedFile;
    }
}
