package systems.dmx.files.migrations;

import static systems.dmx.core.Constants.*;
import static systems.dmx.files.Constants.*;
import systems.dmx.core.CompDef;
import systems.dmx.core.service.Migration;



/**
 * Set "Path" as the identity attribute for both, "File" and "Folder".
 * <p>
 * Part of DMX 5.3
 * Runs only in UPDATE mode.
 */
public class Migration5 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setIdentityAttr(dmx.getTopicType(FILE).getCompDef(PATH));
        setIdentityAttr(dmx.getTopicType(FOLDER).getCompDef(PATH));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setIdentityAttr(CompDef compDef) {
        compDef.update(mf.newCompDefModel(mf.newAssocModel(mf.newChildTopicsModel().set(IDENTITY_ATTR, true)), null));
    }
}
