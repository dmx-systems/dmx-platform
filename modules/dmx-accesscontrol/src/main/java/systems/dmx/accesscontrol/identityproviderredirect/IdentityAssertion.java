package systems.dmx.accesscontrol.identityproviderredirect;

/**
 * Collection of user attributes that an identity provider delivers to DMX
 * during a login.
 */
public final class IdentityAssertion {

    private final String username;
    private final String emailAddress;
    private final String displayName;
    private final boolean enabled;

    IdentityAssertion(
            String username,
            String emailAddress,
            String displayName,
            boolean enabled) {
        this.username = username;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Builder for {@link IdentityAssertion} to provide a future-proof API
     * to create such instances.
     */
    public static final class Builder {
        private String username;
        private String emailAddress;
        private String displayName;
        private boolean enabled;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder emailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public IdentityAssertion build() {
            return new IdentityAssertion(
                    username,
                    emailAddress,
                    displayName,
                    enabled
            );
        }
    }
}
