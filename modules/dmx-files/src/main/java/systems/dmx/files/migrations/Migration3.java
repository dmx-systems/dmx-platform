package systems.dmx.files.migrations;

import systems.dmx.core.service.Migration;



/**
 * Deletes topic types "File Content" and "Foler Content".
 * <p>
 * Part of DMX 5.2
q * Runs only in UPDATE mode.
 */
public class Migration3 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        dmx.deleteTopicType("dmx.files.file_content");
        dmx.deleteTopicType("dmx.files.folder_content");
    }
}
