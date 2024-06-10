package systems.dmx.accountmanagement;

import systems.dmx.core.Topic;

public class CheckCredentialsResult {
    private CheckCredentialsResult(boolean success, Topic usernameTopic) {
        this.success = success;
        this.usernameTopic = usernameTopic;
    }

    public final boolean success;

    public final Topic usernameTopic;

    public static CheckCredentialsResult success(Topic usernameTopic) {
        if (usernameTopic == null) {
            throw new IllegalArgumentException("usernameTopic must not be null");
        }
        return new CheckCredentialsResult(true, usernameTopic);
    }

    public static CheckCredentialsResult lookupOrCreationRequired() {
        return new CheckCredentialsResult(true, null);
    }

    public static CheckCredentialsResult failed() {
        return new CheckCredentialsResult(false, null);
    }
}