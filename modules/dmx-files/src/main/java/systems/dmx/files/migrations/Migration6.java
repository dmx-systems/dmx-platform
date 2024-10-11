package systems.dmx.files.migrations;

import static systems.dmx.files.Constants.*;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;
import systems.dmx.core.util.JavaUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;



/**
 * Sets missing Media Types for existing office File topics.
 * <p>
 * Part of DMX 5.3.5
 * Runs only in UPDATE mode.
 */
public class Migration6 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        List office = Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp");
        int f = 0;  // files
        int o = 0;  // office
        int r = 0;  // repaired
        int c = 0;  // corrupt
        for (Topic file : dmx.getTopicsByType(FILE)) {
            f++;
            String fileName = file.getChildTopics().getString(FILE_NAME, null);
            if (fileName == null) {
                c++;
                logger.warning(String.format("File %d has no name, path=%s", file.getId(),
                    file.getChildTopics().getString(FILE_NAME, null)));
                continue;
            }
            String ext = JavaUtils.getExtension(fileName);
            if (office.contains(ext)) {
                o++;
                String mediaType = file.getChildTopics().getString(MEDIA_TYPE, null);
                if (mediaType == null) {
                    r++;
                    mediaType = JavaUtils.getFileType(fileName);
                    file.update(mf.newChildTopicsModel().set(MEDIA_TYPE, mediaType));
                }
            }
        }
        logger.info(String.format("### Office media type migration complete\n  File topics: %d\n  Office files: %d\n" +
          "  Office files repaired: %d\n  Corrupt files: %d", f, o, r, c));
    }
}
