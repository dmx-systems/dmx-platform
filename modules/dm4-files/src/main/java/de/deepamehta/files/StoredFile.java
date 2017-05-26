package de.deepamehta.files;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class StoredFile implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final String fileName;
    private final String repoPath;
    private final long fileTopicId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    StoredFile(String fileName, String repoPath, long fileTopicId) {
        this.fileName = fileName;
        this.repoPath = repoPath;
        this.fileTopicId = fileTopicId;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getFileName() {
        return fileName;
    }

    public String getRepoPath() {
        return repoPath;
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
            storedFile.put("repo_path", repoPath);
            storedFile.put("topic_id", fileTopicId);
            return storedFile;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
