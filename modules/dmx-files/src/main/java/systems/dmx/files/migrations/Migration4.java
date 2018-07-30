package systems.dmx.files.migrations;

import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Migration;

import java.util.List;
import java.util.logging.Logger;



/**
 * Renames root Folder topics, in case of dmx.filerepo.per_workspace=true.
 * Renames topic type "Disk Quota" -> "Disk Quota (MB)".
 * Installs the file size renderer.
 * <p>
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.8
 */
public class Migration4 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final boolean FILE_REPOSITORY_PER_WORKSPACE = Boolean.getBoolean("dmx.filerepo.per_workspace");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // 1) Rename root Folder topics
        if (FILE_REPOSITORY_PER_WORKSPACE) {
            List<Topic> workspaces = dmx.getTopicsByType("dmx.workspaces.workspace");
            logger.info("########## Renaming root Folder topics of " + workspaces.size() + " possible workspaces");
            int renamed = 0;
            for (Topic workspace : workspaces) {
                Topic folderTopic = fetchFolderTopic("/workspace-" + workspace.getId());
                if (folderTopic != null) {
                    folderTopic.getChildTopics().set("dmx.files.folder_name", workspace.getSimpleValue().toString());
                    renamed++;
                }
            }
            logger.info("########## Root Folder topics renamed: " + renamed);
        } else {
            logger.info("########## Renaming root Folder topics SKIPPED -- per-workspace file repositories are " +
                "switched off");
        }
        //
        // 2) Rename topic type "Disk Quota"
        dmx.getTopicType("dmx.files.disk_quota").setSimpleValue("Disk Quota (MB)");
        //
        // 3) Install file size renderer
        setTopicTypeViewConfigValue("dmx.files.size", "simple_renderer_uri", "dmx.files.file_size_renderer");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Fetches the Folder topic representing the directory at the given repository path.
     * If no such Folder topic exists <code>null</code> is returned.
     */
    private Topic fetchFolderTopic(String repoPath) {
        Topic topic = dmx.getTopicByValue("dmx.files.path", new SimpleValue(repoPath));
        return topic != null ? topic.getRelatedTopic("dmx.core.composition", "dmx.core.child", "dmx.core.parent",
            "dmx.files.folder") : null;
    }
}
