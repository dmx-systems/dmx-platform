package systems.dmx.accountmanagement;

import systems.dmx.core.Topic;

public final class CheckCredentialsResult {
    private CheckCredentialsResult(boolean success, Topic usernameTopic) {
        this.success = success;
        this.usernameTopic = usernameTopic;
    }

    public final boolean success;

    public final Topic usernameTopic;

    /**
     * Creates an instance which tells the platform that the credentials are correct and which username topic
     * to user in order to proceed.
     *
     * @param usernameTopic
     * @return
     */
    static CheckCredentialsResult success(Topic usernameTopic) {
        if (usernameTopic == null) {
            throw new IllegalArgumentException("usernameTopic must not be null");
        }
        return new CheckCredentialsResult(true, usernameTopic);
    }

    /**
     * Creates an instance which tells the platform that the credentials are correct and that a username topic
     * has to be done which could also lead to the creation of such. All of which the platform takes care of.
     *
     * @return
     */
    public static CheckCredentialsResult lookupOrCreationRequired() {
        return new CheckCredentialsResult(true, null);
    }

    /**
     * Creates an instance which tells the platform that the credentials are incorrect and login shall not proceed.
     *
     * @return
     */
    public static CheckCredentialsResult failed() {
        return new CheckCredentialsResult(false, null);
    }
}