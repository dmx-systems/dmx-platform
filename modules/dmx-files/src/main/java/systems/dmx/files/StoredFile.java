package systems.dmx.files;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.RelatedTopic;

import org.codehaus.jettison.json.JSONObject;



// TODO: drop this class. Use File topic instead?
public class StoredFile implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final String fileName;
    private final String repoPath;
    private final long fileTopicId;
    private final RelatedTopic fileTopic;

    // ---------------------------------------------------------------------------------------------------- Constructors

    StoredFile(String fileName, String repoPath, long fileTopicId, RelatedTopic fileTopic) {
        this.fileName = fileName;
        this.repoPath = repoPath;
        this.fileTopicId = fileTopicId;
        this.fileTopic = fileTopic;
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

    public RelatedTopic getFileTopic() {
        return fileTopic;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("fileName", fileName)
                .put("repoPath", repoPath)
                .put("topicId", fileTopicId)
                .put("topic", fileTopic.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
