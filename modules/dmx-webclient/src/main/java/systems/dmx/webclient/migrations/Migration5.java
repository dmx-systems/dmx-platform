package systems.dmx.webclient.migrations;

import static systems.dmx.webclient.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Migration;



/**
 * Adds "Arrow Shape" and "Hollow" props to "View Configuration".
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.webclient.arrow_shape", "Arrow Shape", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.webclient.hollow", "Hollow", BOOLEAN));
        TopicType viewConfig = dmx.getTopicType(VIEW_CONFIG);
        viewConfig.addCompDef(mf.newCompDefModel(VIEW_CONFIG, "dmx.webclient.arrow_shape", ONE));
        viewConfig.addCompDef(mf.newCompDefModel(VIEW_CONFIG, "dmx.webclient.hollow", ONE));
    }
}
