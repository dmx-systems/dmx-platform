package de.deepamehta.files;

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
            storedFile.put("fileName", fileName);
            storedFile.put("topicId", fileTopicId);
            return storedFile;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
