package systems.dmx.base.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds assoc type "User Mailbox".
 *
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration2 extends Migration {

    @Override
    public void run() {
        dmx.createAssocType(mf.newAssocTypeModel("dmx.base.user_mailbox", "User Mailbox", "dmx.core.text"));
    }
}
