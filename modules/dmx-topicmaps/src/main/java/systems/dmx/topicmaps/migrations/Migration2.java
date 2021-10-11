package systems.dmx.topicmaps.migrations;

import static systems.dmx.base.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.topicmaps.Constants.*;
import static systems.dmx.webclient.Constants.*;
import systems.dmx.core.service.Migration;



/**
 * Adds "Background Image" to "Topicmap".
 *
 * Part of DMX 5.3
 * Runs only in UPDATE mode.
 */
public class Migration2 extends Migration {

    @Override
    public void run() {
        dmx.createAssocType(mf.newAssocTypeModel(BACKGROUND_IMAGE, "Background Image", TEXT));
        dmx.getTopicType(TOPICMAP).addCompDef(mf.newCompDefModel(
            BACKGROUND_IMAGE, false, false,
            TOPICMAP, URL, ONE
        )).getViewConfig()
            .setConfigValue(VIEW_CONFIG, COLOR, "hsl(5, 50%, 53%)")
            .setConfigValue(VIEW_CONFIG, COLOR + "#" + BACKGROUND_COLOR, "hsl(5, 80%, 96%)");
    }
}
