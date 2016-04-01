package de.deepamehta.plugins.files.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Renames root Folder topics, in case of dm4.filerepo.per_workspace=true.
 * Renames topic type "Disk Quota" -> "Disk Quota (MB)".
 * Installs the file size renderer.
 * <p>
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.8-SNAPSHOT
 */
public class Migration4 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final boolean FILE_REPOSITORY_PER_WORKSPACE = Boolean.getBoolean("dm4.filerepo.per_workspace");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // 1) Rename root Folder topics
        if (FILE_REPOSITORY_PER_WORKSPACE) {
            List<Topic> workspaces = dm4.getTopicsByType("dm4.workspaces.workspace");
            logger.info("########## Renaming root Folder topics of " + workspaces.size() + " possible workspaces");
            int renamed = 0;
            for (Topic workspace : workspaces) {
                Topic folderTopic = fetchFolderTopic("/workspace-" + workspace.getId());
                if (folderTopic != null) {
                    folderTopic.getChildTopics().set("dm4.files.folder_name", workspace.getSimpleValue().toString());
                    renamed++;
                }
            }
            logger.info("########## Root Folder topics renamed: " + renamed);
        } else {
            logger.info("########## Renaming root Folder topics ABORTED -- per-workspace file repositories are " +
                "switched off");
        }
        //
        // 2) Rename topic type "Disk Quota"
        dm4.getTopicType("dm4.files.disk_quota").setSimpleValue("Disk Quota (MB)");
        //
        // 3) Install file size renderer
        addTopicTypeSetting("dm4.files.size", "simple_renderer_uri", "dm4.files.file_size_renderer");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Fetches the Folder topic representing the directory at the given repository path.
     * If no such Folder topic exists <code>null</code> is returned.
     */
    private Topic fetchFolderTopic(String repoPath) {
        Topic topic = dm4.getTopicByValue("dm4.files.path", new SimpleValue(repoPath));
        return topic != null ? topic.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
            "dm4.files.folder") : null;
    }
}
