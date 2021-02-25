package systems.dmx.webclient.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds "Noneditable" to "View Configuration".
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.webclient.noneditable", "Noneditable", "dmx.core.boolean"));
        dmx.getTopicType("dmx.webclient.view_config").addCompDefBefore(mf.newCompDefModel(
            "dmx.webclient.view_config", "dmx.webclient.noneditable", "dmx.core.one"
        ), "dmx.webclient.widget");
    }
}
