package systems.dmx.files.migrations;

import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;



/**
 * Deletes topic types "File Content" and "Foler Content".
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration3 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        for (Topic topic : dmx.getTopicsByType("dmx.files.file_content")) {
            topic.delete();
        }
        for (Topic topic : dmx.getTopicsByType("dmx.files.folder_content")) {
            topic.delete();
        }
        dmx.deleteTopicType("dmx.files.file_content");
        dmx.deleteTopicType("dmx.files.folder_content");
    }
}
