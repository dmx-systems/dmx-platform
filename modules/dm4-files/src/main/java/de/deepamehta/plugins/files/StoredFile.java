package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class StoredFile implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String fileName;
    private long fileTopicId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    StoredFile(String fileName, long fileTopicId) {
        this.fileName = fileName;
        this.fileTopicId = fileTopicId;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getFileName() {
        return fileName;
    }

    public long getFileTopicId() {
        return fileTopicId;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject storedFile = new JSONObject();
            storedFile.put("file_name", fileName);
            storedFile.put("topic_id", fileTopicId);
            return storedFile;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
