package systems.dmx.core.migrations;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.service.Migration;



/**
 * Adds "Ordered" to "Composition Definition".
 * <p>
 * Part of DMX 5.3
 * Runs only in UPDATE mode.
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        dmx.createTopicType(mf.newTopicTypeModel(ORDERED, "Ordered", BOOLEAN));
        dmx.getAssocType(COMPOSITION_DEF).addCompDefBefore(
            mf.newCompDefModel(COMPOSITION_DEF, ORDERED, ONE),
            ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE
        );
    }
}
