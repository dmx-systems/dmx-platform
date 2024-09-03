package systems.dmx.accountmanagement.configuration;

/**
 * Static configuration values from DMX' config.properties.
 */
public final class Configuration {

    private final boolean newAccountsAreEnabled;

    private final String accountManager;

    private final ExpectedPasswordComplexity expectedPasswordComplexity;

    private final int expectedMinPasswordLength;

    private final int expectedMaxPasswordLength;

    public Configuration(
            boolean newAccountsAreEnabled,
            String accountManager,
            ExpectedPasswordComplexity expectedPasswordComplexity,
            int expectedMinPasswordLength,
            int expectedMaxPasswordLength) {
        this.newAccountsAreEnabled = newAccountsAreEnabled;
        this.accountManager = accountManager;
        this.expectedPasswordComplexity = expectedPasswordComplexity;
        this.expectedMinPasswordLength = expectedMinPasswordLength;
        this.expectedMaxPasswordLength = expectedMaxPasswordLength;
    }

    public boolean isNewAccountsAreEnabled() {
        return newAccountsAreEnabled;
    }

    public String getAccountManager() {
        return accountManager;
    }

    /**
     * {@link ExpectedPasswordComplexity}
     */
    public ExpectedPasswordComplexity getExpectedPasswordComplexity() {
        return expectedPasswordComplexity;
    }

    /**
     * Minimum number of characters for a password to be accepted.
     */
    public int getExpectedMinPasswordLength() {
        return expectedMinPasswordLength;
    }

    /**
     * Maximum number of characters for a password to be accepted.
     */
    public int getExpectedMaxPasswordLength() {
        return expectedMaxPasswordLength;
    }

}
